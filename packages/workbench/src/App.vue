<template>
    <div class="app-container">
        <Splitpanes class="default-theme h-screen">
            <Pane min-size="20" max-size="50" size="25">
                <FileSystemExplorer
                    :file-system="fileSystem"
                    @file-activate="handleFileActivate"
                    @folder-activate="handleFolderActivate"
                    @fs-change="handleFileSystemChange"
                />
            </Pane>
            <Pane>
                <div ref="editorElement" class="editor-element h-full"></div>
            </Pane>
        </Splitpanes>
    </div>
</template>
<script setup lang="ts">
import { MonacoVscodeApiWrapper, type MonacoVscodeApiConfig } from 'monaco-languageclient/vscodeApiWrapper';
import { useWorkerFactory } from 'monaco-languageclient/workerFactory';
import { LogLevel, Uri } from 'vscode';
import { onMounted, ref, useTemplateRef } from 'vue';
import monacoEditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import { LanguageClientWrapper, type LanguageClientConfig } from 'monaco-languageclient/lcwrapper';
import { BrowserMessageReader, BrowserMessageWriter } from 'vscode-languageserver-protocol/browser';
import { EditorApp, type EditorAppConfig } from 'monaco-languageclient/editorApp';
import * as monaco from "monaco-editor";
import { getService, ISearchService } from "@codingame/monaco-vscode-api";
import getSearchServiceOverride from '@codingame/monaco-vscode-search-service-override';
import { QueryType } from '@codingame/monaco-vscode-api/vscode/vs/workbench/services/search/common/search';
import { E_COMMERCE_METAMODEL, LIBRARY_METAMODEL } from './testing/example';
import { Splitpanes, Pane } from 'splitpanes';
import 'splitpanes/dist/splitpanes.css';
import FileSystemExplorer from './components/FileSystemExplorer.vue';
import { BrowserFileSystem, type FileSystemEntry } from './lib/filesystem';


const editorElement = useTemplateRef("editorElement");

// File system instance
const fileSystem = new BrowserFileSystem()
const currentFile = ref<FileSystemEntry | null>(null)
const refreshTrigger = ref(0)
let editorApp: EditorApp | null = null

/**
 * Handles file activation from the file explorer
 */
function handleFileActivate(entry: FileSystemEntry): void {
    currentFile.value = entry
    if (editorApp && entry.content !== undefined) {
        const editor = editorApp.getEditor()
        if (editor) {
            editor.setValue(entry.content)
        }
    }
}

/**
 * Handles folder activation from the file explorer
 */
function handleFolderActivate(entry: FileSystemEntry): void {
    // Currently just focus the folder, could expand in the future
    console.log('Folder activated:', entry.name)
}

/**
 * Handles file system changes to refresh the view
 */
function handleFileSystemChange(): void {
    // Force reactivity update by incrementing trigger
    refreshTrigger.value++
}

onMounted(async () => {
    const vscodeApiConfig: MonacoVscodeApiConfig = {
        $type: "classic",
        viewsConfig: {
            $type: "EditorService",
            htmlContainer: editorElement.value!
        },
        logLevel: LogLevel.Debug,
        serviceOverrides: {
            ...getSearchServiceOverride(),
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
            documentSelector: ["metamodel"],
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
    }

    const languageClientWrapper = new LanguageClientWrapper(languageClientConfig);
    await languageClientWrapper.start();

    const editorAppConfig: EditorAppConfig = {
        editorOptions: {
            language: "metamodel",
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
            pattern: "class",
        },
        folderQueries:[
            
        ]
    })
    console.log(serachResult)
})


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