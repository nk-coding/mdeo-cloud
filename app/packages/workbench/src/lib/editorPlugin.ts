import type { Plugin, InjectionKey } from "vue";
import type { PluginContext } from "@mdeo/editor-common";
import * as inversify from "inversify";
import * as glspClient from "@eclipse-glsp/client";
import * as glspSprotty from "@eclipse-glsp/sprotty";
import * as glspProtocol from "@eclipse-glsp/protocol";
import * as minisearch from "minisearch";
import * as lucide from "lucide";
import { initializeEditorPluginContext } from "@mdeo/editor-common";

/**
 * Injection key for accessing the editor plugin context in Vue components.
 */
export const editorContextKey: InjectionKey<PluginContext> = Symbol("editorContext");

/**
 * Vue plugin that provides editor context with GLSP client and Sprotty dependencies.
 * Initializes the editor plugin context and makes it available via dependency injection.
 */
export const editorPlugin: Plugin = {
    install(app) {
        const context: PluginContext = {
            inversify,
            "@eclipse-glsp/client": glspClient,
            "@eclipse-glsp/sprotty": glspSprotty,
            "@eclipse-glsp/protocol": glspProtocol,
            minisearch,
            lucide
        };

        initializeEditorPluginContext(context);

        app.provide(editorContextKey, context);
    }
};
