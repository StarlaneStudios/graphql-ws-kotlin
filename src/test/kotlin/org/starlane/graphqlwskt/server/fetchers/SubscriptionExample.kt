package org.starlane.graphqlwskt.server.fetchers

import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.starlane.graphqlwskt.util.FlowDataFetcher

class SubscriptionExample : FlowDataFetcher<String> {

	val responses = listOf(
		"Hello World",
		"Foo Bar",
		"Lorem Ipsum"
	)

	override fun resolve(environment: DataFetchingEnvironment): Flow<String> {
		return flow {
			throw Error("pepega moment")
		}
	}

}