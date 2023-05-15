package org.starlane.graphqlwskt.server

import org.starlane.graphqlwskt.DEFAULT_HOST
import org.starlane.graphqlwskt.DEFAULT_PORT
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * The GraphQL server engine which listens to incoming
 * connections and handles them
 */
class GraphQLServer(
	val adapter: GraphQLServerAdapter,
	val address: InetSocketAddress = InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT),
	val path: String = "/subscriptions",
	val initTimeout: Long = 5000,
	val context: CoroutineContext = EmptyCoroutineContext
) {

	private val handler = ServerHandler(address, adapter, path, initTimeout, context)
	private var initialized = false

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
		if (initialized) {
			throw IllegalStateException("Server can only be started once")
		}

		handler.start()
		handler.startupTask.await()
		initialized = true
	}

	/**
	 * Stop the GraphQL Web Socket server and
	 * disconnects any connected clients
	 */
	fun stop() = handler.stop()

}