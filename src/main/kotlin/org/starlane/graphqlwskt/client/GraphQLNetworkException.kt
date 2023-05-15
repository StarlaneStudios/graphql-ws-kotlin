package org.starlane.graphqlwskt.client

import graphql.GraphQLException

class GraphQLNetworkException: GraphQLException("Failed to connect to GraphQL server")