import type { Execution } from "../../execution/execution";
import { showSuccess, showError, showInfo } from "@/lib/notifications";

/**
 * Base interface for all WebSocket messages
 */
interface WebSocketMessage {
    messageType: string;
}

/**
 * Message to subscribe to a project's events
 */
interface SubscribeMessage extends WebSocketMessage {
    messageType: "subscribe";
    projectId: string;
}

/**
 * Message to unsubscribe from a project's events
 */
interface UnsubscribeMessage extends WebSocketMessage {
    messageType: "unsubscribe";
    projectId: string;
}

/**
 * Notification when an execution state changes
 */
interface ExecutionStateChangedMessage extends WebSocketMessage {
    messageType: "executionStateChanged";
    execution: Execution;
}

/**
 * Connection state for the WebSocket
 */
export type ConnectionState = "disconnected" | "connecting" | "connected";

/**
 * Callback function for execution state changes
 */
export type ExecutionStateChangeCallback = (execution: Execution) => void;

/**
 * Configuration options for WebSocketApi
 */
export interface WebSocketApiOptions {
    /**
     * Base URL for WebSocket connection (e.g., "/api")
     */
    baseUrl: string;
    /**
     * Delay in milliseconds before attempting reconnect (default: 3000)
     */
    reconnectDelay?: number;
    /**
     * Maximum reconnect delay in milliseconds (default: 30000)
     */
    maxReconnectDelay?: number;
    /**
     * Whether to show toast notifications for state changes (default: true)
     */
    showNotifications?: boolean;
}

/**
 * API for managing WebSocket connections for real-time notifications.
 * Handles connection establishment, automatic reconnection, and project subscriptions.
 */
export class WebSocketApi {
    private socket: WebSocket | null = null;
    private connectionState: ConnectionState = "disconnected";
    private currentProjectId: string | null = null;
    private reconnectAttempts = 0;
    private reconnectTimeoutId: ReturnType<typeof setTimeout> | null = null;
    private readonly stateChangeCallbacks: Set<ExecutionStateChangeCallback> = new Set();
    private readonly reconnectDelay: number;
    private readonly maxReconnectDelay: number;
    private readonly showNotifications: boolean;
    private readonly wsUrl: string;
    private readonly executionStates: Map<string, string> = new Map();

    /**
     * Creates a new WebSocketApi instance
     *
     * @param options Configuration options for the WebSocket connection
     */
    constructor(options: WebSocketApiOptions) {
        this.reconnectDelay = options.reconnectDelay ?? 3000;
        this.maxReconnectDelay = options.maxReconnectDelay ?? 30000;
        this.showNotifications = options.showNotifications ?? true;
        this.wsUrl = this.buildWebSocketUrl(options.baseUrl);
    }

    /**
     * Builds the WebSocket URL from the base URL
     *
     * @param baseUrl The base API URL
     * @returns The full WebSocket URL
     */
    private buildWebSocketUrl(baseUrl: string): string {
        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        const host = window.location.host;
        const wsPath = baseUrl.replace(/^\//, "") + "/ws";
        return `${protocol}//${host}/${wsPath}`;
    }

    /**
     * Gets the current connection state
     *
     * @returns The current connection state
     */
    getConnectionState(): ConnectionState {
        return this.connectionState;
    }

    /**
     * Registers a callback to be called when execution state changes
     *
     * @param callback The callback function to register
     */
    onExecutionStateChange(callback: ExecutionStateChangeCallback): void {
        this.stateChangeCallbacks.add(callback);
    }

    /**
     * Unregisters a previously registered callback
     *
     * @param callback The callback function to unregister
     */
    offExecutionStateChange(callback: ExecutionStateChangeCallback): void {
        this.stateChangeCallbacks.delete(callback);
    }

    /**
     * Connects to the WebSocket server and optionally subscribes to a project
     *
     * @param projectId Optional project ID to subscribe to after connecting
     */
    connect(projectId?: string): void {
        if (this.connectionState === "connecting" || this.connectionState === "connected") {
            if (projectId && projectId !== this.currentProjectId) {
                this.subscribeToProject(projectId);
            }
            return;
        }

        this.connectionState = "connecting";
        this.currentProjectId = projectId ?? null;

        this.socket = new WebSocket(this.wsUrl);
        this.setupSocketEventHandlers();
    }

    /**
     * Sets up event handlers for the WebSocket connection
     */
    private setupSocketEventHandlers(): void {
        if (!this.socket) return;

        this.socket.onopen = () => this.handleOpen();
        this.socket.onmessage = (event) => this.handleMessage(event);
        this.socket.onclose = (event) => this.handleClose(event);
        this.socket.onerror = (event) => this.handleError(event);
    }

    /**
     * Handles successful WebSocket connection
     */
    private handleOpen(): void {
        this.connectionState = "connected";
        this.reconnectAttempts = 0;

        if (this.currentProjectId) {
            this.subscribeToProject(this.currentProjectId);
        }
    }

    /**
     * Handles incoming WebSocket messages
     *
     * @param event The message event
     */
    private handleMessage(event: MessageEvent): void {
        const message = JSON.parse(event.data) as WebSocketMessage;
        this.routeMessage(message);
    }

    /**
     * Routes a message to the appropriate handler based on type
     *
     * @param message The parsed WebSocket message
     */
    private routeMessage(message: WebSocketMessage): void {
        switch (message.messageType) {
            case "executionStateChanged":
                this.handleExecutionStateChanged(message as ExecutionStateChangedMessage);
                break;
            default:
                throw new Error(`Unknown WebSocket message type: ${message.messageType}`);
        }
    }

    /**
     * Handles execution state change notifications
     *
     * @param message The execution state changed message
     */
    private handleExecutionStateChanged(message: ExecutionStateChangedMessage): void {
        const execution = message.execution;
        const previousState = this.executionStates.get(execution.id);
        const stateChanged = previousState !== execution.state;

        this.executionStates.set(execution.id, execution.state);

        this.notifyCallbacks(execution);

        if (stateChanged) {
            this.showStateChangeNotification(execution);
        }
    }

    /**
     * Notifies all registered callbacks of an execution state change
     *
     * @param execution The updated execution
     */
    private notifyCallbacks(execution: Execution): void {
        for (const callback of this.stateChangeCallbacks) {
            callback(execution);
        }
    }

    /**
     * Shows a toast notification for an execution state change
     *
     * @param execution The updated execution
     */
    private showStateChangeNotification(execution: Execution): void {
        if (!this.showNotifications) return;

        const message = this.buildNotificationMessage(execution);
        if (!message) return;

        this.displayNotification(execution.state, message, execution.name);
    }

    /**
     * Builds the notification message based on execution state
     *
     * @param execution The execution
     * @returns The notification message or null if no notification should be shown
     */
    private buildNotificationMessage(execution: Execution): string | null {
        switch (execution.state) {
            case "completed":
                return "Execution completed successfully";
            case "failed":
                return "Execution failed";
            case "cancelled":
                return "Execution was cancelled";
            case "running":
                return "Execution started running";
            default:
                return null;
        }
    }

    /**
     * Displays a notification based on execution state
     *
     * @param state The execution state
     * @param message The notification message
     * @param executionName The name of the execution
     */
    private displayNotification(state: string, message: string, executionName: string): void {
        const description = executionName;

        switch (state) {
            case "completed":
                showSuccess(message, { description });
                break;
            case "failed":
                showError(message, { description });
                break;
            case "cancelled":
            case "running":
                showInfo(message, { description });
                break;
        }
    }

    /**
     * Handles WebSocket connection close
     *
     * @param event The close event
     */
    private handleClose(event: CloseEvent): void {
        this.connectionState = "disconnected";
        this.socket = null;

        if (!event.wasClean && this.currentProjectId) {
            this.scheduleReconnect();
        }
    }

    /**
     * Handles WebSocket errors
     *
     * @param _event The error event
     */
    private handleError(_event: Event): void {
        throw new Error("WebSocket error occurred");
    }

    /**
     * Schedules a reconnection attempt with exponential backoff
     */
    private scheduleReconnect(): void {
        if (this.reconnectTimeoutId) {
            clearTimeout(this.reconnectTimeoutId);
        }

        const delay = this.calculateReconnectDelay();
        this.reconnectAttempts++;

        this.reconnectTimeoutId = setTimeout(() => {
            this.reconnectTimeoutId = null;
            this.connect(this.currentProjectId ?? undefined);
        }, delay);
    }

    /**
     * Calculates the reconnect delay using exponential backoff
     *
     * @returns The delay in milliseconds
     */
    private calculateReconnectDelay(): number {
        const exponentialDelay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts);
        return Math.min(exponentialDelay, this.maxReconnectDelay);
    }

    /**
     * Subscribes to a project's events
     *
     * @param projectId The project ID to subscribe to
     */
    subscribeToProject(projectId: string): void {
        if (this.currentProjectId && this.currentProjectId !== projectId) {
            this.unsubscribeFromProject(this.currentProjectId);
        }

        this.currentProjectId = projectId;

        if (this.connectionState !== "connected") {
            this.connect(projectId);
            return;
        }

        this.sendSubscribeMessage(projectId);
    }

    /**
     * Sends a subscribe message to the server
     *
     * @param projectId The project ID to subscribe to
     */
    private sendSubscribeMessage(projectId: string): void {
        const message: SubscribeMessage = {
            messageType: "subscribe",
            projectId
        };
        this.sendMessage(message);
    }

    /**
     * Unsubscribes from a project's events
     *
     * @param projectId The project ID to unsubscribe from
     */
    unsubscribeFromProject(projectId: string): void {
        if (this.connectionState !== "connected") return;

        const message: UnsubscribeMessage = {
            messageType: "unsubscribe",
            projectId
        };
        this.sendMessage(message);

        if (this.currentProjectId === projectId) {
            this.currentProjectId = null;
        }
    }

    /**
     * Sends a message through the WebSocket connection
     *
     * @param message The message to send
     */
    private sendMessage(message: WebSocketMessage): void {
        if (!this.socket || this.connectionState !== "connected") {
            throw new Error("WebSocket is not connected");
        }

        this.socket.send(JSON.stringify(message));
    }

    /**
     * Disconnects the WebSocket connection
     */
    disconnect(): void {
        if (this.reconnectTimeoutId) {
            clearTimeout(this.reconnectTimeoutId);
            this.reconnectTimeoutId = null;
        }

        this.currentProjectId = null;

        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }

        this.connectionState = "disconnected";
    }
}
