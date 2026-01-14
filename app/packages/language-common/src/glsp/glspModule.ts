import type { ActionMessage, JsonRpcServerInstance, MaybePromise, ServerModule } from "@eclipse-glsp/server";
import type { PluginContext } from "../plugin/pluginContext.js";
import type { Container, ContainerModule } from "inversify";
import type { CustomGLSPServerLauncher, CustomGLSPServerLauncherOptions } from "./customLauncher.js";
import type { LangiumSharedServices } from "langium/lsp";
import type { Module } from "langium";
import { createJsonrpcGLSPClient } from "../protocol/glsp.js";
import type { MessageConnection } from "vscode-jsonrpc";

/**
 * Additional services provided by GLSP integration in Langium shared services.
 */
export interface GLSPSharedAdditionalServices {
    glsp: {
        /**
         * The inversify container for dependency injection.
         */
        container: Container;
        /**
         * The GLSP server module configuration.
         */
        serverModule: ServerModule;
        /**
         * The custom GLSP server launcher.
         */
        launcher: CustomGLSPServerLauncher;
    };
}

/**
 * Combined type of Langium shared services with GLSP additions.
 */
export type LangiumSharedGLSPServices = GLSPSharedAdditionalServices & LangiumSharedServices;

/**
 * Creates a Langium module that provides GLSP integration services.
 *
 * @param context The plugin context providing necessary dependencies
 * @returns A Langium module with GLSP service providers
 */
export function createGLSPModule(
    context: PluginContext
): Module<LangiumSharedGLSPServices, GLSPSharedAdditionalServices> {
    return {
        glsp: {
            container: (services) => createContainer(services, context),
            serverModule: (services) => createServerModule(services, context),
            launcher: (services) => createLauncher(services, context)
        }
    };
}

/**
 * Creates the inversify container for GLSP dependency injection.
 * Initializes the container with the GLSP application module.
 *
 * @param _services The Langium shared GLSP services (unused)
 * @param context The plugin context providing inversify and GLSP server dependencies
 * @returns The configured inversify container
 */
function createContainer(
    _services: LangiumSharedGLSPServices,
    { inversify, "@eclipse-glsp/server/browser.js": glspServerBrowser }: PluginContext
): Container {
    const { Container } = inversify;
    const { createAppModule } = glspServerBrowser;

    const container = new Container();
    const appModule = createAppModule();
    container.load(appModule);
    return container;
}

/**
 * Creates the GLSP server module for diagram configuration.
 * This module is used to register diagram-specific services and handlers.
 *
 * @param _services The Langium shared GLSP services (unused)
 * @param context The plugin context providing GLSP server dependencies
 * @returns A new server module instance
 */
function createServerModule(
    _services: LangiumSharedGLSPServices,
    { "@eclipse-glsp/server": glspServer }: PluginContext
): ServerModule {
    const { ServerModule } = glspServer;

    const serverModule = new ServerModule();
    return serverModule;
}

/**
 * Creates and configures the custom GLSP server launcher.
 * The launcher handles starting the GLSP server and configuring client-server communication.
 *
 * @param services The Langium shared GLSP services providing container and server module
 * @param context The plugin context providing GLSP server and inversify dependencies
 * @returns A configured custom GLSP server launcher
 */
function createLauncher(services: LangiumSharedGLSPServices, context: PluginContext): CustomGLSPServerLauncher {
    const { "@eclipse-glsp/server": glspServer, inversify } = context;
    const { JsonRpcGLSPServerLauncher, GLSPClientProxy, JsonrpcClientProxy } = glspServer;
    const { injectable, ContainerModule } = inversify;
    const { container, serverModule } = services.glsp;

    // Create the JsonrpcGLSPClient with the context
    const JsonrpcGLSPClient = createJsonrpcGLSPClient(context["vscode-jsonrpc"]);

    /**
     * Custom JSON-RPC client proxy to handle action messages.
     */
    class CustomJsonRPCClientProxy extends JsonrpcClientProxy {
        override process(message: ActionMessage): void {
            this.clientConnection?.sendNotification(JsonrpcGLSPClient.ActionMessageNotification, message);
        }
    }

    /**
     * Launcher implementation for GLSP server.
     * Extends the base JSON-RPC launcher with custom connection configuration.
     */
    @injectable()
    class CustomLauncher
        extends JsonRpcGLSPServerLauncher<CustomGLSPServerLauncherOptions>
        implements CustomGLSPServerLauncher
    {
        /**
         * Runs the GLSP server with the provided connection options.
         *
         * @param startParams The connection and configuration parameters
         */
        protected override run(startParams: CustomGLSPServerLauncherOptions): MaybePromise<void> {
            const connection = startParams.connection;
            this.createServerInstance(connection);
        }

        /**
         * Configures the client connection by registering request and notification handlers.
         * Sets up the JSON-RPC protocol handlers for GLSP communication.
         *
         * @param serverInstance The server instance containing the connection and server
         */
        protected override configureClientConnection(serverInstance: JsonRpcServerInstance): void {
            const clientConnection = serverInstance.clientConnection;
            const server = serverInstance.server;

            clientConnection.onRequest(JsonrpcGLSPClient.InitializeRequest.method, (params) =>
                server.initialize(params)
            );
            clientConnection.onRequest(JsonrpcGLSPClient.InitializeClientSessionRequest, (params) =>
                server.initializeClientSession(params)
            );
            clientConnection.onRequest(JsonrpcGLSPClient.DisposeClientSessionRequest, (params) =>
                server.disposeClientSession(params)
            );
            clientConnection.onNotification(JsonrpcGLSPClient.ActionMessageNotification, (message) => {
                server.process(message);
            });
        }

        protected override createJsonRpcModule(clientConnection: MessageConnection): ContainerModule {
            return new ContainerModule((bind) => {
                bind(GLSPClientProxy).toDynamicValue((ctx) => {
                    const proxy = ctx.container.resolve(CustomJsonRPCClientProxy);
                    proxy.initialize(clientConnection);
                    return proxy;
                });
            });
        }
    }

    const launcher = container.resolve(CustomLauncher);
    launcher.configure(serverModule);
    return launcher;
}
