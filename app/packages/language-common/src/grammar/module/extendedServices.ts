import type { LangiumServices, LangiumSharedServices } from "langium/lsp";
import type { AstReflection } from "./module.js";
import type { DeepPartial, DocumentBuilder } from "langium";

/**
 * Langium DocumentBuilder extended with dependency-tracking capabilities.
 */
export interface ExtendedDocumentBuilder extends DocumentBuilder {
    getTransitiveDependencies(uri: string): string[];
}

/**
 * Langium services with custom extensions
 */
export type ExtendedLangiumServices = LangiumServices & {
    shared: ExtendedLangiumSharedServices;
};

/**
 * Partial Langium services with custom extensions
 */
export type PartialExtendedLangiumServices = DeepPartial<ExtendedLangiumServices>;

/**
 * Langium shared services with custom extensions
 */
export type ExtendedLangiumSharedServices = LangiumSharedServices & {
    AstReflection: AstReflection;
    workspace: {
        DocumentBuilder: ExtendedDocumentBuilder;
    };
};
