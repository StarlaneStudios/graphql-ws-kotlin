package org.starlane.graphqlwskttest.server.fetchers

import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.starlane.graphqlwskt.util.FlowDataFetcher

class SubscriptionExample : FlowDataFetcher<String> {

	val responses = listOf(
		"Hello World",
		"Foo Bar",
		"Lorem Ipsum"
	)

	override fun resolve(environment: DataFetchingEnvironment): Flow<String> {
		return responses.asFlow().map {
			delay(1000)
			it
		}
	}

}