package org.starlane.graphqlwskt.server

import graphql.GraphQL
import graphql.GraphQLContext
import org.java_websocket.WebSocket
import org.reactivestreams.Subscription

/**
 * Represents a connected socket including its state
 */
class SocketState(
	val socket: WebSocket,
	var graphql: GraphQL?,
	var initialized: Boolean,
	var acknowledged: Boolean,
	val context: GraphQLContext,
	val subscriptions: MutableMap<String, Subscription>
)