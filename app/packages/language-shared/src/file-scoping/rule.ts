import type { AstNode } from "langium";
import type { TerminalRule } from "@mdeo/language-common";
import { createRule, many, optional, ref } from "@mdeo/language-common";
import { ID, NEWLINE, STRING } from "@mdeo/language-common";
import type { FileImportType, ImportType } from "./types.js";
import type { FileScopingConfig } from "./config.js";

/**
 * Generates grammar rules for import statements in a language.
 * Creates two rules: one for individual entity imports and one for file imports.
 *
 * The generated grammar follows the pattern:
 * - Import: `entityRef [as name]`
 * - FileImport: `import { import1, import2, ... } from "./file"`
 *
 * @template T The AstNode type being imported
 * @param config Configuration for file-scoped composition
 * @param importType The interface type for a single import (typically from generateImportTypes)
 * @param fileImportType The interface type for a file import statement (typically from generateImportTypes)
 * @param terminal Optional terminal rule to use for entity references (if not provided, uses default cross-reference)
 * @param idTerminal Optional terminal rule for identifiers (default: ID)
 * @param stringTerminal Optional terminal rule for strings (default: STRING)
 * @param newlineTerminal Optional terminal rule for newlines (default: NEWLINE)
 * @param newlineAwareMode Whether to make the rules newline-aware (default: true)
 * @returns An object containing the importRule and fileImportRule
 */
export function generateImportRules<T extends AstNode>(
    config: FileScopingConfig<T>,
    importType: ImportType<T>,
    fileImportType: FileImportType<T>,
    terminal: TerminalRule<any>,
    idTerminal: TerminalRule<any> = ID,
    stringTerminal: TerminalRule<any> = STRING,
    newlineTerminal: TerminalRule<any> = NEWLINE,
    newlineAwareMode: boolean = true
) {
    const newlineTokens = newlineAwareMode ? [many(newlineTerminal)] : [];
    const importRule = createRule(config.importRuleName)
        .returns(importType)
        .as(({ set }) => [set("entity", ref(config.type, terminal)), optional("as", set("name", idTerminal))]);
    const fileImportRule = createRule(config.fileImportRuleName)
        .returns(fileImportType)
        .as(({ set, add }) => [
            "import",
            "{",
            ...newlineTokens,
            add("imports", importRule),
            many(...newlineTokens, ",", ...newlineTokens, add("imports", importRule)),
            ...newlineTokens,
            "}",
            "from",
            set("file", stringTerminal)
        ]);

    return { importRule, fileImportRule };
}
