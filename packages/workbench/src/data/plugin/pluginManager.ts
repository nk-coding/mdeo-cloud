import { ref } from "vue";
import { type FileTypePlugin } from "./fileTypePlugin";

/**
 * Manager for the plugins used by the workbench
 */
export class PluginManager {
    /**
     * The loaded file type plugins
     */
    readonly fileTypePlugins = ref<FileTypePlugin[]>([]);

    constructor(fileTypePlugins: FileTypePlugin[]) {
        this.fileTypePlugins.value = fileTypePlugins;
    }
}
