<template>
    <div ref="editorElement" class="editor-element"></div>
</template>
<script setup lang="ts">
import { MonacoVscodeApiWrapper, type MonacoVscodeApiConfig } from 'monaco-languageclient/vscodeApiWrapper';
import { useWorkerFactory } from 'monaco-languageclient/workerFactory';
import { LogLevel, Uri } from 'vscode';
import { onMounted, useTemplateRef } from 'vue';
import monacoEditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import { LanguageClientWrapper, type LanguageClientConfig } from 'monaco-languageclient/lcwrapper';
import { BrowserMessageReader, BrowserMessageWriter } from 'vscode-languageserver-protocol/browser';
import { EditorApp, type EditorAppConfig } from 'monaco-languageclient/editorApp';
import * as monaco from "monaco-editor";
import { getService, ISearchService } from "@codingame/monaco-vscode-api";
import getSearchServiceOverride from '@codingame/monaco-vscode-search-service-override';
import { QueryType } from '@codingame/monaco-vscode-api/vscode/vs/workbench/services/search/common/search';
import { E_COMMERCE_METAMODEL, LIBRARY_METAMODEL } from './testing/example';


const editorElement = useTemplateRef("editorElement");

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

    const editorApp = new EditorApp(editorAppConfig);
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
.editor-element {
    width: 100vw;
    height: 100vh;
}
</style>