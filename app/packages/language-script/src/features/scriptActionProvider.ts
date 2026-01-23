import type { GetFileActionsParams, GetFileActionsResponse, ActionIconNode } from "@mdeo/language-common";
import { convertIcon } from "@mdeo/language-common";
import type { ActionProvider } from "@mdeo/language-shared";
import { ActionDisplayLocation } from "@mdeo/language-common";
import { Play } from "lucide";

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
        // Only provide actions for script files
        if (params.languageId !== "script") {
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
