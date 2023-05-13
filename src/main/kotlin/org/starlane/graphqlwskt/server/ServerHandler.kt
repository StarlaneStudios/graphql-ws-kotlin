package org.starlane.graphqlwskt.server

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.GraphQLContext
import graphql.execution.SubscriptionExecutionStrategy
import kotlinx.coroutines.*
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
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
) : WebSocketServer(address) {

	internal val startupTask = CompletableDeferred<Unit>()
	internal val sockets = HashMap<WebSocket, SocketState>()

	override fun onOpen(conn: WebSocket, handshake: ClientHandshake) = runBlocking(context) r@ {
		val path = handshake.resourceDescriptor

		println("path = $path")

		if (!conn.protocol.acceptProvidedProtocol(PROTOCOL)) {
			conn.close(CloseCode.SubprotocolNotAcceptable)
			return@r
		}

		val context = GraphQLContext.newContext()
			.put("__socket", conn)
			.build()

		val state = SocketState(
			socket = conn,
			initialized = false,
			acknowledged = false,
			context = context,
			subscriptions = HashMap(),
			graphql = null,
		)

		launch {
			delay(initTimeout)

			if (!state.initialized && conn.isOpen) {
				conn.close(CloseCode.ConnectionInitialisationTimeout)
			}
		}

		sockets[conn] = state
	}

	override fun onMessage(conn: WebSocket, message: String) = runBlocking(context) r@ {
		val state = sockets[conn] ?: run {
			conn.close(CloseCode.InternalServerError)
			return@r
		}

		val msg = try {
			GraphqlGson.fromJson<Message>(message)
		} catch(e: Exception) {
			conn.close(CloseCode.BadRequest)
			return@r
		}

		when(msg) {
			is ConnectionInit -> {
				if (state.initialized) {
					conn.close(CloseCode.TooManyInitialisationRequests)
					return@r
				}

				state.initialized = true
				state.context.putAll(msg.payload)

				val result = adapter.onConnect(state.context)

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
					return@r
				}

				if (msg.id in state.subscriptions) {
					conn.close(CloseCode.SubscriberAlreadyExists)
					return@r
				}

				val mid = msg.id
				val input = ExecutionInput.newExecutionInput()
					.query(msg.payload.query)
					.operationName(msg.payload.operationName)
					.variables(msg.payload.variables)
					.extensions(msg.payload.extensions)

				val result = withContext(Dispatchers.IO) {
					state.graphql!!.execute(input)
				}

				if (result.errors.isNotEmpty()) {
					conn.send(Error(mid, result.errors.toTypedArray()))
					conn.send(Complete(mid))
					return@r
				}

				val data = result.getData<Any>()

				if (data !is Publisher<*>) {
					conn.send(Next(mid, data))
					conn.send(Complete(mid))
					return@r
				}

				data.subscribe(object : Subscriber<Any> {

					override fun onSubscribe(sub: Subscription) {
						state.subscriptions[mid] = sub
						sub.request(Long.MAX_VALUE)
					}

					override fun onNext(value: Any) {
						conn.send(Next(mid, value))
					}

					override fun onError(error: Throwable) {
						conn.send(Error(mid, arrayOf(error)))
						conn.send(Complete(mid))
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

	override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) = runBlocking(context) r@ {
		val state = sockets.remove(conn)

		if (state != null) {
			for (sub in state.subscriptions.values) {
				sub.cancel()
			}

			if (state.acknowledged) {
				adapter.onDisconnect(state.context, code, reason)
			}
		}
	}

	override fun onError(conn: WebSocket?, err: Exception) = runBlocking(context) r@ {
		if (conn == null) {
			startupTask.completeExceptionally(err)
			return@r
		}

		val state = sockets.remove(conn)

		if (state != null) {
			for (sub in state.subscriptions.values) {
				sub.cancel()
			}

			if (state.acknowledged) {
				adapter.onError(state.context, err)
				adapter.onDisconnect(state.context, -1, err.message ?: "Unknown WebSocket error")
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
	}

}