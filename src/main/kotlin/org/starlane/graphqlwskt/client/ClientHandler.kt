package org.starlane.graphqlwskt.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.starlane.graphqlwskt.*
import org.starlane.graphqlwskt.Message.*
import java.net.URI
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * The main client-side WebSocket implementation
 */
internal class ClientHandler(
	endpoint: URI,
	val retryAttempts: Int,
	val context: CoroutineContext,
	val keepAlive: Duration,
	val initTimeout: Duration,
	val params: (() -> Payload)?,
) : WebSocketClient(endpoint, buildDraft()) {

	internal var initPayload: Payload? = null
	internal var acknowledged = false
	internal val scope = CoroutineScope(context)
	internal val subscriptions = HashMap<String, ActiveSubscription>()
	internal val completable = CompletableDeferred<Unit>()

	override fun onOpen(handshakedata: ServerHandshake?) {
		val payload = params?.invoke() ?: emptyMap()

		send(ConnectionInit(payload))

		scope.launch {
			delay(initTimeout.toMillis())

			if (!acknowledged && isOpen) {
				close(CloseCode.ConnectionAcknowledgementTimeout)
			}
		}
	}

	override fun onMessage(message: String) {
		val msg = try {
			MessageParser.fromJson<Message>(message)
		} catch(e: Exception) {
			close(CloseCode.BadResponse)
			return
		}

		when(msg) {
			is ConnectionAck -> {
				acknowledged = true

				if (msg.payload.isNotEmpty()) {
					initPayload = msg.payload
				}

				completable.complete(Unit)

				scope.launch {
					while (isOpen) {
						delay(keepAlive.toMillis())
						send(Ping())
					}
				}
			}
			is Next -> {
				subscriptions[msg.id]?.onNext?.invoke(msg.payload ?: JsonObject())
			}
			is Error -> {
				subscriptions[msg.id]?.onError?.invoke(GraphQLQueryException(msg.payload))
			}
			is Complete -> {
				subscriptions[msg.id]?.onComplete?.invoke()
			}
			is Pong -> {
				// ignore
			}
			else -> close(CloseCode.BadResponse)
		}
	}

	override fun onClose(code: Int, reason: String?, remote: Boolean) {
		scope.cancel()
		subscriptions.clear()
	}

	override fun onError(ex: Exception?) {
		scope.cancel()
		subscriptions.clear()
	}

	fun <T : Any> subscribe(request: OperationRequest, type: KClass<T>): Flow<T> {
		val id = UUID.randomUUID().toString()

		return channelFlow {
			subscriptions[id] = ActiveSubscription(
				onNext = {
					trySend(Gson().fromJson(it, type.java))
				},
				onComplete = {
					close()
					subscriptions.remove(id)
				},
				onError = {
					close(it)
					subscriptions.remove(id)
				}
			)

			send(Subscribe(id, request))
			awaitClose()
		}
	}
}