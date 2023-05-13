package org.starlane.graphqlwskt

/**
 * An operation sent by the client to the server
 */
data class OperationRequest(
	val operationName: String?,
	val query: String,
	val variables: Map<String, Any>?,
	val extensions: Map<String, Any>?
)