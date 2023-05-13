package org.starlane.graphqlwskt

/**
 * Represents a message sent over a GraphQL connection
 *
 * @see <a href="https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md">GraphQL WS Protocol</a>
 */
sealed class Message(
	val type: String
) {

	/**
	 * Client -> Server: Indicate a request to initialize a connection
	 */
	class ConnectionInit(
		val payload: Map<String, Any?> = emptyMap()
	) : Message("connection_init")

	/**
	 * Server -> Client: Respond to a connection_init request
	 */
	class ConnectionAck(
		val payload: Map<String, Any?> = emptyMap()
	) : Message("connection_ack")

	/**
	 * Client -> Server: Subscribe to a new query
	 */
	class Subscribe(
		val id: String,
		val payload: OperationRequest
	) : Message("subscribe")

	/**
	 * Server -> Client: Respond with a new subscription result
	 */
	class Next(
		val id: String,
		val payload: Any?
	) : Message("next")

	/**
	 * Server -> Client: Respond with a subscription error
	 */
	class Error(
		val id: String,
		val payload: Array<Map<String, Any>>
	) : Message("error")

	/**
	 * Client -> Server: Unsubscribe from a subscription
	 * Server -> Client: Complete a subscription
	 */
	class Complete(
		val id: String
	) : Message("complete")

	/**
	 * Client -> Server: Ping the server
	 */
	class Ping : Message("ping")

	/**
	 * Server -> Client: Respond to a ping
	 */
	class Pong : Message("pong")
}