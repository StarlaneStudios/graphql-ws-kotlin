package org.starlane.graphqlwskt

import graphql.GraphQLContext

/**
 * The implementation of a single GraphQL resolver
 */
interface GraphqlResolver<P, A> {

	/**
	 * Execute the resolver based on an incoming request
	 * and return a produced response value.
	 *
	 * @param parent The parent object
	 * @param args The arguments passed to the resolver
	 * @param ctx The context of the request
	 */
	suspend fun execute(parent: P, args: A, ctx: GraphQLContext): Any?

}