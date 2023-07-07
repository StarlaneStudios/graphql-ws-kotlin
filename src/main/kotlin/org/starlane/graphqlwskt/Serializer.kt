package org.starlane.graphqlwskt

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal val MessageParser = GsonBuilder()
	.registerTypeAdapter(Message::class.java, MessageAdapter())
	.registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
	.registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
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

class LocalDateSerializer : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

	override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate {
		return LocalDate.parse(json.asString, DateTimeFormatter.ISO_DATE)
	}

	override fun serialize(src: LocalDate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
		return JsonPrimitive(src.format(DateTimeFormatter.ISO_DATE))
	}

}

class LocalDateTimeSerializer : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

	override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {
		return LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_DATE_TIME)
	}

	override fun serialize(src: LocalDateTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
		return JsonPrimitive(src.format(DateTimeFormatter.ISO_DATE_TIME))
	}

}