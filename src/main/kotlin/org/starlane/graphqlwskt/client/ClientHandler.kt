package org.starlane.graphqlwskt.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.starlane.graphqlwskt.*
import org.starlane.graphqlwskt.Message.*
import java.net.URI
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * The main client-side WebSocket implementation
 */
internal class ClientHandler(
	endpoint: URI,
	val keepAlive: Long = 10,
	val retryAttempts: Int = 5,
	val context: CoroutineContext,
	val initTimeout: Long = 5000,
	val params: (() -> Payload)? = null,
) : WebSocketClient(endpoint) {

	internal var initPayload: Payload? = null
	internal var acknowledged = false
	internal val scope = CoroutineScope(context)
	internal val subscriptions = HashMap<String, ActiveSubscription>()

	override fun onOpen(handshakedata: ServerHandshake?) {
		val payload = params?.invoke() ?: emptyMap()

		send(ConnectionInit(payload))

		scope.launch {
			delay(initTimeout)

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

				scope.launch {
					while (isOpen) {
						delay(keepAlive)
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
	}

	override fun onError(ex: Exception?) {
		scope.cancel()
	}

	fun <T : Any> subscribe(request: OperationRequest, type: KClass<T>): Flow<T> {
		val id = UUID.randomUUID().toString()

		send(Subscribe(id, request))

		return channelFlow {
			subscriptions[id] = ActiveSubscription(
				onNext = {
					trySend(Gson().fromJson(it, type.java))
				},
				onComplete = {
					close()
				},
				onError = {
					close(it)
				}
			)
			awaitClose()
		}
	}

}