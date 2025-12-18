import type { AstNode } from "langium";
import type { TerminalRule } from "../../grammar/rule/terminal/types.js";
import { createRule, many, optional, ref } from "../../grammar/rule/parser/factory.js";
import { ID, STRING } from "../../language/defaultTokens.js";
import type { FileImportType, ImportType } from "./types.js";
import type { FileScopingConfig } from "./config.js";

/**
 * Generates grammar rules for import statements in a language.
 * Creates two rules: one for individual entity imports and one for file imports.
 *
 * The generated grammar follows the pattern:
 * - Import: `entityRef [as name]`
 * - FileImport: `import { import1, import2, ... } from "file"`
 *
 * @template T The AstNode type being imported
 * @param config Configuration for file-scoped composition
 * @param importType The interface type for a single import (typically from generateImportTypes)
 * @param fileImportType The interface type for a file import statement (typically from generateImportTypes)
 * @param terminal Optional terminal rule to use for entity references (if not provided, uses default cross-reference)
 * @returns An object containing the importRule and fileImportRule
 */
export function generateImportRules<T extends AstNode>(
    config: FileScopingConfig<T>,
    importType: ImportType<T>,
    fileImportType: FileImportType<T>,
    terminal?: TerminalRule<any>
) {
    const importRule = createRule(config.importRuleName)
        .returns(importType)
        .as(({ set }) => [set("entity", ref(config.type, terminal)), optional("as", set("name", ID))]);
    const fileImportRule = createRule(config.fileImportRuleName)
        .returns(fileImportType)
        .as(({ set, add }) => [
            "import",
            "{",
            add("imports", importRule),
            many(",", add("imports", importRule)),
            "}",
            "from",
            set("file", STRING)
        ]);

    return { importRule, fileImportRule };
}
