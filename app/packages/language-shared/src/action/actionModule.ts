import type { ActionHandler } from "./actionHandler.js";
import type { ActionProvider } from "./actionProvider.js";

/**
 * Additional services for action support.
 * Extends Langium's language-specific services with action handling capabilities.
 */
export interface ActionAdditionalServices {
    /**
     * Action services namespace
     */
    action: {
        /**
         * The action service for handling dialog flows
         */
        ActionHandler: ActionHandler;
        /**
         * The action provider for getting available file actions
         */
        ActionProvider: ActionProvider;
    };
}
