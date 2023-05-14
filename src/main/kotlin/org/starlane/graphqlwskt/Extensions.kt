package org.starlane.graphqlwskt

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import graphql.GraphQLContext
import org.java_websocket.WebSocket

/**
 * Retrieves the WebSocket connection from the context
 */
fun GraphQLContext.getSocket(): WebSocket = get("__socket")

// ---------- Internal extensions ----------

internal fun WebSocket.close(code: CloseCode) {
	this.close(code.code, code.reason)
}

internal fun WebSocket.send(msg: Message) {
	this.send(MessageParser.toJson(msg))
}

internal inline fun <reified T> Gson.fromJson(json: String): T {
	return fromJson(json, object : TypeToken<T>() {}.type)
}

internal inline fun <reified T> Gson.fromJson(json: JsonObject): T {
	return fromJson(json, object : TypeToken<T>() {}.type)
}