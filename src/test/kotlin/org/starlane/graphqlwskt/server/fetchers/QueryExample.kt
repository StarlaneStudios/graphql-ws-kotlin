package org.starlane.graphqlwskt.server.fetchers

import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.delay
import org.starlane.graphqlwskt.util.AsyncDataFetcher

class QueryExample : AsyncDataFetcher<String> {

	val responses = listOf(
		"Hello World",
		"Foo Bar",
		"Lorem Ipsum"
	)

	override suspend fun resolve(environment: DataFetchingEnvironment): String {
		delay(500)

		return responses.random()
	}

}