package org.starlane.graphqlwskt

import graphql.schema.DataFetcher
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal typealias Resolver = (CoroutineScope) -> DataFetcher<Any>
internal typealias FieldMap = MutableMap<String, Resolver>
internal typealias TypeMap = MutableMap<String, FieldMap>

/**
 * An generic empty GraphQL schema
 */
val EmptySchema: GraphQLSchema
	get() = GraphQLSchema.newSchema().build()

/**
 * Build a new GraphQL schema
 *
 * @param resource The schema resource name
 * @param builder The schema builder
 */
fun buildSchema(resource: String, builder: SchemaBuilder.() -> Unit): SchemaDefinition {
	val stream = resource::class.java.classLoader.getResourceAsStream("${resource}.graphql")
		?: throw IllegalArgumentException("Schema resource \"$resource\" not found")

	val schemaText = stream.readBytes().contentToString()
	val typeMap: TypeMap = HashMap()

	SchemaBuilder(typeMap).apply(builder)

	return SchemaDefinition(
		schemaText,
		typeMap
	)
}

/**
 * Combine the given schema definitions into a single schema
 *
 * @param schemas The schema definitions
 * @param context The coroutine context to use for resolvers
 */
fun combineSchemas(
	schemas: Collection<SchemaDefinition>,
	context: CoroutineContext = EmptyCoroutineContext
): GraphQLSchema {
	val resolvers = schemas.map { it.resolvers }
	val schema = schemas.joinToString { it.schema }
	val parser = SchemaParser()
	val generator = SchemaGenerator()
	val definition = parser.parse(schema)
	val wiring = RuntimeWiring.newRuntimeWiring()
	val scope = CoroutineScope(context)

	for (typeMap in resolvers) {
		for ((typeName, fieldMap) in typeMap) {
			wiring.type(typeName) { builder ->
				builder.apply {
					for ((fieldName, resolver) in fieldMap) {
						builder.dataFetcher(fieldName, resolver(scope))
					}
				}
			}
		}
	}

	return generator.makeExecutableSchema(definition, wiring.build())
}

/**
 * The definition of a GraphQL schema
 */
class SchemaDefinition internal constructor(
	val schema: String,
	val resolvers: TypeMap
)

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
	fun <P, A> resolve(name: String, resolver: GraphqlResolver<P, A>) {
		val argsType = resolver::class.nestedClasses.firstOrNull {
			it.simpleName == "Args"
		} ?: throw IllegalArgumentException("Resolver ${resolver::class.simpleName} does not have an Args class")

		resolverMap[name] = { scope ->
			DataFetcher<Any> { env ->
				val parent = env.getSource<P>()
				val tree = GraphqlGson.toJsonTree(env.arguments)

				@Suppress("UNCHECKED_CAST")
				val args = GraphqlGson.fromJson(tree, argsType.java) as A

				CompletableFuture<Any?>().apply {
					scope.launch {
						try {
							complete(resolver.execute(parent, args, env.graphQlContext))
						} catch(err: Throwable) {
							completeExceptionally(err)
						}
					}
				}
			}
		}
	}

}