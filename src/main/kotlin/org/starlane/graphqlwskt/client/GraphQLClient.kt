package org.starlane.graphqlwskt.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.withContext
import org.starlane.graphqlwskt.OperationRequest
import org.starlane.graphqlwskt.Payload
import java.net.URI
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 *
 */
class GraphQLClient(
	val endpoint: URI,
	val retryAttempts: Int = 5,
	val context: CoroutineContext = EmptyCoroutineContext,
	val keepAlive: Duration = Duration.ofSeconds(15),
	val initTimeout: Duration = Duration.ofSeconds(5),
	val params: (() -> Payload)? = null
) {

	private var handler: ClientHandler? = null

	/**
	 * Returns true if the client is connected
	 */
	val isConnected: Boolean
		get() = handler?.isOpen == true

	/**
	 * Returns true if the client is connected and
	 * the server has acknowledged the connection
	 */
	val isAcknowledged: Boolean
		get() = handler?.isOpen == true && handler?.acknowledged == true

	/**
	 * Returns the payload sent by the server after
	 * the connection was opened.
	 */
	val initPayload: Payload?
		get() {
			if (!isAcknowledged) {
				throw IllegalStateException("Client not acknowledged")
			}

			return handler?.initPayload
		}

	/**
	 * Open a connection to the given endpoint
	 */
	suspend fun connect() {
		if (handler?.isOpen == true) {
			throw IllegalStateException("Client already connected")
		}

		val client = ClientHandler(
			endpoint,
			retryAttempts,
			context,
			keepAlive,
			initTimeout,
			params
		).also {
			handler = it
		}

		withContext(Dispatchers.IO) {
			val success = client.connectBlocking()

			if(!success) {
				throw GraphQLNetworkException()
			}
		}

		client.completable.await()
	}

	/**
	 * Disconnect the active connection
	 */
	fun close() {
		handler?.close()
		handler = null
	}

	/**
	 * Send a GraphQL query to the server and
	 * subscribe to the response. This function
	 * can also be used for Query and Mutation
	 * operations.
	 */
	fun <T : Any> subscribe(
		type: KClass<T>,
		query: String,
		operationName: String? = null,
		variables: Map<String, Any>? = null,
		extensions: Map<String, Any>? = null
	): Flow<T> {
		if (!isAcknowledged) {
			throw IllegalStateException("Client not acknowledged")
		}

		return handler!!.subscribe(OperationRequest(
			query = query,
			operationName = operationName,
			variables = variables,
			extensions = extensions
		), type)
	}

	/**
	 * Send a GraphQL query to the server and
	 * subscribe to the response. This function
	 * can also be used for Query and Mutation
	 * operations.
	 */
	inline fun <reified T : Any> subscribe(
		query: String,
		operationName: String? = null,
		variables: Map<String, Any>? = null,
		extensions: Map<String, Any>? = null
	): Flow<T> {
		return subscribe(T::class, query, operationName, variables, extensions)
	}

	/**
	 * Send a GraphQL query to the server and
	 * receive a single response. For subscriptions
	 * please use the [subscribe] function.
	 */
	suspend fun <T : Any> query(
		type: KClass<T>,
		query: String,
		operationName: String? = null,
		variables: Map<String, Any>? = null,
		extensions: Map<String, Any>? = null
	): T {
		return subscribe(type, query, operationName, variables, extensions).single()
	}

	/**
	 * Send a GraphQL query to the server and
	 * receive a single response. For subscriptions
	 * please use the [subscribe] function.
	 */
	suspend inline fun <reified T : Any> query(
		query: String,
		operationName: String? = null,
		variables: Map<String, Any>? = null,
		extensions: Map<String, Any>? = null
	): T {
		return query(T::class, query, operationName, variables, extensions)
	}

}