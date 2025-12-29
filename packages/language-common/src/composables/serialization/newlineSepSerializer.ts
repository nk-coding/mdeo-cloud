import type { AstPath, Doc } from "prettier";
import type { Builders, PrintContext } from "./astSerializer.js";
import type { AstNode } from "langium";

/**
 * Prints a list of newline separated entries, preserving blank lines from the original source
 * to some extent.
 *
 * @param entries the entries to serialize
 * @param context the print context
 * @param builders the Prettier document builders
 * @returns the serialized entries
 */
export function serializeNewlineSep<T extends AstNode, P extends ArrayProperties<T>>(
    { path, document, print }: PrintContext<T>,
    sections: P[],
    builders: Builders
): Doc {
    let lastLine = Number.MAX_SAFE_INTEGER;

    const result = sections.flatMap((section) => {
        return path.map((entry) => {
            const node = entry.node as AstNode;
            const cstNode = node.$cstNode;
            const startLocation =
                cstNode != undefined ? document.textDocument.positionAt(cstNode.offset).line : undefined;
            const docs: Doc[] = [];
            if (startLocation != undefined && startLocation > lastLine + 1) {
                docs.push(builders.hardline);
            }
            docs.push(print(entry));
            const endLocation = cstNode != undefined ? document.textDocument.positionAt(cstNode.end).line : undefined;
            docs.push(builders.hardline);
            lastLine = endLocation ?? Number.MAX_SAFE_INTEGER;
            return docs;
        }, section as any);
    });
    result.at(-1)?.pop();
    return result;
}

/**
 * Utility type to get AstNode array properties
 */
type ArrayProperties<T> = {
    [K in keyof T]: NonNullable<T[K]> extends readonly AstNode[] ? K : never;
}[keyof T];
