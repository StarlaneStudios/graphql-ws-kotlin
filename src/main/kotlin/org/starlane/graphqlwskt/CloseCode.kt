package org.starlane.graphqlwskt

/**
 * A code representing a socket termination reason
 */
enum class CloseCode(
	val code: Int,
	val reason: String
) {
	InternalServerError(4500, "Internal Server Error"),
	InternalClientError(4005, "Internal Client Error"),
	BadRequest(4400, "Invalid message received"),
	BadResponse(4004, "Bad Response"),
	Unauthorized(4401, "Unauthorized"),
	Forbidden(4403, "Forbidden"),
	SubprotocolNotAcceptable(4406, "Subprotocol not acceptable"),
	ConnectionInitialisationTimeout(4408, "Connection initialisation timeout"),
	ConnectionAcknowledgementTimeout(4504, "Connection acknowledgement timeout"),
	SubscriberAlreadyExists(4409, "Subscriber already exists"),
	TooManyInitialisationRequests(4429, "Too many initialisation requests"),
}