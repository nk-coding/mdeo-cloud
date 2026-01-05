import type { MessageConnection } from "vscode-jsonrpc";
import type { LangiumSharedGLSPServices } from "./glspModule.js";

/**
 * Configures and starts the GLSP server with the given Langium services.
 * Initializes the GLSP launcher using the LSP connection from the services.
 *
 * @param services The Langium shared services with GLSP integration
 */
export function configureGLSPServer(services: LangiumSharedGLSPServices): void {
    const launcher = services.glsp.launcher;
    const connection = services.lsp.Connection;
    if (connection == undefined) {
        throw new Error("No connection available to start GLSP server launcher.");
    }
    launcher.start({
        connection: connection as unknown as MessageConnection
    });
}
