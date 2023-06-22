package org.starlane.graphqlwskt.client

import com.google.gson.Gson
import graphql.GraphQLException

class GraphQLQueryException(
	val errors: Array<Map<String, Any>>
) : GraphQLException("GraphQL server returned errors: " + Gson().toJson(errors))