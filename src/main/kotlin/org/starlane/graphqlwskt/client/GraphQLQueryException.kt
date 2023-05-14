package org.starlane.graphqlwskt.client

import graphql.GraphQLException

class GraphQLQueryException(
	val errors: Array<Map<String, Any>>
) : GraphQLException("GraphQL server returned errors")