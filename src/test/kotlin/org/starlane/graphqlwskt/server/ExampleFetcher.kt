package org.starlane.graphqlwskt.server

import graphql.schema.DataFetchingEnvironment
import org.starlane.graphqlwskt.util.DataResolver

class ExampleFetcher : DataResolver<String> {

	val responses = listOf(
		"Hello World",
		"Foo Bar",
		"Lorem Ipsum"
	)

	override suspend fun resolve(environment: DataFetchingEnvironment): String {
		return responses.random()
	}

}