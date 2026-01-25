import type { Doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstNode } from "langium";
import type { PrintContext, Builders, AstSerializerAdditionalServices } from "@mdeo/language-common";
import { ID, STRING } from "@mdeo/language-common";
import type { ImportType, FileImportType } from "./types.js";
import type { ASTType } from "@mdeo/language-common";

/**
 * Registers import serializers for pretty-printing import AST nodes.
 *
 * @template T The AstNode type being imported
 * @param services The Langium core services with AST serializer
 * @param builders Prettier doc builders
 * @param importType The import type interface
 * @param fileImportType The file import type interface
 */
export function registerImportSerializers<T extends AstNode>(
    services: LangiumCoreServices & AstSerializerAdditionalServices,
    builders: Builders,
    importType: ImportType<T>,
    fileImportType: FileImportType<T>
): void {
    const { AstSerializer } = services;
    AstSerializer.registerNodeSerializer(importType, (ctx) => printImport(ctx));
    AstSerializer.registerNodeSerializer(fileImportType, (ctx) => printFileImport(ctx, builders));
}

/**
 * Prints an import node (entity reference with optional alias).
 *
 * @param context The print context
 * @returns The formatted import
 */
export function printImport<T extends AstNode>(context: PrintContext<ASTType<ImportType<T>>>): Doc {
    const { ctx, path, printPrimitive, getPrimitive, printReference } = context;
    const docs: Doc[] = [path.call((ref) => printReference(ref, ID), "entity")];

    if (ctx.name != undefined) {
        docs.push(" as ");
        docs.push(printPrimitive(getPrimitive(ctx, "name"), ID));
    }

    return docs;
}

/**
 * Prints a file import node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted file import
 */
export function printFileImport<T extends AstNode>(
    context: PrintContext<ASTType<FileImportType<T>>>,
    builders: Builders
): Doc {
    const { ctx, path, print, printPrimitive, getPrimitive } = context;
    const { join, group, line, indent } = builders;
    const docs: Doc[] = ["import "];

    docs.push(group(["{", indent([line, join([",", line], path.map(print, "imports"))]), line, "} from "]));

    docs.push(printPrimitive(getPrimitive(ctx, "file"), STRING));

    return docs;
}
