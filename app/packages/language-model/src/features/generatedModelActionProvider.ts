import type { GetFileActionsParams, GetFileActionsResponse, ActionIconNode } from "@mdeo/language-common";
import { convertIcon, ActionDisplayLocation } from "@mdeo/language-common";
import type { ActionProvider } from "@mdeo/language-shared";
import { Save } from "lucide";

/**
 * Action provider for generated model files (.m_gen).
 * Provides a "Save as Model" action that allows saving a generated model as a regular .m file.
 */
export class GeneratedModelActionProvider implements ActionProvider {
    async getFileActions(params: GetFileActionsParams): Promise<GetFileActionsResponse> {
        if (params.languageId !== "model_gen") {
            return { actions: [] };
        }

        const saveIcon: ActionIconNode = convertIcon(Save);

        return {
            actions: [
                {
                    name: "Save as Model",
                    icon: saveIcon,
                    key: "save-as-model",
                    displayLocations: [ActionDisplayLocation.EDITOR_TITLE, ActionDisplayLocation.CONTEXT_MENU]
                }
            ]
        };
    }
}
