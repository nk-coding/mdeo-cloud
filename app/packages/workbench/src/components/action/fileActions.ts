import type { ComputedRef, Ref, ShallowRef } from "vue";
import type { MonacoLanguageClient } from "monaco-languageclient";
import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";
import {
    createActionProtocol,
    type ActionStartParams,
    type FileAction,
    type FileMenuActionData
} from "@mdeo/language-common";
import * as vscodeJsonrpc from "vscode-jsonrpc";

const ActionProtocol = createActionProtocol(vscodeJsonrpc);

interface FileActionContext {
    languageClient: ShallowRef<MonacoLanguageClient | undefined>;
    languagePluginByExtension: ComputedRef<Map<string, ResolvedWorkbenchLanguagePlugin>>;
}

/**
 * Fetches all available file actions for the given file.
 *
 * @param context Access to language client and language plugin lookup
 * @param fileUri The URI of the file
 * @param fileExtension The extension of the file
 * @returns Available file actions
 */
export async function fetchFileActions(context: FileActionContext, fileUri: string, fileExtension: string) {
    if (!context.languageClient.value) {
        return [];
    }

    const languagePlugin = context.languagePluginByExtension.value.get(fileExtension);
    if (!languagePlugin) {
        return [];
    }

    try {
        const response = await context.languageClient.value.sendRequest(ActionProtocol.GetFileActionsRequest, {
            languageId: languagePlugin.id,
            fileUri
        });
        return response?.actions ?? [];
    } catch {
        return [];
    }
}

/**
 * Triggers a file action by publishing it as pending action.
 *
 * @param pendingAction Target pending action reference
 * @param languagePluginByExtension Lookup for language plugin by extension
 * @param action The action to trigger
 * @param fileUri URI of the file
 * @param fileExtension Extension of the file
 */
export function triggerFileAction(
    pendingAction: Ref<ActionStartParams | undefined>,
    languagePluginByExtension: ComputedRef<Map<string, ResolvedWorkbenchLanguagePlugin>>,
    action: FileAction,
    fileUri: string,
    fileExtension: string
): void {
    const languagePlugin = languagePluginByExtension.value.get(fileExtension);
    if (!languagePlugin) {
        return;
    }

    pendingAction.value = {
        type: action.key,
        languageId: languagePlugin.id,
        data: {
            uri: fileUri
        } satisfies FileMenuActionData
    };
}
