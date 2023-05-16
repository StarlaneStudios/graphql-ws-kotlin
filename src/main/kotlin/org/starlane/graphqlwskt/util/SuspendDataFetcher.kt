package org.starlane.graphqlwskt.util

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

/**
 * An implementation of a data fetcher which provides
 * suspendable resolution of the data
 */
interface SuspendDataFetcher<T> : DataFetcher<Future<T>> {

	suspend fun resolve(environment: DataFetchingEnvironment): T

	override fun get(environment: DataFetchingEnvironment): Future<T> {
		val future = CompletableFuture<T>()

		runBlocking {
			try {
				future.complete(resolve(environment))
			} catch(err: Throwable) {
				future.completeExceptionally(err)
			}
		}

		return future
	}

}