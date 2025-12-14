import * as monaco from "monaco-editor";
import { MonacoVscodeApiWrapper, type MonacoVscodeApiConfig } from "monaco-languageclient/vscodeApiWrapper";
import { LogLevel } from "vscode";
import { type Plugin, type InjectionKey, watch } from "vue";
import getSearchServiceOverride from "@codingame/monaco-vscode-search-service-override";
import { useWorkerFactory } from "monaco-languageclient/workerFactory";
import monacoEditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import { useColorMode } from "@vueuse/core";

export const monacoReadyKey: InjectionKey<Promise<void>> = Symbol("monacoReady");

export const monacoPlugin: Plugin = {
    install(app) {
        const monacoReadyPromise = setupMonaco();
        app.provide(monacoReadyKey, monacoReadyPromise);

        monacoReadyPromise.then(() => {
            monaco.editor.defineTheme("custom-dark", customDarkTheme);
            monaco.editor.defineTheme("custom-light", customLightTheme);
            app.runWithContext(() => {
                const theme = useColorMode();
                watch(
                    theme,
                    (newTheme) => {
                        monaco.editor.setTheme(newTheme === "dark" ? "custom-dark" : "custom-light");
                    },
                    { immediate: true }
                );
            });
        });
    }
};

async function setupMonaco() {
    const vscodeApiConfig: MonacoVscodeApiConfig = {
        $type: "classic",
        viewsConfig: {
            $type: "EditorService",
            // @ts-expect-error will be provided later
            htmlContainer: undefined
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
}

/**
 * Dark color theme
 */
const customDarkTheme: monaco.editor.IStandaloneThemeData = {
    base: "vs-dark",
    inherit: true,
    rules: [
        {
            token: "default",
            foreground: "#ff0000"
        },
        {
            token: "entity.name.function",
            foreground: "#dcdcaa"
        },
        {
            token: "variable",
            foreground: "#4fc1ff"
        },
        {
            token: "variable.property",
            foreground: "#9cdcfe"
        },
        {
            token: "string.quoted",
            foreground: "#ce9178"
        },
        {
            token: "string.escape",
            foreground: "#d7ba7d"
        },
        {
            token: "constant.numerical",
            foreground: "#dcdcaa"
        },
        {
            token: "delimiter.curly",
            foreground: "#569cd6"
        }
    ],
    colors: {
        "editor.background": "#030712"
    }
};

/**
 * Light color theme
 */
const customLightTheme: monaco.editor.IStandaloneThemeData = {
    base: "vs",
    inherit: true,
    rules: [
        {
            token: "default",
            foreground: "#ff0000"
        },
        {
            token: "entity.name.function",
            foreground: "#795e26"
        },
        {
            token: "variable",
            foreground: "#0070c1"
        },
        {
            token: "variable.property",
            foreground: "#001080"
        },
        {
            token: "string.quoted",
            foreground: "#a31515"
        },
        {
            token: "string.escape",
            foreground: "#ee0000"
        },
        {
            token: "constant.numerical",
            foreground: "#098658"
        },
        {
            token: "delimiter.curly",
            foreground: "#0000ff"
        }
    ],
    colors: {
        "editor.background": "#ffffff"
    }
};
