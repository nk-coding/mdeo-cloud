import { computed, nextTick, ref, shallowRef, watch, type Reactive } from "vue";
import { type EditorTab } from "./tab/editorTab";
import type { WorkbenchPlugin } from "./plugin/plugin";
import type { BackendApi } from "./api/backendApi";
import type { MonacoApi } from "@/lib/monacoPlugin";
import type { Project } from "./project/project";
import type { Folder } from "./filesystem/file";
import { useFileTree } from "./filesystem/useFileTree";
import { BackendFileSystemProvider } from "./filesystem/fileSystemProvider";
import { registerFileSystemOverlay } from "@codingame/monaco-vscode-files-service-override";
import type { ServerContributionPlugin, ServerLanguagePlugin, ServerPlugin } from "./plugin/serverPlugin";
import { BrowserMessageReader, BrowserMessageWriter } from "vscode-languageserver-protocol/browser";
import { MonacoLanguageClient } from "monaco-languageclient";
import { CloseAction, ErrorAction, State, type ResponseMessage } from "vscode-languageclient";
import { Uri } from "vscode";
import { watchArray } from "@vueuse/core";
import { reinitializeWorkspace } from "@codingame/monaco-vscode-configuration-service-override";
import {
    GetPluginsRequest,
    ServerReadyNotification,
    ReadFileRequest,
    StatRequest,
    ReadDirectoryRequest,
    ReadMetadataRequest,
    WriteMetadataRequest,
    type ReadFileParams,
    type StatParams,
    type StatResponse,
    type ReadDirectoryParams,
    type FileSystemNode,
    type ReadMetadataParams,
    type WriteMetadataParams
} from "@/server/protocol";
import type { ResolvedLanguagePlugin } from "./plugin/languagePlugin";

/**
 * Manager for the overall state of the workbench
 */
export class WorkbenchState {
    /**
     * The currently active sidebar
     */
    readonly activeSidebar = ref<SidebarType>("projects");

    /**
     * Whether the sidebar is collapsed
     */
    readonly sidebarCollapsed = ref(false);

    /**
     * Internal ref for the currently loaded project
     */
    private readonly _project = ref<Project>();

    /**
     * The currently open tabs in the editor
     */
    readonly tabs = ref<EditorTab[]>([]);

    /**
     * The currently active tab in the editor
     */
    readonly activeTab = ref<EditorTab>();

    /**
     * Plugins loaded in the workbench
     */
    readonly plugins = ref<Map<string, WorkbenchPlugin>>(new Map());

    /**
     * The current language client if existing
     * should be disposed before a new one is created
     */
    readonly languageClient = shallowRef<MonacoLanguageClient>();

    /**
     * Counter for language client restarts
     * Can be used as vue key to force re-creation of components
     */
    readonly clientVersionCounter = ref(0);

    /**
     * File type plugins provided by any plugin
     */
    readonly languagePlugins = computed<ResolvedLanguagePlugin[]>(() => {
        const plugins = [...this.plugins.value.values()];
        const contributionPluginsByLanguage = new Map<string, ServerContributionPlugin[]>();
        for (const plugin of plugins) {
            for (const contributionPlugin of plugin.serverContributionPlugins) {
                if (!contributionPluginsByLanguage.has(contributionPlugin.languageId)) {
                    contributionPluginsByLanguage.set(contributionPlugin.languageId, []);
                }
                contributionPluginsByLanguage.get(contributionPlugin.languageId)!.push(contributionPlugin);
            }
        }

        return plugins
            .flatMap((plugin) =>
                plugin.languagePlugins.map((langPlugin) => ({ ...langPlugin }) as ResolvedLanguagePlugin)
            )
            .map((langPlugin) => ({
                ...langPlugin,
                serverContributionPlugins: contributionPluginsByLanguage.get(langPlugin.id) ?? []
            }));
    });

    /**
     * Map of language plugins by their file extension
     */
    readonly languagePluginByExtension = computed<Map<string, ResolvedLanguagePlugin>>(() => {
        const map = new Map<string, ResolvedLanguagePlugin>();
        for (const langPlugin of this.languagePlugins.value) {
            if (map.has(langPlugin.extension)) {
                throw new Error(`Multiple language plugins registered for extension ${langPlugin.extension}`);
            }
            map.set(langPlugin.extension, langPlugin);
        }
        return map;
    });

    /**
     * Server plugins provided by any plugin
     */
    readonly serverPlugins = computed<ServerPlugin[]>(() =>
        [...this.languagePlugins.value].flatMap((plugin) => [
            ...plugin.serverContributionPlugins,
            {
                ...plugin.serverPlugin,
                type: "language",
                languageId: plugin.id,
                extension: plugin.extension
            } satisfies ServerLanguagePlugin
        ])
    );

    /**
     * The root folder of the file tree
     */
    readonly fileTree: Reactive<Folder>;

    /**
     * The project currently loaded in the workbench (if any)
     */
    readonly project = computed<Project | undefined>({
        get: () => this._project.value,
        set: (newProject) => {
            if (this._project.value?.id !== newProject?.id) {
                this._project.value = newProject;
                this.tabs.value = [];
                this.activeTab.value = undefined;

                const newPath = `/${newProject?.id ?? ""}`;
                if (window.location.pathname !== newPath) {
                    window.history.pushState(null, "", newPath);
                }

                if (newProject != undefined) {
                    reinitializeWorkspace({
                        id: newProject.id,
                        uri: Uri.parse(`file:///${newProject.id}`)
                    });
                }
            }
        }
    });

    /**
     * The current language server worker
     */
    private languageServerWorker: Worker | undefined;

    /**
     * Set of languages already registered with monaco
     */
    private registeredLanguages = new Set<string>();

    /**
     * Creates a new workbench state manager
     *
     * @param monacoApi The Monaco API instance
     * @param backendApi The backend API instance
     */
    constructor(
        readonly monacoApi: MonacoApi,
        readonly backendApi: BackendApi
    ) {
        const fileSystemProvider = new BackendFileSystemProvider(backendApi);
        registerFileSystemOverlay(1, fileSystemProvider);
        this.fileTree = useFileTree(monacoApi, this);
        this.initiLsp();
    }

    /**
     * Initializes the languge server and client
     * and sets up watching for project and language changes
     */
    private initiLsp() {
        watchArray(
            this.languagePlugins,
            (_new, _old, added) => {
                for (const plugin of added) {
                    if (!this.registeredLanguages.has(plugin.id)) {
                        this.monacoApi.monaco.languages.register({ id: plugin.id });
                        this.monacoApi.monaco.languages.setLanguageConfiguration(
                            plugin.id,
                            plugin.languageConfiguration
                        );
                        this.monacoApi.monaco.languages.setMonarchTokensProvider(
                            plugin.id,
                            this.generateMonarchTokensProvider(plugin)
                        );
                        this.registeredLanguages.add(plugin.id);
                    }
                }
            },
            { immediate: true }
        );

        watch(
            [this.project, this.serverPlugins],
            async ([newProject, newServerPlugins]) => {
                await this.recreateLanguageServer(newProject, newServerPlugins);
            },
            { immediate: true }
        );
    }

    /**
     * Generates monarch tokens provider with merged keywords from contribution plugins
     *
     * @param plugin The resolved language plugin
     * @returns The monarch tokens provider with merged keywords
     */
    private generateMonarchTokensProvider(plugin: ResolvedLanguagePlugin) {
        return {
            ...plugin.monarchTokensProvider,
            keywords: [
                ...plugin.monarchTokensProvider.keywords,
                ...plugin.serverContributionPlugins.flatMap(
                    (contributionPlugin) => contributionPlugin.additionalKeywords ?? []
                )
            ]
        };
    }

    /**
     * Recreates the language server and client when configuration changes
     * Terminates the old worker and creates a new one
     *
     * @param newProject The project to create the client for
     * @param serverPlugins The list of server plugins to initialize
     */
    private async recreateLanguageServer(newProject: Project | undefined, serverPlugins: ServerPlugin[]) {
        const languageClient = this.languageClient.value;
        this.languageClient.value = undefined;
        await nextTick();
        if (languageClient != undefined) {
            try {
                await languageClient.stop();
                await languageClient.dispose();
            } catch {
                // ignore errors during disposal
            }
        }
        this.terminateWorker();

        if (newProject != undefined) {
            this.clientVersionCounter.value += 1;
            this.createWorkerAndSetupHandlers(newProject, serverPlugins);
        }
    }

    /**
     * Terminates the language server worker
     */
    private terminateWorker() {
        if (this.languageServerWorker != undefined) {
            this.languageServerWorker.terminate();
            this.languageServerWorker = undefined;
        }
    }

    /**
     * Creates a new worker and sets up message handlers
     *
     * @param project The project to create the worker for
     * @param serverPlugins The list of server plugins
     */
    private createWorkerAndSetupHandlers(project: Project, serverPlugins: ServerPlugin[]) {
        const worker = new Worker(new URL("../server/extensibleLangiumServer.ts", import.meta.url), {
            type: "module"
        });
        this.languageServerWorker = worker;

        const reader = new BrowserMessageReader(worker);
        const writer = new BrowserMessageWriter(worker);

        reader.listen((message: any) => {
            if (message.method === GetPluginsRequest.method && message.id != undefined) {
                this.handlePluginRequest(writer, message.id, serverPlugins);
            } else if (message.method === ServerReadyNotification.method) {
                this.createLanguageClient(reader, writer, project);
            }
        });
    }

    /**
     * Handles the plugin configuration request from the server
     *
     * @param writer The message writer
     * @param requestId The request ID to respond to
     * @param serverPlugins The list of server plugins to send
     */
    private handlePluginRequest(
        writer: BrowserMessageWriter,
        requestId: number | string,
        serverPlugins: ServerPlugin[]
    ) {
        const response: GetPluginsRequest.Response = {
            plugins: serverPlugins
        };
        const responseMessage: ResponseMessage = {
            jsonrpc: "2.0",
            id: requestId,
            result: response
        };
        writer.write(responseMessage);
    }

    /**
     * Creates and starts the Monaco language client
     *
     * @param reader The message reader
     * @param writer The message writer
     * @param project The project to initialize the client for
     */
    private createLanguageClient(reader: BrowserMessageReader, writer: BrowserMessageWriter, project: Project) {
        const languageClient = new MonacoLanguageClient({
            name: "Extensible Language Client",
            clientOptions: {
                documentSelector: this.languagePlugins.value.map((plugin) => ({ language: plugin.id })),
                errorHandler: {
                    error: () => ({ action: ErrorAction.Continue }),
                    closed: () => ({ action: CloseAction.DoNotRestart })
                },
                workspaceFolder: {
                    uri: Uri.file(`/${project.id}`),
                    name: project.name,
                    index: 0
                }
            },
            messageTransports: { reader, writer }
        });
        this.languageClient.value = languageClient;

        // Register file system request handlers
        this.registerFileSystemHandlers(languageClient);

        languageClient.start();

        setTimeout(async () => {
            if (this.languageClient.value?.state === State.Starting) {
                await this.recreateLanguageServer(project, this.serverPlugins.value);
            }
        }, 1000);
    }

    /**
     * Extracts the path from a URI.
     * Expected format: schema://projectId/path
     */
    private extractPath(uri: Uri): string {
        const fullPath = uri.path;
        const parts = fullPath.substring(1).split("/");
        const path = "/" + parts.slice(1).join("/");
        return path;
    }

    /**
     * Registers file system request handlers on the language client
     *
     * @param client The language client to register handlers on
     */
    private registerFileSystemHandlers(client: MonacoLanguageClient) {
        client.onRequest(ReadFileRequest.type, async (params: ReadFileParams): Promise<string> => {
            const uri = Uri.parse(params.uri);
            const result = await this.monacoApi.fileService.readFile(uri);
            return result.value.toString();
        });

        client.onRequest(StatRequest.type, async (params: StatParams): Promise<StatResponse> => {
            const uri = Uri.parse(params.uri);
            const stat = await this.monacoApi.fileService.resolve(uri, { resolveMetadata: false });
            return {
                isFile: stat.isFile,
                isDirectory: stat.isDirectory,
                uri: params.uri
            };
        });

        client.onRequest(ReadDirectoryRequest.type, async (params: ReadDirectoryParams): Promise<FileSystemNode[]> => {
            const uri = Uri.parse(params.uri);
            const stat = await this.monacoApi.fileService.resolve(uri, { resolveMetadata: false });
            if (!stat.children) {
                throw new Error("Not a directory");
            }
            return stat.children.map((child) => ({
                isFile: child.isFile,
                isDirectory: child.isDirectory,
                uri: child.resource.toString()
            }));
        });

        client.onRequest(ReadMetadataRequest.type, async (params: ReadMetadataParams): Promise<object> => {
            const uri = Uri.parse(params.uri);
            const result = await this.backend.readMetadata(this.activeProject.value!.id, this.extractPath(uri));
            if (result.ok) {
                return result.value;
            }
            throw new Error("Failed to read metadata");
        });

        client.onRequest(WriteMetadataRequest.type, async (params: WriteMetadataParams): Promise<void> => {
            const uri = Uri.parse(params.uri);
            const result = await this.backend.writeMetadata(
                this.activeProject.value!.id,
                this.extractPath(uri),
                params.metadata
            );
            if (!result.ok) {
                throw new Error("Failed to write metadata");
            }
        });
    }
}

/**
 * Type for the sidebar
 */
export type SidebarType = "files" | "search" | "projects";
