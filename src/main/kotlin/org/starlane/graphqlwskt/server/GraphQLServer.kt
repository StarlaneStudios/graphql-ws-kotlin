package org.starlane.graphqlwskt.server

import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The GraphQL server engine which listens to incoming
 * connections and handles them
 */
class GraphQLServer(
	val adapter: GraphQLServerAdapter,
	val address: InetSocketAddress = InetSocketAddress(ServerHandler.DEFAULT_HOST, ServerHandler.DEFAULT_PORT),
	val path: String = "/subscriptions",
	val initTimeout: Long = 5000,
	val context: CoroutineContext = EmptyCoroutineContext
) {

	private val handler = ServerHandler(address, adapter, path, initTimeout, context)

	/**
	 * Returns a list of all active connections
	 */
	val connections: Collection<Connection>
		get() = handler.sockets.values.map {
			Connection(it.socket, it.context)
		}

	/**
	 * Start the GraphQL Web Socket server in
	 * a dedicated thread and listen for incoming
	 * connections.
	 *
	 * Suspends until the server is bound to the port,
	 * throwing an exception if the server could not
	 * be started.
	 */
	suspend fun start() {
		handler.start()
		handler.startupTask.await()
	}

	/**
	 * Stop the GraphQL Web Socket server
	 */
	fun stop() = handler.stop()

}