package org.starlane.graphqlwskt.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.withContext
import org.starlane.graphqlwskt.OperationRequest
import org.starlane.graphqlwskt.Payload
import java.net.URI
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 *
 */
class GraphQLClient(
	val endpoint: URI,
	val keepAlive: Long = 10,
	val retryAttempts: Int = 5,
	val initTimeout: Long = 5000,
	val context: CoroutineContext = EmptyCoroutineContext,
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
			keepAlive,
			retryAttempts,
			context,
			initTimeout,
			params
		).also {
			handler = it
		}

		withContext(Dispatchers.IO) {
			client.connectBlocking()
		}
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
	fun <T> subscribe(
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
		))
	}

	/**
	 * Send a GraphQL query to the server and
	 * receive a single response. For subscriptions
	 * please use the [subscribe] function.
	 */
	suspend fun <T> query(
		query: String,
		operationName: String? = null,
		variables: Map<String, Any>? = null,
		extensions: Map<String, Any>? = null
	): T {
		return subscribe<T>(query, operationName, variables, extensions).single()
	}

}