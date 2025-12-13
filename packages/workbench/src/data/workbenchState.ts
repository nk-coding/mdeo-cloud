import { ref, type InjectionKey, type Ref, type ShallowRef } from "vue";
import type { FileSystem } from "./filesystem/fileSystem";
import { type EditorTab } from "./tab/editorTab";
import { PluginManager } from "./plugin/pluginManager";
import type { FileTypePlugin } from "./plugin/fileTypePlugin";

/**
 * Manager for the overall state of the workbench
 */
export class WorkbenchState {
    /**
     * The filesystem used by the workbench
     */
    readonly fileSystem: FileSystem;
    /**
     * The currently open tabs in the editor
     */
    readonly tabs = ref<EditorTab[]>([]);
    /**
     * The currently active tab in the editor
     */
    readonly activeTab = ref<EditorTab>();
    /**
     * Manager for the plugins used by the workbench
     */
    readonly pluginManager: PluginManager;


    /**
     * Supported file types for the workbench
     */
    get supportedFileTypes(): Ref<FileTypePlugin[]> {
        return this.pluginManager.fileTypePlugins;
    }

    constructor(fileSystem: FileSystem, fileTypePlugins: FileTypePlugin[]) {
        this.fileSystem = fileSystem;
        this.pluginManager = new PluginManager(fileTypePlugins);
    }
}

/**
 * Injection key for the WorkbenchState
 */
export const workbenchStateKey = Symbol("workbenchState") as InjectionKey<ShallowRef<WorkbenchState>>;
