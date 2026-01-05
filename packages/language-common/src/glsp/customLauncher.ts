import type { JsonRpcGLSPServerLauncher } from "@eclipse-glsp/server";
import type { MessageConnection } from "vscode-jsonrpc";

/**
 * Options for launching a custom GLSP server.
 */
export interface CustomGLSPServerLauncherOptions {
    /**
     * The JSON-RPC message connection to use for server communication.
     */
    connection: MessageConnection;
}

/**
 * Type alias for a GLSP server launcher configured with custom options.
 */
export type CustomGLSPServerLauncher = JsonRpcGLSPServerLauncher<CustomGLSPServerLauncherOptions>;
