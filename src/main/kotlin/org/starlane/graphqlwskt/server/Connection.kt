package org.starlane.graphqlwskt.server

import graphql.GraphQLContext
import org.java_websocket.WebSocket

/**
 * Represents a connection to the server including
 * public information about the connection.
 */
class Connection(
	val socket: WebSocket,
	val context: GraphQLContext
)