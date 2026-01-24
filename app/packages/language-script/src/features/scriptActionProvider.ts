import type { GetFileActionsParams, GetFileActionsResponse, ActionIconNode } from "@mdeo/language-common";
import { convertIcon, FileCategory, parseUri } from "@mdeo/language-common";
import type { ActionProvider } from "@mdeo/language-shared";
import { ActionDisplayLocation } from "@mdeo/language-common";
import { Play } from "lucide";
import { URI } from "langium";

/**
 * Action provider for script files.
 * Provides the "run" action for executing script methods.
 */
export class ScriptActionProvider implements ActionProvider {
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
}
