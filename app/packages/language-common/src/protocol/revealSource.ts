import type { DocumentUri, Range } from "vscode-languageserver-protocol";
import type { PluginContext } from "../plugin/pluginContext.js";

/**
 * Parameters sent with the {@code textDocument/revealSource} LSP notification from the
 * language server to the workbench client.
 */
export interface RevealSourceParams {
    /**
     * The URI of the document containing the source range to reveal.
     */
    uri: DocumentUri;
    /**
     * The LSP source range that should be selected and scrolled into view in the
     * Monaco textual editor.
     */
    range: Range;
}

/**
 * Creates the LSP notification type used to tell the workbench to reveal a specific
 * source range in the Monaco editor.
 *
 * <p>The function accepts the {@code vscode-jsonrpc} module from the plugin context so
 * that the type is constructed with the same runtime instance that the language server
 * uses for all other notifications, preventing duplicate symbol conflicts.
 *
 * @param vscodeJsonrpc The {@code vscode-jsonrpc} module from the plugin context.
 * @returns A namespace object containing the {@code RevealSourceNotification} type.
 */
export function createRevealSourceProtocol(vscodeJsonrpc: PluginContext["vscode-jsonrpc"]) {
    const { NotificationType } = vscodeJsonrpc;

    return {
        /**
         * Notification type for the {@code textDocument/revealSource} LSP method.
         * Sent from the language server to the workbench when a graphical element's
         * source should be revealed in the textual editor.
         */
        RevealSourceNotification: new NotificationType<RevealSourceParams>("textDocument/revealSource")
    };
}
