import type {
    CompletionItem,
    CompletionParams,
    Connection,
    DocumentFormattingParams,
    InitializeParams,
    InitializeResult,
    ServerCapabilities,
    TextDocumentChangeEvent,
    DidChangeWatchedFilesParams,
    WorkspaceFoldersChangeEvent
} from "vscode-languageserver";
import {
    createConnection,
    Range,
    TextDocuments,
    TextEdit,
    uinteger,
    FileChangeType,
    BrowserMessageReader,
    BrowserMessageWriter
} from "vscode-languageserver/browser";
import type { TextDocumentContentChangeEvent } from "vscode-languageserver-textdocument";
import { TextDocument } from "vscode-languageserver-textdocument";

/**
 * Simple Language Server
 * Monitors key workspace and document events for demonstration purposes
 */
class SimpleLanguageServer {
    private connection: Connection;
    private documents: TextDocuments<TextDocument>;
    private workspaceFolders: string[] = [];

    constructor() {
        // Create a connection for the server using browser message reader/writer
        const messageReader = new BrowserMessageReader(self);
        const messageWriter = new BrowserMessageWriter(self);
        this.connection = createConnection(messageReader, messageWriter);

        // Create a text document manager
        this.documents = new TextDocuments(TextDocument);

        this.setupEventHandlers();
        this.setupDocumentHandlers();
    }

    private setupEventHandlers(): void {
        // ===== INITIALIZATION EVENT =====
        this.connection.onInitialize((params: InitializeParams): InitializeResult => {
            console.log("[Server] Initialization started");
            console.log("[Server] Client info:", params.clientInfo);
            console.log("[Server] Root URI:", params.rootUri);
            console.log("[Server] Root Path:", params.rootPath);

            // Store workspace folders
            if (params.workspaceFolders) {
                this.workspaceFolders = params.workspaceFolders.map((folder) => folder.uri);
                console.log("[Server] Initial workspace folders:", this.workspaceFolders);
            }

            const capabilities: ServerCapabilities = {
                textDocumentSync: {
                    openClose: true,
                    change: 2 // Incremental sync
                },
                completionProvider: {
                    resolveProvider: true,
                    triggerCharacters: ["."]
                },
                documentFormattingProvider: true,
                workspace: {
                    workspaceFolders: {
                        supported: true,
                        changeNotifications: true
                    },
                    fileOperations: {
                        didCreate: {
                            filters: [{ pattern: { glob: "**/*" } }]
                        },
                        didDelete: {
                            filters: [{ pattern: { glob: "**/*" } }]
                        },
                        didRename: {
                            filters: [{ pattern: { glob: "**/*" } }]
                        }
                    }
                }
            };

            const result: InitializeResult = {
                capabilities,
                serverInfo: {
                    name: "Simple Language Server",
                    version: "1.0.0"
                }
            };

            console.log("[Server] Initialization complete");
            return result;
        });

        this.connection.onInitialized(() => {
            console.log("[Server] Server initialized and ready");

            // Register for workspace folder changes
            this.connection.workspace.onDidChangeWorkspaceFolders((event: WorkspaceFoldersChangeEvent) => {
                this.handleWorkspaceFoldersChanged(event);
            });

            // Register for file operation events (create, delete, rename)
            this.connection.workspace.onDidCreateFiles((params) => {
                console.log(
                    "[Server] Files created:",
                    params.files.map((f) => f.uri)
                );
            });

            this.connection.workspace.onDidDeleteFiles((params) => {
                console.log(
                    "[Server] Files deleted:",
                    params.files.map((f) => f.uri)
                );
            });

            this.connection.workspace.onDidRenameFiles((params) => {
                console.log("[Server] Files renamed:");
                params.files.forEach((f) => {
                    console.log(`  ${f.oldUri} -> ${f.newUri}`);
                });
            });
        });

        // ===== FILE SYSTEM CHANGES (Watched Files) =====
        this.connection.onDidChangeWatchedFiles((params: DidChangeWatchedFilesParams) => {
            this.handleFileSystemChanges(params);
        });

        // ===== SHUTDOWN EVENT =====
        this.connection.onShutdown(() => {
            console.log("[Server] Shutting down...");
        });

        // ===== COMPLETION =====
        this.connection.onCompletion((params: CompletionParams): CompletionItem[] => {
            const document = this.documents.get(params.textDocument.uri);
            if (!document) {
                console.log("[Server] ===== COMPLETION REQUEST (Document not found) =====");
                console.log("[Server] URI:", params.textDocument.uri);
                return [];
            }

            console.log("[Server] ===== COMPLETION REQUEST =====");
            console.log("[Server] Document URI:", params.textDocument.uri);
            console.log("[Server] Position:", params.position);
            console.log("[Server] Context:", params.context);

            // Get the text at the completion position
            const offset = document.offsetAt(params.position);
            const textBefore = document.getText(
                Range.create({ line: params.position.line, character: 0 }, params.position)
            );
            const textAfter = document.getText(
                Range.create(params.position, { line: params.position.line, character: uinteger.MAX_VALUE })
            );

            console.log("[Server] Line text before cursor:", JSON.stringify(textBefore));
            console.log("[Server] Line text after cursor:", JSON.stringify(textAfter));
            console.log("[Server] Offset:", offset);
            console.log("[Server] Document version:", document.version);
            console.log("[Server] Language ID:", document.languageId);

            // Return simple completion items as example
            const completionItems: CompletionItem[] = [
                {
                    label: "example",
                    kind: 1, // Text
                    detail: "Example completion item",
                    documentation: "This is an example completion item"
                },
                {
                    label: "function",
                    kind: 3, // Function
                    detail: "Example function",
                    documentation: "This is an example function completion"
                },
                {
                    label: "variable",
                    kind: 6, // Variable
                    detail: "Example variable",
                    documentation: "This is an example variable completion"
                }
            ];

            console.log("[Server] Returning", completionItems.length, "completion items");
            console.log("[Server] ===================================");

            return completionItems;
        });

        // ===== COMPLETION RESOLVE =====
        this.connection.onCompletionResolve((item: CompletionItem): CompletionItem => {
            console.log("[Server] ===== COMPLETION RESOLVE =====");
            console.log("[Server] Resolving item:", item.label);
            console.log("[Server] ==================================");

            // Add additional information to the completion item if needed
            return item;
        });

        // ===== FORMATTING =====
        this.connection.onDocumentFormatting((params: DocumentFormattingParams): TextEdit[] => {
            const document = this.documents.get(params.textDocument.uri);
            if (!document) {
                return [];
            }

            console.log("[Server] Formatting requested for:", params.textDocument.uri);

            // Return empty array (no formatting changes)
            return [];
        });
    }

    private setupDocumentHandlers(): void {
        // ===== DOCUMENT OPENED =====
        this.documents.onDidOpen((event: TextDocumentChangeEvent<TextDocument>) => {
            console.log("[Server] Document opened:", event.document.uri);
            console.log("  Language:", event.document.languageId);
            console.log("  Version:", event.document.version);
            console.log("  Line count:", event.document.lineCount);
        });

        // ===== DOCUMENT CONTENT CHANGED =====
        this.documents.onDidChangeContent((event: TextDocumentChangeEvent<TextDocument>) => {
            console.log("[Server] Document content changed:", event.document.uri);
            console.log("  New version:", event.document.version);
            console.log("  Line count:", event.document.lineCount);

            // Access the document text if needed
            const text = event.document.getText();
            console.log("  Total characters:", text.length);
        });

        // ===== DOCUMENT SAVED =====
        this.documents.onDidSave((event: TextDocumentChangeEvent<TextDocument>) => {
            console.log("[Server] Document saved:", event.document.uri);
            console.log("  Version:", event.document.version);
        });

        // ===== DOCUMENT CLOSED =====
        this.documents.onDidClose((event: TextDocumentChangeEvent<TextDocument>) => {
            console.log("[Server] Document closed:", event.document.uri);
        });

        // Make the text document manager listen on the connection
        this.documents.listen(this.connection);
    }

    // ===== WORKSPACE FOLDERS CHANGED EVENT =====
    private handleWorkspaceFoldersChanged(event: WorkspaceFoldersChangeEvent): void {
        console.log("[Server] ===== WORKSPACE FOLDERS CHANGED =====");

        // Handle added folders
        if (event.added.length > 0) {
            console.log("[Server] Added folders:");
            event.added.forEach((folder) => {
                console.log(`  + ${folder.uri} (${folder.name})`);
                this.workspaceFolders.push(folder.uri);
            });
        }

        // Handle removed folders
        if (event.removed.length > 0) {
            console.log("[Server] Removed folders:");
            event.removed.forEach((folder) => {
                console.log(`  - ${folder.uri} (${folder.name})`);
                this.workspaceFolders = this.workspaceFolders.filter((f) => f !== folder.uri);
            });
        }

        console.log("[Server] Current workspace folders:", this.workspaceFolders);
        console.log("[Server] ========================================");
    }

    // ===== FILE SYSTEM CHANGES EVENT =====
    private handleFileSystemChanges(params: DidChangeWatchedFilesParams): void {
        console.log("[Server] ===== FILE SYSTEM CHANGES =====");

        params.changes.forEach((change) => {
            const changeType = this.getFileChangeTypeString(change.type);
            console.log(`[Server] ${changeType}: ${change.uri}`);

            switch (change.type) {
                case FileChangeType.Created:
                    this.handleFileCreated(change.uri);
                    break;
                case FileChangeType.Changed:
                    this.handleFileChanged(change.uri);
                    break;
                case FileChangeType.Deleted:
                    this.handleFileDeleted(change.uri);
                    break;
            }
        });

        console.log("[Server] ====================================");
    }

    private handleFileCreated(uri: string): void {
        console.log(`[Server]   -> File created in workspace: ${uri}`);
        // Add your custom logic here
    }

    private handleFileChanged(uri: string): void {
        console.log(`[Server]   -> File changed in workspace: ${uri}`);
        // Add your custom logic here
    }

    private handleFileDeleted(uri: string): void {
        console.log(`[Server]   -> File deleted from workspace: ${uri}`);
        // Add your custom logic here
    }

    private getFileChangeTypeString(type: FileChangeType): string {
        switch (type) {
            case FileChangeType.Created:
                return "CREATED";
            case FileChangeType.Changed:
                return "CHANGED";
            case FileChangeType.Deleted:
                return "DELETED";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Start listening on the connection
     */
    public start(): void {
        console.log("[Server] Starting language server...");
        this.connection.listen();
    }
}

// Create and start the server
const server = new SimpleLanguageServer();
server.start();

export { SimpleLanguageServer };
