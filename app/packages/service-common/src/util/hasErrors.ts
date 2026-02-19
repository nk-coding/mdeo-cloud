import type { LangiumDocument } from "langium";

/**
 * Checks if the given document has any parser-level errors (lexer or parser errors only).
 * Deliberately excludes diagnostic-level errors (such as unresolved references) because
 * these are expected in contexts where cross-language references cannot be resolved.
 *
 * @param document The Langium document to check
 * @returns True if the document has lexer or parser errors, false otherwise
 */
export function hasParserErrors(document: LangiumDocument): boolean {
    return document.parseResult.lexerErrors.length > 0 || document.parseResult.parserErrors.length > 0;
}

/**
 * Checks if the given document has any errors (lexer, parser, or diagnostics).
 *
 * @param document The Langium document to check
 * @returns True if the document has errors, false otherwise
 */
export function hasErrors(document: LangiumDocument): boolean {
    return hasParserErrors(document) || (document.diagnostics != undefined && document.diagnostics.length > 0);
}
