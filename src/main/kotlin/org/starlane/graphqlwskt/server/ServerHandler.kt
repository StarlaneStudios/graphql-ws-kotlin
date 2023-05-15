package org.starlane.graphqlwskt.server

import com.google.gson.JsonObject
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.GraphQLContext
import graphql.execution.SubscriptionExecutionStrategy
import kotlinx.coroutines.*
import org.java_websocket.WebSocket
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.protocols.Protocol
import org.java_websocket.server.WebSocketServer
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.starlane.graphqlwskt.*
import org.starlane.graphqlwskt.Message.*
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext

/**
 * The main server-side WebSocket implementation
 */
internal class ServerHandler(
	address: InetSocketAddress,
	val adapter: GraphQLServerAdapter,
	val path: String,
	val initTimeout: Long,
	val context: CoroutineContext
) : WebSocketServer(address, buildDraft()) {

	internal val startupTask = CompletableDeferred<Unit>()
	internal val sockets = HashMap<WebSocket, SocketState>()
	internal val scope = CoroutineScope(context)

	override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
		if (handshake.resourceDescriptor != path) {
			conn.close(CloseCode.BadRequest)
			return
		}

		val ctx = GraphQLContext.newContext()
			.put("__socket", conn)
			.build()

		val state = SocketState(
			socket = conn,
			initialized = false,
			acknowledged = false,
			context = ctx,
			subscriptions = HashMap(),
			graphql = null,
		)

		scope.launch {
			delay(initTimeout)

			if (!state.initialized && conn.isOpen) {
				conn.close(CloseCode.ConnectionInitialisationTimeout)
			}
		}

		sockets[conn] = state
	}

	override fun onMessage(conn: WebSocket, message: String) {
		val state = sockets[conn] ?: run {
			conn.close(CloseCode.InternalServerError)
			return
		}

		val msg = try {
			MessageParser.fromJson<Message>(message)
		} catch(e: Exception) {
			conn.close(CloseCode.BadRequest)
			return
		}

		when(msg) {
			is ConnectionInit -> {
				if (state.initialized) {
					conn.close(CloseCode.TooManyInitialisationRequests)
					return
				}

				state.initialized = true
				state.context.putAll(msg.payload)

				val result = runBlocking {
					adapter.onConnect(state.context)
				}

				when(result) {
					is ConnectResult.Failure -> {
						conn.close(CloseCode.Forbidden)
					}
					is ConnectResult.Success -> {
						val response = result.payload ?: emptyMap()

						conn.send(ConnectionAck(response))

						val graphql = GraphQL.newGraphQL(result.schema)
							.subscriptionExecutionStrategy(SubscriptionExecutionStrategy())
							.build()

						state.acknowledged = true
						state.graphql = graphql
					}
				}
			}
			is Subscribe -> {
				if (!state.acknowledged) {
					conn.close(CloseCode.Unauthorized)
					return
				}

				if (msg.id in state.subscriptions) {
					conn.close(CloseCode.SubscriberAlreadyExists)
					return
				}

				val mid = msg.id
				val (operationName, query, variables, extensions) = msg.payload

				val input = ExecutionInput.newExecutionInput()
					.query(query)
					.operationName(operationName)

				if (variables != null) {
					input.variables(variables)
				}

				if (extensions != null) {
					input.extensions(extensions)
				}

				val result = state.graphql!!.execute(input)
				val data = result.getData<Any?>()

				if (!result.isDataPresent) {
					val errors = result.errors.map {
						it.toSpecification()
					}

					conn.send(Error(mid, errors.toTypedArray()))
					conn.send(Complete(mid))
					return
				}

				if (data !is Publisher<*>) {
					val payload = MessageParser.toJsonTree(result.toSpecification()) as JsonObject

					conn.send(Next(mid, payload))
					conn.send(Complete(mid))
					return
				}

				data.subscribe(object : Subscriber<Any> {

					private lateinit var stream: Subscription

					override fun onSubscribe(sub: Subscription) {
						state.subscriptions[mid] = sub

						stream = sub
						stream.request(1)
					}

					override fun onNext(value: Any) {
						if (value !is ExecutionResult) {
							throw IllegalStateException("Expected ExecutionResult, got ${value::class.simpleName}")
						}

						val payload = MessageParser.toJsonTree(value.toSpecification()) as JsonObject

						conn.send(Next(mid, payload))
						stream.request(1)
					}

					override fun onError(error: Throwable) {
						runBlocking {
							adapter.onError(state.context, error)
						}

						conn.send(Complete(mid))
						state.subscriptions.remove(mid)
					}

					override fun onComplete() {
						conn.send(Complete(mid))
						state.subscriptions.remove(mid)
					}

				})
			}
			is Complete -> {
				state.subscriptions[msg.id]?.cancel()
			}
			is Ping -> {
				conn.send(Pong())
			}
			else -> conn.close(CloseCode.BadRequest)
		}
	}

	override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
		val state = sockets.remove(conn)

		if (state != null) {
			for (sub in state.subscriptions.values) {
				sub.cancel()
			}

			if (state.acknowledged) {
				runBlocking {
					adapter.onDisconnect(state.context, code, reason)
				}
			}
		}
	}

	override fun onError(conn: WebSocket?, err: Exception) {
		if (conn == null) {
			startupTask.completeExceptionally(err)
			return
		}

		val state = sockets.remove(conn)

		if (state != null) {
			for (sub in state.subscriptions.values) {
				sub.cancel()
			}

			if (state.acknowledged) {
				runBlocking {
					adapter.onError(state.context, err)
					adapter.onDisconnect(state.context, -1, err.message ?: "Unknown WebSocket error")
				}
			}
		}
	}

	override fun onStart() {
		startupTask.complete(Unit)
	}

	companion object {
		const val PROTOCOL = "graphql-transport-ws"
		const val DEFAULT_HOST = "127.0.0.1"
		const val DEFAULT_PORT = 4000

		private fun buildDraft(): List<Draft> {
			return listOf(Draft_6455(
				listOf(),
				listOf(Protocol(PROTOCOL))
			))
		}
	}

}