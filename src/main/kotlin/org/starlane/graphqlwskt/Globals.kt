package org.starlane.graphqlwskt

import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.protocols.Protocol

typealias Payload = Map<String, Any?>

const val PROTOCOL = "graphql-transport-ws"
const val DEFAULT_HOST = "127.0.0.1"
const val DEFAULT_PORT = 4000

internal fun buildDraft(): Draft = Draft_6455(
	listOf(),
	listOf(Protocol(PROTOCOL))
)

internal fun buildDraftList(): List<Draft> = listOf(
	buildDraft()
)