import type { Module } from "langium";
import type { TerminalRule } from "../grammar/rule/terminal/types.js";
import type { ParserRule } from "../grammar/rule/types.js";
import type { PluginContext } from "./pluginContext.js";
import type { DefaultSharedModuleContext } from "langium/lsp";
import type { ExtendedLangiumServices, PartialExtendedLangiumServices } from "../grammar/module/extendedServices.js";

/**
 * Language plugin, which provides support for one language for a langium-based language server.
 *
 * @template T The type of the language's additional services
 */
export interface LanguagePlugin<T> {
    /**
     * The root parser rule that serves as the entry point for parsing
     */
    rootRule: ParserRule<any>;
    /**
     * Array of terminal rules that should be included in the grammar
     */
    additionalTerminals: TerminalRule<any>[];
    /**
     * The module for the language
     */
    module: Module<ExtendedLangiumServices & T, PartialExtendedLangiumServices & T>;
    /**
     * Optional callback that is invoked after the language services have been created.
     *
     * @param services the created language services
     * @param context module context with the LSP connection
     */
    postCreate?: (services: ExtendedLangiumServices & T, context: DefaultSharedModuleContext) => void;
}

/**
 * Type for a function which when provided a PluginContext returns a service provider function which can be used for module definitions.
 *
 * @template T The type of the language's additional services
 * @template V The type of the provided service
 */
export type ServiceProvider<T, V> = (context: PluginContext) => (services: ExtendedLangiumServices & T) => V;
