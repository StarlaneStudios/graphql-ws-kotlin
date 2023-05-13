package org.starlane.graphqlwskt

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import graphql.GraphQLContext
import java.lang.reflect.Type

internal val GraphqlGson = GsonBuilder()
	.registerTypeAdapter(Message::class.java, MessageAdapter())
	.registerTypeAdapter(GraphQLContext::class.java, ContextAdapter())
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

class ContextAdapter : JsonDeserializer<GraphQLContext>, JsonSerializer<GraphQLContext> {

	override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): GraphQLContext {
		val token = object: TypeToken<HashMap<String, Any>>() {}.type
		val content = context.deserialize<Map<String, Any>>(json, token)

		return GraphQLContext.of(content)
	}

	override fun serialize(src: GraphQLContext, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
		val result = JsonObject()

		src.stream().forEach { (key, value) ->
			result.add(key as String, context.serialize(value))
		}

		return result
	}

}