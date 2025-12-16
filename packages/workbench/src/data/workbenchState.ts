import { computed, ref, watch, type Reactive, type Ref } from "vue";
import { type EditorTab } from "./tab/editorTab";
import type { Plugin } from "./plugin/plugin";
import type { BackendApi } from "./api/backendApi";
import type { MonacoApi } from "@/plugins/monacoPlugin";
import type { Project } from "./project/project";
import type { File, Folder } from "./filesystem/file";
import { useFileTree } from "./filesystem/useFileTree";
import { BackendFileSystemProvider } from "./filesystem/fileSystemProvider";
import { registerFileSystemOverlay } from "@codingame/monaco-vscode-files-service-override";
import type { ServerLanguagePlugin, ServerPlugin } from "./plugin/serverPlugin";
import { BrowserMessageReader, BrowserMessageWriter } from "vscode-languageserver-protocol/browser";
import { MonacoLanguageClient } from "monaco-languageclient";
import { CloseAction, ErrorAction, State, type ResponseMessage } from "vscode-languageclient";
import { Uri } from "vscode";
import { watchArray } from "@vueuse/core";
import { reinitializeWorkspace } from "@codingame/monaco-vscode-configuration-service-override";
import { GetPluginsRequest, ServerReadyNotification } from "@/server/protocol";

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
    readonly plugins = ref<Map<string, Plugin>>(new Map());

    /**
     * File type plugins provided by any plugin
     */
    readonly languagePlugins = computed(() =>
        [...this.plugins.value.values()].flatMap((plugin) => plugin.languagePlugins)
    );

    /**
     * Server plugins provided by any plugin
     */
    readonly serverPlugins = computed<ServerPlugin[]>(() =>
        [...this.plugins.value.values()].flatMap((plugin) => [
            ...plugin.serverContributionPlugins,
            ...plugin.languagePlugins.map(
                (languagePlugin) =>
                    ({
                        ...languagePlugin.serverPlugin,
                        type: "language",
                        languageId: languagePlugin.id,
                        extension: languagePlugin.extension
                    }) satisfies ServerLanguagePlugin
            )
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
     * The current language client if existing
     * should be disposed before a new one is created
     */
    private languageClient: MonacoLanguageClient | undefined;

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
        const fileSystemProvider = new BackendFileSystemProvider(backendApi, this._project);
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
     * Recreates the language server and client when configuration changes
     * Terminates the old worker and creates a new one
     *
     * @param newProject The project to create the client for
     * @param serverPlugins The list of server plugins to initialize
     */
    private async recreateLanguageServer(newProject: Project | undefined, serverPlugins: ServerPlugin[]) {
        await this.cleanupLanguageClient();
        this.terminateWorker();

        if (newProject != undefined && serverPlugins.length > 0) {
            this.createWorkerAndSetupHandlers(newProject, serverPlugins);
        }
    }

    /**
     * Cleans up the existing language client
     */
    private async cleanupLanguageClient() {
        try {
            await this.languageClient?.stop();
            await this.languageClient?.dispose();
        } catch {
            // ignore errors during disposal
        }
        this.languageClient = undefined;
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
        this.languageClient = languageClient;
        languageClient.start();

        setTimeout(async () => {
            if (this.languageClient?.state === State.Starting) {
                await this.recreateLanguageServer(project, this.serverPlugins.value);
            }
        }, 1000);
    }

    openTab(file: File, temporary: boolean) {
        const existingTab = this.tabs.value.find((tab) => tab.file.id.toString() === file.id.toString());

        if (existingTab) {
            this.activeTab.value = existingTab;
            if (!temporary && existingTab.temporary) {
                existingTab.temporary = false;
            }
        } else {
            const newTab = {
                file: file,
                temporary: temporary
            };
            this.tabs.value.push(newTab);
            this.activeTab.value = newTab;
        }
    }
}

/**
 * Type for the sidebar
 */
export type SidebarType = "files" | "search" | "projects";
