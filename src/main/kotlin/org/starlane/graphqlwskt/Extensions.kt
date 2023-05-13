package org.starlane.graphqlwskt

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.java_websocket.WebSocket

internal fun WebSocket.close(code: CloseCode) {
	this.close(code.code, code.reason)
}

internal fun WebSocket.send(msg: Message) {
	this.send(GraphqlGson.toJson(msg))
}

internal inline fun <reified T> Gson.fromJson(json: String): T {
	return fromJson(json, object : TypeToken<T>() {}.type)
}