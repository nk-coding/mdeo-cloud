import type { GetFileActionsParams, GetFileActionsResponse, ActionIconNode } from "@mdeo/language-common";
import { convertIcon, FileCategory, parseUri } from "@mdeo/language-common";
import { sharedImport, type ActionProvider } from "@mdeo/language-shared";
import { ActionDisplayLocation } from "@mdeo/language-common";
import { Play } from "lucide";
import type { LangiumSharedServices } from "langium/lsp";
import type { ScriptType } from "../grammar/scriptTypes.js";

const { URI } = sharedImport("langium");

/**
 * Action provider for script files.
 * Provides the "run" action for executing script methods.
 */
export class ScriptActionProvider implements ActionProvider {
    constructor(private readonly sharedServices: LangiumSharedServices) {}

    /**
     * Gets the list of actions available for a script file.
     *
     * @param params The parameters containing the language ID and file URI
     * @returns A promise resolving to the list of available actions
     */
    async getFileActions(params: GetFileActionsParams): Promise<GetFileActionsResponse> {
        if (params.languageId !== "script") {
            return { actions: [] };
        }
        const parsedUri = parseUri(URI.parse(params.fileUri));
        if (parsedUri.category !== FileCategory.RegularFile) {
            return { actions: [] };
        }

        if (!(await this.hasExecutableMethod(params.fileUri))) {
            return { actions: [] };
        }

        const runIcon: ActionIconNode = convertIcon(Play);

        return {
            actions: [
                {
                    name: "Run",
                    icon: runIcon,
                    key: "run",
                    displayLocations: [ActionDisplayLocation.EDITOR_TITLE, ActionDisplayLocation.CONTEXT_MENU]
                }
            ]
        };
    }

    /**
     * Checks whether a script contains at least one parameterless function.
     *
     * @param uri The script URI
     * @returns True if an executable function exists
     */
    private async hasExecutableMethod(uri: string): Promise<boolean> {
        const parsedUri = URI.parse(uri);
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(parsedUri);

        if (!document || document.parseResult.lexerErrors.length > 0 || document.parseResult.parserErrors.length > 0) {
            return false;
        }

        const root = document.parseResult.value as ScriptType;
        return root.functions.some(
            (func) => func.parameterList == undefined || func.parameterList.parameters.length === 0
        );
    }
}
