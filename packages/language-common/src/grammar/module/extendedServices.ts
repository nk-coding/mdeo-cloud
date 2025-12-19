import type { LangiumServices, LangiumSharedServices } from "langium/lsp";
import type { AstReflection } from "./module.js";
import type { DeepPartial } from "langium";

/**
 * Langium services with custom extensions
 */
export type ExtendedLangiumServices = LangiumServices & {
    shared: ExtendedLangiumSharedServices
}

/** 
 * Partial Langium services with custom extensions
 */
export type PartialExtendedLangiumServices = DeepPartial<ExtendedLangiumServices>

/**
 * Langium shared services with custom extensions
 */
export type ExtendedLangiumSharedServices = LangiumSharedServices & {
    AstReflection: AstReflection
}