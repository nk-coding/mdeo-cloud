import type {
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse
} from "@mdeo/language-common";

/**
 * Service interface for handling action dialogs.
 * Provides methods to start and submit action dialog forms.
 */
export interface ActionHandler {
    /**
     * Starts an action dialog flow.
     * Called when a trigger event occurs that should show an action dialog.
     *
     * @param params The parameters containing the trigger type, language ID, and trigger data
     * @returns A promise resolving to the first page of the action dialog
     */
    startAction(params: ActionStartParams): Promise<ActionStartResponse>;

    /**
     * Submits form data for the current action dialog page.
     * Returns validation errors, the next page, or completion.
     *
     * @param params The parameters containing the original start params and collected inputs
     * @returns A promise resolving to the submission response
     */
    submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse>;
}

/**
 * Default action service implementation that throws errors for all methods.
 * Used as a fallback when no language-specific action service is configured.
 */
export class DefaultActionHandler implements ActionHandler {
    /**
     * Throws an error indicating actions are not supported.
     *
     * @param _params The start parameters (unused)
     * @throws Error indicating actions are not supported for this language
     */
    async startAction(_params: ActionStartParams): Promise<ActionStartResponse> {
        throw new Error("Action dialogs are not supported for this language");
    }

    /**
     * Throws an error indicating actions are not supported.
     *
     * @param _params The submit parameters (unused)
     * @throws Error indicating actions are not supported for this language
     */
    async submitAction(_params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        throw new Error("Action dialogs are not supported for this language");
    }
}
