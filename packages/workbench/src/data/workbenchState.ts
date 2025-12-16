import { computed, ref, watch, type Reactive, type Ref } from "vue";
import { type EditorTab } from "./tab/editorTab";
import type { Plugin } from "./plugin/plugin";
import type { BackendApi } from "./api/backendApi";
import type { MonacoApi } from "@/plugins/monacoPlugin";
import type { Project } from "./project/project";
import type { Folder } from "./filesystem/file";
import { useFileTree } from "./filesystem/useFileTree";
import { BackendFileSystemProvider } from "./filesystem/fileSystemProvider";
import { registerFileSystemOverlay } from "@codingame/monaco-vscode-files-service-override";
import type { ServerLanguagePlugin, ServerPlugin } from "./plugin/serverPlugin";
import { BrowserMessageReader, BrowserMessageWriter } from "vscode-languageserver-protocol/browser";
import { MonacoLanguageClient } from "monaco-languageclient";
import { CloseAction, ErrorAction } from "vscode-languageclient";
import { Uri } from "vscode";
import { watchArray } from "@vueuse/core";

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
            }
        }
    });

    /**
     * The current langaugeclient if existing
     * should be disposed before a new one is created
     */
    private languageClient: MonacoLanguageClient | undefined;

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

    private initiLsp() {
        const worker = new Worker(new URL("../server/extensibleLangiumServer.ts", import.meta.url), { type: "module" });
        const reader = new BrowserMessageReader(worker);
        const writer = new BrowserMessageWriter(worker);

        watchArray(this.languagePlugins, (_new, _old, added) => {
            for (const plugin of added) {
                if (!this.registeredLanguages.has(plugin.id)) {
                    this.monacoApi.monaco.languages.register({ id: plugin.id });
                    this.registeredLanguages.add(plugin.id);
                }
            }
        });

        watch([this.project, this.serverPlugins], async ([newProject]) => {
            await this.languageClient?.stop();
            await this.languageClient?.dispose();
            this.languageClient = undefined;

            if (newProject != undefined) {
                const languageClient = new MonacoLanguageClient({
                    name: "Extensible Language Client",
                    clientOptions: {
                        documentSelector: this.languagePlugins.value.map((plugin) => ({ language: plugin.id })),
                        errorHandler: {
                            error: () => ({ action: ErrorAction.Continue }),
                            closed: () => ({ action: CloseAction.DoNotRestart })
                        },
                        workspaceFolder: {
                            uri: Uri.file(`/${newProject.id}`),
                            name: newProject.name,
                            index: 0
                        }
                    },
                    messageTransports: { reader, writer }
                });
                this.languageClient = languageClient;
                languageClient.start();
            }
        });
    }
}

/**
 * Type for the sidebar
 */
export type SidebarType = "files" | "search" | "projects";
