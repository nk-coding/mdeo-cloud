import type { ActionHandlerRegistry } from "./actionHandlerRegistry.js";
import type { ActionProvider } from "./actionProvider.js";

/**
 * Additional services for action handler registry support.
 * Extends Langium's language-specific services with action handling capabilities.
 */
export interface ActionHandlerRegistryAdditionalServices {
    /**
     * Action services namespace
     */
    action: {
        /**
         * The action handler registry for managing action handlers
         */
        ActionHandlerRegistry: ActionHandlerRegistry;
        /**
         * The action provider for getting available file actions
         */
        ActionProvider: ActionProvider;
    };
}
