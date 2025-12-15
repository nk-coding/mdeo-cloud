import { computed, ref, type Reactive, type Ref } from "vue";
import { type EditorTab } from "./tab/editorTab";
import type { Plugin } from "./plugin/plugin";
import type { BackendApi } from "./api/backendApi";
import type { MonacoApi } from "@/plugins/monacoPlugin";
import type { Project } from "./project/project";
import type { Folder } from "./filesystem/file";
import { useFileTree } from "./filesystem/useFileTree";
import { BackendFileSystemProvider } from "./filesystem/fileSystemProvider";
import { registerFileSystemOverlay } from "@codingame/monaco-vscode-files-service-override";

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
    readonly fileTypePlugins = computed(() => [...this.plugins.value.values()].flatMap((plugin) => plugin.fileTypes));

    readonly fileTree: Reactive<Folder>;

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
    }

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
}

/**
 * Type for the sidebar
 */
export type SidebarType = "files" | "search" | "projects";
