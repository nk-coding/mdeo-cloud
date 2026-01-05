import type {
    ActionMessage,
    DisposeClientSessionParameters,
    InitializeClientSessionParameters,
    InitializeParameters,
    InitializeResult
} from "@eclipse-glsp/protocol";
import { NotificationType, RequestType } from "vscode-jsonrpc";

/**
 * Namespace containing JSON-RPC message types for GLSP client-server communication.
 * Defines the protocol methods and notifications used for GLSP interactions.
 */
export namespace JsonrpcGLSPClient {
    /**
     * Notification type for action messages sent from the server to the client.
     * Used to process diagram actions and updates.
     */
    export const ActionMessageNotification = new NotificationType<ActionMessage>("glsp/process");
    
    /**
     * Request type for initializing the GLSP server.
     * Returns server capabilities and configuration.
     */
    export const InitializeRequest = new RequestType<InitializeParameters, InitializeResult, void>("glsp/initialize");
    
    /**
     * Request type for initializing a client session.
     * Sets up a new diagram editing session for a specific client.
     */
    export const InitializeClientSessionRequest = new RequestType<InitializeClientSessionParameters, void, void>(
        "glsp/initializeClientSession"
    );
    
    /**
     * Request type for disposing a client session.
     * Cleans up resources associated with a diagram editing session.
     */
    export const DisposeClientSessionRequest = new RequestType<DisposeClientSessionParameters, void, void>(
        "glsp/disposeClientSession"
    );
}
