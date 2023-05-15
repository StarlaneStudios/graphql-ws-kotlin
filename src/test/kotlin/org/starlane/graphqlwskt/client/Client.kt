package org.starlane.graphqlwskt.client

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.net.URI

/**
 * Copyright 2019-2023 (c) Starlane Studios. All Rights Reserved.
 *
 * @author Matthew (Mateo)
 */
fun main() {

	val client = GraphQLClient(
		endpoint = URI.create("ws://127.0.0.1:4000/subscriptions")
	)

	runBlocking {
		client.connect()

		client.query<String>("example").single().let {
			println(it)
		}

		println("Done. Moving to subscriptions")

		client.subscribe<String>("examples").collect {
			println(it)
		}
	}
}