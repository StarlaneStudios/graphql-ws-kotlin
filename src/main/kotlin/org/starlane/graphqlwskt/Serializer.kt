package org.starlane.graphqlwskt

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal val MessageParser = GsonBuilder()
	.registerTypeAdapter(Message::class.java, MessageAdapter())
	.create()

class MessageAdapter : JsonDeserializer<Message> {

	override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Message {
		val content = json.asJsonObject
		val type = content.get("type").asString

		val target = when (type) {
			"connection_init" -> Message.ConnectionInit::class.java
			"connection_ack" -> Message.ConnectionAck::class.java
			"subscribe" -> Message.Subscribe::class.java
			"next" -> Message.Next::class.java
			"error" -> Message.Error::class.java
			"complete" -> Message.Complete::class.java
			"ping" -> Message.Ping::class.java
			"pong" -> Message.Pong::class.java
			else -> throw IllegalArgumentException("Unknown message type: $type")
		}

		return context.deserialize(json, target)
	}

}