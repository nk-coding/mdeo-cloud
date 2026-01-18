import type { LangiumDocument } from "langium";

/**
 * Checks if the given document has any errors (lexer, parser, or diagnostics).
 *
 * @param document The Langium document to check
 * @returns True if the document has errors, false otherwise
 */
export function hasErrors(document: LangiumDocument): boolean {
    return (
        document.parseResult.lexerErrors.length > 0 ||
        document.parseResult.parserErrors.length > 0 ||
        (document.diagnostics != undefined && document.diagnostics.length > 0)
    );
}
