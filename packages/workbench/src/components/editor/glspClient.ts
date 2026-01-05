import type { MonacoLanguageClient } from "monaco-languageclient";
import type { Event, Disposable } from "@eclipse-glsp/protocol";
import {
    type GLSPClient,
    type InitializeParameters,
    type InitializeResult,
    type InitializeClientSessionParameters,
    type DisposeClientSessionParameters,
    type ActionMessage,
    type ActionMessageHandler,
    ClientState,
    Emitter
} from "@eclipse-glsp/protocol";
import type { TextDocumentIdentifier } from "vscode-languageserver-types";
import { JsonrpcGLSPClient } from "@mdeo/language-common";

/**
 * Options for creating a MonacoGLSPClient
 */
export interface MonacoGLSPClientOptions {
    /**
     * The Monaco Language Client to use for communication
     */
    client: MonacoLanguageClient;

    /**
     * The text document URI
     */
    uri: string;

    /**
     * Optional client ID
     */
    id: string;
}

/**
 * A GLSP client implementation that uses MonacoLanguageClient for communication.
 * This client assumes the connection is already initialized and does not handle
 * server lifecycle management (start/stop/dispose).
 *
 * Based on BaseJsonrpcGLSPClient from @eclipse-glsp/sprotty but adapted for workbench usage.
 */
export class MonacoGLSPClient implements GLSPClient {
    /**
     * The unique identifier for this GLSP client.
     */
    readonly id: string;
    
    /**
     * The Monaco Language Client used for JSON-RPC communication.
     */
    protected readonly client: MonacoLanguageClient;
    
    /**
     * The text document identifier associated with this client.
     */
    protected readonly textDocument: TextDocumentIdentifier;
    
    /**
     * Pending server initialization promise to avoid duplicate initialization.
     */
    protected pendingServerInitialize?: Promise<InitializeResult>;

    /**
     * Emitter for server initialization events.
     */
    protected onServerInitializedEmitter = new Emitter<InitializeResult>();
    
    /**
     * Event fired when the server has been initialized.
     */
    get onServerInitialized(): Event<InitializeResult> {
        return this.onServerInitializedEmitter.event;
    }

    /**
     * Emitter for action message notifications from the server.
     */
    protected onActionMessageNotificationEmitter = new Emitter<ActionMessage>();
    
    /**
     * Event fired when an action message is received from the server.
     */
    protected get onActionMessageNotification(): Event<ActionMessage> {
        return this.onActionMessageNotificationEmitter.event;
    }

    /**
     * Emitter for client state changes.
     */
    protected onCurrentStateChangedEmitter = new Emitter<ClientState>();
    
    /**
     * Event fired when the client state changes.
     */
    get onCurrentStateChanged(): Event<ClientState> {
        return this.onCurrentStateChangedEmitter.event;
    }

    /**
     * Internal client state tracking.
     */
    protected _state: ClientState = ClientState.Initial;
    
    /**
     * Sets the client state and fires state change event if changed.
     */
    protected set state(state: ClientState) {
        if (this._state !== state) {
            this._state = state;
            this.onCurrentStateChangedEmitter.fire(state);
        }
    }
    
    /**
     * Gets the current client state.
     */
    protected get state(): ClientState {
        return this._state;
    }

    /**
     * Cached server initialization result.
     */
    protected _initializeResult?: InitializeResult;
    
    /**
     * Gets the server initialization result if available.
     */
    get initializeResult(): InitializeResult | undefined {
        return this._initializeResult;
    }

    /**
     * Creates a new MonacoGLSPClient instance.
     *
     * @param options Configuration options for the GLSP client
     */
    constructor(options: MonacoGLSPClientOptions) {
        this.client = options.client;
        this.textDocument = { uri: options.uri };
        this.id = options.id;
        this._state = ClientState.Initial;

        this.setupNotificationHandlers();
    }

    /**
     * Sets up handlers for notifications from the GLSP server.
     * Registers listeners for action messages sent by the server.
     */
    protected setupNotificationHandlers(): void {
        this.client.onNotification(
            JsonrpcGLSPClient.ActionMessageNotification.method,
            (msg: ActionMessage & { textDocument: TextDocumentIdentifier }) => {
                this.onActionMessageNotificationEmitter.fire(msg);
            }
        );
    }

    /**
     * Starts the GLSP client connection.
     * This implementation assumes the connection is already active.
     */
    async start(): Promise<void> {
        if (this.state === ClientState.Running || this.state === ClientState.StartFailed) {
            return;
        } else if (this.state === ClientState.Starting) {
            return new Promise<void>((resolve) => {
                const disposable = this.onCurrentStateChanged((state) => {
                    if (state === ClientState.Running || state === ClientState.StartFailed) {
                        disposable.dispose();
                        resolve();
                    }
                });
            });
        }

        this.state = ClientState.Starting;
        this.state = ClientState.Running;
    }

    /**
     * Initializes the GLSP server with the given parameters.
     * Caches and returns the initialization result for subsequent calls.
     *
     * @param params Parameters for server initialization
     * @returns The server initialization result
     * @throws {Error} If initialization fails
     */
    async initializeServer(params: InitializeParameters): Promise<InitializeResult> {
        if (this.initializeResult) {
            return this.initializeResult;
        } else if (this.pendingServerInitialize) {
            return this.pendingServerInitialize;
        }

        try {
            this.pendingServerInitialize = this.client.sendRequest(JsonrpcGLSPClient.InitializeRequest, params);
            this._initializeResult = await this.pendingServerInitialize;
            this.onServerInitializedEmitter.fire(this._initializeResult);
            this.pendingServerInitialize = undefined;
        } catch (error) {
            this._initializeResult = undefined;
            this.pendingServerInitialize = undefined;
            throw error;
        }

        return this._initializeResult;
    }

    /**
     * Initializes a client session with the GLSP server.
     *
     * @param params Parameters for client session initialization
     * @returns Promise that resolves when session is initialized
     */
    initializeClientSession(params: InitializeClientSessionParameters): Promise<void> {
        return this.client.sendRequest(JsonrpcGLSPClient.InitializeClientSessionRequest, params);
    }

    /**
     * Disposes a client session on the GLSP server.
     *
     * @param params Parameters for client session disposal
     * @returns Promise that resolves when session is disposed
     */
    disposeClientSession(params: DisposeClientSessionParameters): Promise<void> {
        return this.client.sendRequest(JsonrpcGLSPClient.DisposeClientSessionRequest, params);
    }

    /**
     * Registers a handler for action messages from the server.
     *
     * @param handler The handler function to invoke for action messages
     * @param clientId Optional client ID to filter messages
     * @returns A disposable to unregister the handler
     */
    onActionMessage(handler: ActionMessageHandler, clientId?: string): Disposable {
        return this.onActionMessageNotification((msg) => {
            if (!clientId || msg.clientId === clientId) {
                handler(msg);
            }
        });
    }

    /**
     * Sends an action message to the GLSP server.
     *
     * @param message The action message to send
     */
    sendActionMessage(message: ActionMessage): void {
        this.client.sendNotification(JsonrpcGLSPClient.ActionMessageNotification, message);
    }

    /**
     * Shuts down the GLSP server.
     * This is a no-op as server lifecycle is not managed by this client.
     */
    shutdownServer(): void {
        // No-op: Server lifecycle is not managed by this client
    }

    /**
     * Stops the GLSP client connection.
     */
    async stop(): Promise<void> {
        this.state = ClientState.Stopped;
    }

    /**
     * Checks if the connection to the server is currently active.
     *
     * @returns True if the connection is active, false otherwise
     */
    isConnectionActive(): boolean {
        return this.state === ClientState.Running;
    }

    /**
     * Gets the current state of the GLSP client.
     */
    get currentState(): ClientState {
        return this.state;
    }
}
