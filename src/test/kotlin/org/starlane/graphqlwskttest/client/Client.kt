package org.starlane.graphqlwskttest.client

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.starlane.graphqlwskt.client.GraphQLClient
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

		val result = client.query<JsonObject>("query { example }")

		println(result)

		println("Done. Moving to subscriptions")

		client.subscribe<JsonObject>("subscription { examples }").collect {
			println(it)
		}

		client.close()
	}
}