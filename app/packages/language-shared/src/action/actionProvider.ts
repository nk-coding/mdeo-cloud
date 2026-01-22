import type { GetFileActionsParams, GetFileActionsResponse } from "@mdeo/language-common";

/**
 * Service interface for providing file actions.
 * Provides methods to get available actions for files.
 */
export interface ActionProvider {
    /**
     * Gets the list of actions available for a file.
     *
     * @param params The parameters containing the language ID and file URI
     * @returns A promise resolving to the list of available actions
     */
    getFileActions(params: GetFileActionsParams): Promise<GetFileActionsResponse>;
}

/**
 * Default action provider implementation that returns an empty list of actions.
 * Used as a fallback when no language-specific action provider is configured.
 */
export class DefaultActionProvider implements ActionProvider {
    /**
     * Returns an empty list of actions.
     *
     * @param _params The parameters (unused)
     * @returns An empty actions response
     */
    async getFileActions(_params: GetFileActionsParams): Promise<GetFileActionsResponse> {
        return { actions: [] };
    }
}
