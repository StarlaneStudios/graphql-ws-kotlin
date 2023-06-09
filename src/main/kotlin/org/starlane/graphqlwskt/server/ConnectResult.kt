package org.starlane.graphqlwskt.server

import graphql.schema.GraphQLSchema
import org.starlane.graphqlwskt.Payload

/**
 * Represents the result of a GraphQL connection
 */
sealed class ConnectResult {

	/**
	 * The connection was successful
	 */
	class Success(
		val schema: GraphQLSchema,
		val payload: Payload? = null
	) : ConnectResult()

	/**
	 * The connection was unsuccessful and a
	 * 4403 Forbidden status will be sent
	 */
	object Failure : ConnectResult()

}

