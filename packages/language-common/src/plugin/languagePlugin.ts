import type { Module } from "langium";
import type { TerminalRule } from "../grammar/rule/terminal/types.js";
import type { ParserRule } from "../grammar/rule/types.js";
import type { PluginContext } from "./pluginContext.js";
import type { DefaultSharedModuleContext, LangiumServices, PartialLangiumServices } from "langium/lsp";

/**
 * Provider for a language plugin, which provides support for one language for a langium-based language server.
 *
 * @template T The type of the language's additional services
 */
export interface LanguagePluginProvider<T> {
    /**
     * Generates the language plugin using the provided context.
     *
     * @param context context with shared dependencies
     * @returns The language plugin instance
     */
    generate: (context: PluginContext) => LanguagePlugin<T>;
}

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
    module: Module<LangiumServices & T, PartialLangiumServices & T>;
    /**
     * Optional callback that is invoked after the language services have been created.
     *
     * @param services the created language services
     * @param context module context with the LSP connection
     */
    postCreate?: (services: LangiumServices & T, context: DefaultSharedModuleContext) => void;
}

/**
 * Type for a function which when provided a PluginContext returns a service provider function which can be used for module definitions.
 *
 * @template T The type of the language's additional services
 * @template V The type of the provided service
 */
export type ServiceProvider<T, V> = (context: PluginContext) => (services: LangiumServices & T) => V;
