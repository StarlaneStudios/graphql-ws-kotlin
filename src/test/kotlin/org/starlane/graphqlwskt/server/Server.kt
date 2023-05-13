package org.starlane.graphqlwskt.server

import kotlinx.coroutines.runBlocking
import org.starlane.graphqlwskt.util.buildSchemaResource
import org.starlane.graphqlwskt.util.compileSchema

fun main() {
	val definition = buildSchemaResource("example.graphql") {
		type("Query") {
			fetcher("example", ExampleFetcher())
		}
	}

	val schema = compileSchema(definition)

	val server = GraphQLServer(
		adapter = TestServerAdapter(schema)
	)

	runBlocking {
		server.start()
	}

	println("Server running")
}