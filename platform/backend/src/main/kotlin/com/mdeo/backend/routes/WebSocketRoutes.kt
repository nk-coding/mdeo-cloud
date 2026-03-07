package com.mdeo.backend.routes

import com.mdeo.backend.plugins.getUserSession
import com.mdeo.backend.plugins.isAdmin
import com.mdeo.backend.service.ProjectPermission
import com.mdeo.backend.service.ProjectService
import com.mdeo.backend.service.SubscribeMessage
import com.mdeo.backend.service.UnsubscribeMessage
import com.mdeo.backend.service.WebSocketNotificationService
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("WebSocketRoutes")

/**
 * Configures WebSocket routes for real-time notifications.
 * Requires session authentication to establish a connection.
 *
 * @param webSocketService Service for managing WebSocket connections
 * @param projectService Service for validating project access
 */
fun Route.webSocketRoutes(
    webSocketService: WebSocketNotificationService,
    projectService: ProjectService
) {
    webSocket("/api/ws") {
        val session = call.getUserSession()
        if (session == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Not authenticated"))
            return@webSocket
        }
        
        val connectionId = UUID.randomUUID().toString()
        val handler = WebSocketMessageHandler(
            connectionId = connectionId,
            userId = session.userId,
            isGlobalAdmin = call.isAdmin(),
            webSocketService = webSocketService,
            projectService = projectService,
            session = this
        )
        
        handler.handleConnection()
    }
}

/**
 * Handler for processing WebSocket messages for a single connection.
 * Manages the connection lifecycle and message processing.
 *
 * @property connectionId Unique identifier for this connection
 * @property userId The authenticated user's ID
 * @property isGlobalAdmin Whether the user has global admin permission
 * @property webSocketService Service for managing subscriptions
 * @property projectService Service for validating project access
 * @property session The WebSocket session
 */
private class WebSocketMessageHandler(
    private val connectionId: String,
    private val userId: String,
    private val isGlobalAdmin: Boolean,
    private val webSocketService: WebSocketNotificationService,
    private val projectService: ProjectService,
    private val session: DefaultWebSocketServerSession
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Main connection handler loop.
     * Registers the connection, processes incoming messages, and cleans up on disconnect.
     */
    suspend fun handleConnection() {
        webSocketService.registerConnection(connectionId, session, userId)
        
        try {
            processIncomingMessages()
        } finally {
            webSocketService.removeConnection(connectionId)
        }
    }
    
    /**
     * Processes all incoming WebSocket messages until the connection closes.
     */
    private suspend fun processIncomingMessages() {
        for (frame in session.incoming) {
            when (frame) {
                is Frame.Text -> handleTextFrame(frame)
                is Frame.Close -> return
                else -> {}
            }
        }
    }
    
    /**
     * Handles a text frame by parsing and processing the message.
     *
     * @param frame The text frame to process
     */
    private suspend fun handleTextFrame(frame: Frame.Text) {
        try {
            val text = frame.readText()
            processMessage(text)
        } catch (e: Exception) {
            logger.warn("Failed to process WebSocket message", e)
        }
    }
    
    /**
     * Parses and routes a message to the appropriate handler based on type.
     *
     * @param text The raw JSON message text
     */
    private suspend fun processMessage(text: String) {
        val jsonElement = json.parseToJsonElement(text)
        val type = jsonElement.jsonObject["messageType"]?.jsonPrimitive?.content
        
        when (type) {
            "subscribe" -> handleSubscribe(jsonElement)
            "unsubscribe" -> handleUnsubscribe(jsonElement)
            else -> logger.warn("Unknown message type: $type")
        }
    }
    
    /**
     * Handles a subscription request after validating project access.
     *
     * @param jsonElement The parsed subscribe message
     */
    private suspend fun handleSubscribe(jsonElement: JsonElement) {
        val message = json.decodeFromJsonElement<SubscribeMessage>(jsonElement)
        val projectId = parseProjectId(message.projectId) ?: return
        
        if (!validateProjectAccess(projectId)) {
            logger.warn("User $userId attempted to subscribe to unauthorized project $projectId")
            return
        }
        
        webSocketService.subscribe(connectionId, projectId)
    }
    
    /**
     * Handles an unsubscription request.
     *
     * @param jsonElement The parsed unsubscribe message
     */
    private suspend fun handleUnsubscribe(jsonElement: JsonElement) {
        val message = json.decodeFromJsonElement<UnsubscribeMessage>(jsonElement)
        val projectId = parseProjectId(message.projectId) ?: return
        
        webSocketService.unsubscribe(connectionId, projectId)
    }
    
    /**
     * Parses a project ID string to UUID.
     *
     * @param projectIdString The project ID as a string
     * @return The parsed UUID or null if invalid
     */
    private fun parseProjectId(projectIdString: String): UUID? {
        return try {
            UUID.fromString(projectIdString)
        } catch (e: Exception) {
            logger.warn("Invalid project ID: $projectIdString")
            null
        }
    }
    
    /**
     * Validates that the user has access to the specified project.
     *
     * @param projectId The project UUID to check
     * @return True if the user has access, false otherwise
     */
    private fun validateProjectAccess(projectId: UUID): Boolean {
        val userUuid = try {
            UUID.fromString(userId)
        } catch (e: Exception) {
            return false
        }
        return projectService.hasProjectPermission(projectId, userUuid, isGlobalAdmin, ProjectPermission.READ)
    }
}
