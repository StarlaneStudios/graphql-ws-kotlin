package org.starlane.graphqlwskt.server

import graphql.GraphQLContext

/**
 *
 */
interface  GraphqlServerAdapter {

	/**
	 * Called when the client requests initialization
	 * of the GraphQL connection.
	 *
	 * @param ctx The context passed by the client
	 */
	suspend fun onConnect(ctx: GraphQLContext): ConnectResult

	/**
	 * Called when the client disconnects for any reason
	 *
	 * @param ctx The context passed by the client
	 * @param code The close code
	 * @param reason The close reason
	 */
	suspend fun onDisconnect(ctx: GraphQLContext, code: Int, reason: String)

	/**
	 * Called when the client requests a new GraphQL operation
	 *
	 * @param ctx The context passed by the client
	 * @param operation The operation information
	 */
	suspend fun onOperation(ctx: GraphQLContext, operation: Any)

	/**
	 * Called when an error was caught at any point during execution
	 *
	 * @param ctx The context passed by the client
	 * @param error The error that was caught
	 */
	suspend fun onError(ctx: GraphQLContext, error: Throwable)

}