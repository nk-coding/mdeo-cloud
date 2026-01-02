import type { MonacoLanguageClient } from "monaco-languageclient";
import {
    type GLSPClient,
    type InitializeParameters,
    type InitializeResult,
    type InitializeClientSessionParameters,
    type DisposeClientSessionParameters,
    type ActionMessage,
    type ActionMessageHandler,
    ClientState,
    Emitter,
    Event,
    Disposable
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
    readonly id: string;
    protected readonly client: MonacoLanguageClient;
    protected readonly textDocument: TextDocumentIdentifier;
    protected pendingServerInitialize?: Promise<InitializeResult>;

    protected onServerInitializedEmitter = new Emitter<InitializeResult>();
    get onServerInitialized(): Event<InitializeResult> {
        return this.onServerInitializedEmitter.event;
    }

    protected onActionMessageNotificationEmitter = new Emitter<ActionMessage>();
    protected get onActionMessageNotification(): Event<ActionMessage> {
        return this.onActionMessageNotificationEmitter.event;
    }

    protected onCurrentStateChangedEmitter = new Emitter<ClientState>();
    get onCurrentStateChanged(): Event<ClientState> {
        return this.onCurrentStateChangedEmitter.event;
    }

    protected _state: ClientState = ClientState.Initial;
    protected set state(state: ClientState) {
        if (this._state !== state) {
            this._state = state;
            this.onCurrentStateChangedEmitter.fire(state);
        }
    }
    protected get state(): ClientState {
        return this._state;
    }

    protected _initializeResult?: InitializeResult;
    get initializeResult(): InitializeResult | undefined {
        return this._initializeResult;
    }

    constructor(options: MonacoGLSPClientOptions) {
        this.client = options.client;
        this.textDocument = { uri: options.uri };
        this.id = options.id;
        this._state = ClientState.Initial;
        
        // Register for action message notifications
        this.setupNotificationHandlers();
    }

    protected setupNotificationHandlers(): void {
        // Listen for action message notifications from the server
        this.client.onNotification(JsonrpcGLSPClient.ActionMessageNotification.method, (msg: ActionMessage & { textDocument: TextDocumentIdentifier }) => {
            this.onActionMessageNotificationEmitter.fire(msg);
        });
    }

    async start(): Promise<void> {
        if (this.state === ClientState.Running || this.state === ClientState.StartFailed) {
            return;
        } else if (this.state === ClientState.Starting) {
            // Wait until state changes
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
        // Client is assumed to be already started
        this.state = ClientState.Running;
    }

    async initializeServer(params: InitializeParameters): Promise<InitializeResult> {
        console.log(params)
        if (this.initializeResult) {
            return this.initializeResult;
        } else if (this.pendingServerInitialize) {
            return this.pendingServerInitialize;
        }

        try {
            this.pendingServerInitialize = this.client.sendRequest(
                JsonrpcGLSPClient.InitializeRequest,
                { ...params, textDocument: this.textDocument }
            );
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

    initializeClientSession(params: InitializeClientSessionParameters): Promise<void> {
        return this.client.sendRequest(
            JsonrpcGLSPClient.InitializeClientSessionRequest,
            { ...params, textDocument: this.textDocument }
        );
    }

    disposeClientSession(params: DisposeClientSessionParameters): Promise<void> {
        return this.client.sendRequest(
            JsonrpcGLSPClient.DisposeClientSessionRequest,
            { ...params, textDocument: this.textDocument }
        );
    }

    onActionMessage(handler: ActionMessageHandler, clientId?: string): Disposable {
        return this.onActionMessageNotification(msg => {
            if (!clientId || msg.clientId === clientId) {
                handler(msg);
            }
        });
    }

    sendActionMessage(message: ActionMessage): void {
        this.client.sendNotification(
            JsonrpcGLSPClient.ActionMessageNotification,
            { ...message, textDocument: this.textDocument }
        );
    }

    shutdownServer(): void {
        // No-op: Server lifecycle is not managed by this client
    }

    async stop(): Promise<void> {
        this.state = ClientState.Stopped;
    }

    isConnectionActive(): boolean {
        return this.state === ClientState.Running;
    }

    get currentState(): ClientState {
        return this.state;
    }
}
