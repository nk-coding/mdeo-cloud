<template>
    <div class="app-container">
        <Splitpanes class="h-screen">
            <Pane min-size="0" max-size="50" size="25">
                <div class="p-4 h-full overflow-auto">
                    <h3 class="mb-3 text-sm font-medium">Project Explorer</h3>
                    <Files
                        :filesystem="fileSystem"
                        @select="handleFileSelect"
                        @create-file="handleCreateFile"
                        @create-folder="handleCreateFolder"
                        @rename="handleRename"
                        @delete="handleDelete"
                    />
                </div>
            </Pane>
            <Pane>
                <div ref="editorElement" class="editor-element h-full"></div>
            </Pane>
        </Splitpanes>
    </div>
</template>
<script setup lang="ts">
import { MonacoVscodeApiWrapper, type MonacoVscodeApiConfig } from "monaco-languageclient/vscodeApiWrapper";
import { useWorkerFactory } from "monaco-languageclient/workerFactory";
import { LogLevel, Uri } from "vscode";
import { onMounted, ref, useTemplateRef } from "vue";
import monacoEditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import { LanguageClientWrapper, type LanguageClientConfig } from "monaco-languageclient/lcwrapper";
import { BrowserMessageReader, BrowserMessageWriter } from "vscode-languageserver-protocol/browser";
import { EditorApp, type EditorAppConfig } from "monaco-languageclient/editorApp";
import * as monaco from "monaco-editor";
import { getService, ISearchService } from "@codingame/monaco-vscode-api";
import getSearchServiceOverride from "@codingame/monaco-vscode-search-service-override";
import { QueryType } from "@codingame/monaco-vscode-api/vscode/vs/workbench/services/search/common/search";
// @ts-ignoreimport
import { Splitpanes, Pane } from "splitpanes";
import "splitpanes/dist/splitpanes.css";
import { BrowserFileSystem, type FileSystemNode } from "./data/files";
import Files from "./components/files/Files.vue";

const editorElement = useTemplateRef("editorElement");

// File system instance
const fileSystem = new BrowserFileSystem();
let editorApp: EditorApp | null = null;

const selectedEntry = ref<FileSystemNode | null>(null);

function handleFileSelect(entry: FileSystemNode) {
    selectedEntry.value = entry;
    console.log("Selected:", entry);
}

async function handleCreateFile(name: string, parentId?: string) {
    if (name.trim()) {
        try {
            const newFile = await fileSystem.createFileSimple(name, "", parentId);
            console.log("Created file:", newFile);
        } catch (error) {
            console.error("Failed to create file:", error);
        }
    }
}

async function handleCreateFolder(name: string, parentId?: string) {
    if (name.trim()) {
        try {
            const newFolder = await fileSystem.createFolderSimple(name, parentId);
            console.log("Created folder:", newFolder);
        } catch (error) {
            console.error("Failed to create folder:", error);
        }
    }
}

async function handleRename(id: string, newName: string) {
    try {
        const success = await fileSystem.renameEntry(id, newName);
        console.log("Renamed:", success ? "success" : "failed");
    } catch (error) {
        console.error("Failed to rename:", error);
    }
}

async function handleDelete(id: string) {
    try {
        const success = await fileSystem.deleteEntry(id);
        console.log("Deleted:", success ? "success" : "failed");
    } catch (error) {
        console.error("Failed to delete:", error);
    }
}

onMounted(async () => {
    // Initialize filesystem
    await fileSystem.initialize();
    console.log("Filesystem initialized");

    const vscodeApiConfig: MonacoVscodeApiConfig = {
        $type: "classic",
        viewsConfig: {
            $type: "EditorService",
            htmlContainer: editorElement.value!
        },
        logLevel: LogLevel.Debug,
        serviceOverrides: {
            ...getSearchServiceOverride()
        },
        monacoWorkerFactory: () => {
            useWorkerFactory({
                workerLoaders: {
                    TextEditorWorker: () => new monacoEditorWorker()
                }
            });
        }
    };
    const vscodeApi = new MonacoVscodeApiWrapper(vscodeApiConfig);
    await vscodeApi.start();

    monaco.languages.register({ id: "metamodel" });

    const worker = new Worker(new URL("./server/metamodelServer.ts", import.meta.url), { type: "module" });
    const reader = new BrowserMessageReader(worker);
    const writer = new BrowserMessageWriter(worker);

    const languageClientConfig: LanguageClientConfig = {
        languageId: "metamodel",
        clientOptions: {
            documentSelector: ["metamodel"]
        },
        connection: {
            options: {
                $type: "WorkerDirect",
                worker
            },
            messageTransports: {
                reader,
                writer
            }
        }
    };

    const languageClientWrapper = new LanguageClientWrapper(languageClientConfig);
    await languageClientWrapper.start();

    const editorAppConfig: EditorAppConfig = {
        editorOptions: {
            language: "metamodel"
        },
        overrideAutomaticLayout: false
    };

    editorApp = new EditorApp(editorAppConfig);
    await editorApp.start(editorElement.value!);
    const editor = editorApp.getEditor()!;
    editor.layout();

    const searchService = await getService(ISearchService);
    const serachResult = await searchService.textSearch({
        type: QueryType.Text,
        contentPattern: {
            pattern: "class"
        },
        folderQueries: []
    });
    console.log(serachResult);
});
</script>
<style scoped>
.app-container {
    height: 100vh;
    width: 100vw;
}

.editor-element {
    width: 100%;
    height: 100%;
}
</style>
