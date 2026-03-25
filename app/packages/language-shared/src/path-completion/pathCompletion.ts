import type { LangiumDocument } from "langium";
import type { CompletionAcceptor, CompletionContext } from "langium/lsp";
import type { Range } from "vscode-languageserver-types";
import { calculateRelativePath } from "../util/pathUtils.js";
import { sharedImport } from "../sharedImport.js";

const { CompletionItemKind } = sharedImport("vscode-languageserver-types");

/**
 * Computes relative path completion candidates from the given document to all
 * workspace documents with matching file extensions.
 *
 * @param fromDocument The document currently being edited
 * @param documents The workspace LangiumDocuments registry
 * @param extensions File extensions to filter by (e.g., [".mm", ".script"]). If empty, all files are returned.
 * @returns Array of relative path strings (without quotes) to suggest
 */
export function computeRelativePathCompletions(
    fromDocument: LangiumDocument,
    documents: { all: { toArray(): LangiumDocument[] } },
    extensions: string[]
): string[] {
    const fromPath = fromDocument.uri.path;
    const suggestions: string[] = [];

    for (const doc of documents.all.toArray()) {
        const toPath = doc.uri.path;
        if (toPath === fromPath) {
            continue;
        }
        if (extensions.length > 0 && !extensions.some((ext) => toPath.endsWith(ext))) {
            continue;
        }
        const relativePath = calculateRelativePath(fromPath, toPath);
        suggestions.push(relativePath);
    }

    return suggestions;
}

/**
 * Accepts relative path completion items into the given acceptor using a
 * Monaco-compatible `textEdit` approach.
 *
 * @param context The current LSP completion context
 * @param acceptor The function used to register each completion item
 * @param paths Relative path strings to suggest (without surrounding quotes)
 */
export function acceptRelativePathCompletions(
    context: CompletionContext,
    acceptor: CompletionAcceptor,
    paths: string[]
): void {
    const existingText = context.textDocument.getText().substring(context.tokenOffset, context.offset);
    let filteredPaths = paths;
    let range: Range = { start: context.position, end: context.position };

    if (existingText.length > 0) {
        const existingPath = existingText.substring(1);
        filteredPaths = paths.filter((path) => path.startsWith(existingPath));
        const start = context.textDocument.positionAt(context.tokenOffset + 1);
        const end = context.textDocument.positionAt(context.tokenEndOffset - 1);
        range = { start, end };
    }

    for (const path of filteredPaths) {
        const delimiter = existingText.length > 0 ? "" : '"';
        acceptor(context, {
            label: path,
            textEdit: {
                newText: `${delimiter}${path}${delimiter}`,
                range
            },
            kind: CompletionItemKind.File,
            sortText: "2"
        });
    }
}
