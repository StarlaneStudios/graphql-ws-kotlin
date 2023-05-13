package org.starlane.graphqlwskt.util

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asPublisher
import org.reactivestreams.Publisher

/**
 * An implementation of a data fetcher which collects
 * the result of a [Flow] and returns it as a [Publisher].
 * This is primarily useful for GraphQL subscriptions.
 */
interface FlowDataFetcher<T : Any> : DataFetcher<Publisher<T>> {

	fun resolve(environment: DataFetchingEnvironment): Flow<T>

	override fun get(environment: DataFetchingEnvironment): Publisher<T> {
		return resolve(environment).asPublisher()
	}

}