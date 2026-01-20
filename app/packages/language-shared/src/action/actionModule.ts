import type { ActionHandler } from "./actionHandler.js";

/**
 * Additional services for action dialog support.
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
    };
}
