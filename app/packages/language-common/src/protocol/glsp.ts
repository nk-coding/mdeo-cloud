import type {
    ActionMessage,
    DisposeClientSessionParameters,
    InitializeClientSessionParameters,
    InitializeParameters,
    InitializeResult
} from "@eclipse-glsp/protocol";
import type { PluginContext } from "../plugin/pluginContext.js";

/**
 * Creates a namespace containing JSON-RPC message types for GLSP client-server communication.
 * Defines the protocol methods and notifications used for GLSP interactions.
 *
 * @param vscodeJsonrpc The vscode-jsonrpc module from the plugin context
 * @returns A namespace object with GLSP protocol definitions
 */
export function createJsonrpcGLSPClient(vscodeJsonrpc: PluginContext["vscode-jsonrpc"]) {
    const { NotificationType, RequestType } = vscodeJsonrpc;

    return {
        /**
         * Notification type for action messages sent from the server to the client.
         * Used to process diagram actions and updates.
         */
        ActionMessageNotification: new NotificationType<ActionMessage>("glsp/process"),

        /**
         * Request type for initializing the GLSP server.
         * Returns server capabilities and configuration.
         */
        InitializeRequest: new RequestType<InitializeParameters, InitializeResult, void>("glsp/initialize"),

        /**
         * Request type for initializing a client session.
         * Sets up a new diagram editing session for a specific client.
         */
        InitializeClientSessionRequest: new RequestType<InitializeClientSessionParameters, void, void>(
            "glsp/initializeClientSession"
        ),

        /**
         * Request type for disposing a client session.
         * Cleans up resources associated with a diagram editing session.
         */
        DisposeClientSessionRequest: new RequestType<DisposeClientSessionParameters, void, void>(
            "glsp/disposeClientSession"
        )
    };
}
