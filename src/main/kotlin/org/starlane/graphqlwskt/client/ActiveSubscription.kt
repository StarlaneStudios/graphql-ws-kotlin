package org.starlane.graphqlwskt.client

import com.google.gson.JsonObject

/**
 * Represents an active subscription
 */
class ActiveSubscription(
	var onComplete: () -> Unit,
	var onNext: (JsonObject) -> Unit,
	var onError: (Throwable) -> Unit
)