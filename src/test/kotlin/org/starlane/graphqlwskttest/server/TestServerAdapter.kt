package org.starlane.graphqlwskttest.server

import graphql.GraphQLContext
import graphql.schema.GraphQLSchema
import org.starlane.graphqlwskt.server.ConnectResult
import org.starlane.graphqlwskt.server.GraphQLServerAdapter

class TestServerAdapter(
	val schema: GraphQLSchema
) : GraphQLServerAdapter {

	override suspend fun onConnect(ctx: GraphQLContext): ConnectResult {
		println("- onConnect")

		return ConnectResult.Success(schema)
	}

	override suspend fun onDisconnect(ctx: GraphQLContext, code: Int, reason: String) {
		println("- onDisconnect")
	}

	override suspend fun onOperation(ctx: GraphQLContext, operation: Any) {
		println("- onOperation")
	}

	override suspend fun onError(ctx: GraphQLContext, error: Throwable) {
		println("- onError")

		error.printStackTrace()
	}

}