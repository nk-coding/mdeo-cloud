import type { ActionStartParams } from "./action.js";
import type { PluginContext } from "../plugin/pluginContext.js";

/**
 * Creates a namespace containing JSON-RPC message types for triggering actions from the server.
 * Defines the notification type used to trigger action dialogs in the workbench client.
 *
 * @param vscodeJsonrpc The vscode-jsonrpc module from the plugin context
 * @returns A namespace object with trigger action protocol definitions
 */
export function createTriggerActionProtocol(vscodeJsonrpc: PluginContext["vscode-jsonrpc"]) {
    const { NotificationType } = vscodeJsonrpc;

    return {
        /**
         * Notification type for triggering an action dialog from the server.
         * When sent, the workbench will display the action dialog for the specified action.
         */
        TriggerActionNotification: new NotificationType<ActionStartParams>("action/trigger")
    };
}
