<template>
    <div class="app-container">
        <Splitpanes class="h-screen">
            <Pane min-size="0" max-size="50" size="25">
                <div class="p-4 h-full overflow-auto">
                    <h3 class="mb-3 text-sm font-medium">Project Explorer</h3>
                    <Files />
                </div>
            </Pane>
            <Pane>
                <div class="flex flex-col h-full">
                    <Tabs />
                    <div ref="editorElement" class="editor-element flex-1"></div>
                </div>
            </Pane>
        </Splitpanes>
    </div>
</template>
<script setup lang="ts">
import { MonacoVscodeApiWrapper, type MonacoVscodeApiConfig } from "monaco-languageclient/vscodeApiWrapper";
import { useWorkerFactory } from "monaco-languageclient/workerFactory";
import { LogLevel, Uri } from "vscode";
import { onMounted, provide, shallowRef, useTemplateRef } from "vue";
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
import Files from "./components/files/Files.vue";
import Tabs from "./components/tabs/Tabs.vue";
import { BrowserFileSystem } from "./data/filesystem/browserFileSystem";
import { WorkbenchState, workbenchStateKey } from "./data/workbenchState";
import { useColorMode } from "@vueuse/core";

const editorElement = useTemplateRef("editorElement");

const workbenchState = shallowRef(
    new WorkbenchState(new BrowserFileSystem(), [
        {
            name: "Metamodel",
            extension: ".mm"
        }
    ])
);

const mode = useColorMode();

let editorApp: EditorApp | null = null;

provide(workbenchStateKey, workbenchState);

onMounted(async () => {
    const vscodeApiConfig: MonacoVscodeApiConfig = {
        $type: "classic",
        viewsConfig: {
            $type: "EditorService",
            htmlContainer: editorElement.value!
        },
        logLevel: LogLevel.Warning,
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

    monaco.languages.register({
        id: "metamodel",
        extensions: [".mm"]
    });

    const worker = new Worker(new URL("./server/metamodelServer.ts", import.meta.url), { type: "module" });
    const reader = new BrowserMessageReader(worker);
    const writer = new BrowserMessageWriter(worker);

    const languageClientConfig: LanguageClientConfig = {
        languageId: "metamodel",
        clientOptions: {
            documentSelector: [{ scheme: "file", language: "metamodel" }]
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
<style>
@supports selector(::-webkit-scrollbar) {
  *::-webkit-scrollbar {
    width: 4px;
    height: 4px;
  }

  *::-webkit-scrollbar-thumb {
    background: transparent;
  }

  *:hover::-webkit-scrollbar-thumb,
  *:focus-within::-webkit-scrollbar-thumb {
    background: var(--muted);
  }
}

</style>