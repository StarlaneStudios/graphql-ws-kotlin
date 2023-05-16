package org.starlane.graphqlwskttest.server

import kotlinx.coroutines.runBlocking
import org.starlane.graphqlwskt.server.GraphQLServer
import org.starlane.graphqlwskt.util.buildSchemaResource
import org.starlane.graphqlwskt.util.compileSchema
import org.starlane.graphqlwskttest.server.fetchers.QueryExample
import org.starlane.graphqlwskttest.server.fetchers.SubscriptionExample

fun main() {

	// Load the schema resource and define data fetchers
	val definition = buildSchemaResource("example.graphql") {
		type("Query") {
			fetcher("example", QueryExample())
		}
		type("Subscription") {
			fetcher("examples", SubscriptionExample())
		}
	}

	// Compile the schema into a GraphQLSchema
	val schema = compileSchema(definition)

	// Initialize the server instance
	val server = GraphQLServer(
		adapter = TestServerAdapter(schema)
	)

	// Start the server and await port binding
	runBlocking {
		server.start()
	}

	println("Server running")
}