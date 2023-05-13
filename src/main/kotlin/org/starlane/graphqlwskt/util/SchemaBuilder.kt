package org.starlane.graphqlwskt.util

import graphql.schema.DataFetcher
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser

internal typealias FieldMap = MutableMap<String, DataFetcher<*>>
internal typealias TypeMap = MutableMap<String, FieldMap>

/**
 * A schema definition encapsulates a GraphQL schema and its resolvers
 */
class SchemaDefinition(
	val schema: String,
	val resolvers: TypeMap
)

/**
 * Build a new GraphQL schema given a resource path
 * and schema builder.
 *
 * @param resource The schema resource name
 * @param builder The schema builder
 */
fun buildSchemaResource(resource: String, builder: SchemaBuilder.() -> Unit): SchemaDefinition {
	val stream = builder::class.java.classLoader.getResourceAsStream(resource)
		?: throw IllegalArgumentException("Schema resource \"$resource\" not found")

	return buildSchema(
		schema = stream.readBytes().decodeToString(),
		builder = builder
	)
}

/**
 * Build a new GraphQL schema given a resource path
 * and schema builder.
 *
 * @param schema The schema SDL
 * @param builder The schema builder
 */
fun buildSchema(schema: String, builder: SchemaBuilder.() -> Unit): SchemaDefinition {
	val typeMap: TypeMap = HashMap()

	SchemaBuilder(typeMap).apply(builder)

	return SchemaDefinition(
		schema,
		typeMap
	)
}

/**
 * Combine the given schema definition into a single schema
 *
 * @param schema The schema definition
 */
fun compileSchema(
	schema: SchemaDefinition
): GraphQLSchema {
	return compileSchemas(listOf(schema))
}

/**
 * Combine the given schema definitions into a single schema
 *
 * @param schemas The schema definitions
 */
fun compileSchemas(
	schemas: Collection<SchemaDefinition>
): GraphQLSchema {
	val resolvers = schemas.map { it.resolvers }
	val schema = schemas.joinToString { it.schema }
	val parser = SchemaParser()
	val generator = SchemaGenerator()
	val definition = parser.parse(schema)
	val wiring = RuntimeWiring.newRuntimeWiring()

	for (typeMap in resolvers) {
		for ((typeName, fieldMap) in typeMap) {
			wiring.type(typeName) { builder ->
				builder.apply {
					for ((fieldName, resolver) in fieldMap) {
						builder.dataFetcher(fieldName, resolver)
					}
				}
			}
		}
	}

	return generator.makeExecutableSchema(definition, wiring.build())
}

/**
 * Build a new GraphQL schema
 */
class SchemaBuilder internal constructor(
	private val typeMap: TypeMap
) {

	/**
	 * Define a new type on this schema
	 *
	 * @param name The name of the type
	 * @param builder The type builder
	 */
	fun type(name: String, builder: TypeBuilder.() -> Unit) {
		val resolvers: FieldMap = HashMap()

		typeMap[name] = resolvers

		TypeBuilder(resolvers).apply(builder)
	}

}

/**
 * Build a new GraphQL schema type
 */
class TypeBuilder internal constructor(
	private val resolverMap: FieldMap
) {

	/**
	 * Define a new resolver for this type
	 *
	 * @param name The name of the resolver
	 * @param resolver The resolver
	 */
	fun fetcher(name: String, resolver: DataFetcher<*>) {
		resolverMap[name] = resolver
	}

}