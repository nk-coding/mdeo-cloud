package com.mdeo.backend.service

import com.mdeo.common.model.Execution
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Base interface for all WebSocket messages exchanged between client and server.
 */
@Serializable
sealed interface WebSocketMessage {
    @SerialName("messageType")
    val type: String
}

/**
 * Message sent by client to subscribe to a project's events.
 *
 * @property projectId The UUID of the project to subscribe to
 */
@Serializable
data class SubscribeMessage(
    val projectId: String
) : WebSocketMessage {
    @SerialName("messageType")
    override val type: String = "subscribe"
}

/**
 * Message sent by client to unsubscribe from a project's events.
 *
 * @property projectId The UUID of the project to unsubscribe from
 */
@Serializable
data class UnsubscribeMessage(
    val projectId: String
) : WebSocketMessage {
    @SerialName("messageType")
    override val type: String = "unsubscribe"
}

/**
 * Notification message sent by server when an execution state changes.
 *
 * @property execution The updated execution data
 */
@Serializable
data class ExecutionStateChangedMessage(
    val execution: Execution
) : WebSocketMessage {
    @SerialName("messageType")
    override val type: String = "executionStateChanged"
}

/**
 * Represents an active WebSocket connection with its subscriptions.
 *
 * @property session The WebSocket session
 * @property userId The ID of the authenticated user
 * @property subscribedProjects Set of project IDs this connection is subscribed to
 */
data class WebSocketConnection(
    val session: DefaultWebSocketServerSession,
    val userId: String,
    val subscribedProjects: MutableSet<UUID> = mutableSetOf()
)

/**
 * Service for managing WebSocket connections and broadcasting notifications.
 * Handles multiple simultaneous connections per user and project subscriptions.
 */
class WebSocketNotificationService {
    private val logger = LoggerFactory.getLogger(WebSocketNotificationService::class.java)
    private val json = Json { encodeDefaults = true }
    
    private val connections = ConcurrentHashMap<String, WebSocketConnection>()
    private val connectionMutex = Mutex()

    /**
     * Registers a new WebSocket connection.
     *
     * @param connectionId Unique identifier for this connection
     * @param session The WebSocket session
     * @param userId The authenticated user's ID
     */
    suspend fun registerConnection(
        connectionId: String,
        session: DefaultWebSocketServerSession,
        userId: String
    ) {
        connectionMutex.withLock {
            connections[connectionId] = WebSocketConnection(session, userId)
            logger.info("WebSocket connection registered: $connectionId for user $userId")
        }
    }

    /**
     * Removes a WebSocket connection and cleans up its subscriptions.
     *
     * @param connectionId The connection ID to remove
     */
    suspend fun removeConnection(connectionId: String) {
        connectionMutex.withLock {
            connections.remove(connectionId)
            logger.info("WebSocket connection removed: $connectionId")
        }
    }

    /**
     * Subscribes a connection to a project's events.
     *
     * @param connectionId The connection ID
     * @param projectId The project UUID to subscribe to
     * @return True if subscription was successful, false if connection not found
     */
    suspend fun subscribe(connectionId: String, projectId: UUID): Boolean {
        return connectionMutex.withLock {
            val connection = connections[connectionId] ?: return@withLock false
            connection.subscribedProjects.add(projectId)
            logger.debug("Connection $connectionId subscribed to project $projectId")
            true
        }
    }

    /**
     * Unsubscribes a connection from a project's events.
     *
     * @param connectionId The connection ID
     * @param projectId The project UUID to unsubscribe from
     * @return True if unsubscription was successful, false if connection not found
     */
    suspend fun unsubscribe(connectionId: String, projectId: UUID): Boolean {
        return connectionMutex.withLock {
            val connection = connections[connectionId] ?: return@withLock false
            connection.subscribedProjects.remove(projectId)
            logger.debug("Connection $connectionId unsubscribed from project $projectId")
            true
        }
    }

    /**
     * Broadcasts an execution state change to all connections subscribed to the project.
     *
     * @param projectId The project UUID where the execution belongs
     * @param execution The updated execution data
     */
    suspend fun broadcastExecutionStateChange(projectId: UUID, execution: Execution) {
        val message = ExecutionStateChangedMessage(execution)
        val messageJson = json.encodeToString(message as WebSocketMessage)
        
        broadcastToProject(projectId, messageJson)
    }

    /**
     * Sends a JSON message to all connections subscribed to a specific project.
     *
     * @param projectId The target project UUID
     * @param messageJson The JSON-encoded message to send
     */
    private suspend fun broadcastToProject(projectId: UUID, messageJson: String) {
        val targetConnections = getSubscribedConnections(projectId)
        
        for (connection in targetConnections) {
            sendMessageSafely(connection, messageJson)
        }
    }

    /**
     * Gets all connections currently subscribed to a project.
     *
     * @param projectId The project UUID
     * @return List of connections subscribed to the project
     */
    private suspend fun getSubscribedConnections(projectId: UUID): List<WebSocketConnection> {
        return connectionMutex.withLock {
            connections.values.filter { projectId in it.subscribedProjects }
        }
    }

    /**
     * Safely sends a message to a connection, handling any errors.
     *
     * @param connection The target connection
     * @param messageJson The JSON message to send
     */
    private suspend fun sendMessageSafely(connection: WebSocketConnection, messageJson: String) {
        try {
            connection.session.send(Frame.Text(messageJson))
        } catch (e: Exception) {
            logger.warn("Failed to send WebSocket message to connection", e)
        }
    }
}
