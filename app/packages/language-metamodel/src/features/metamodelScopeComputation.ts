import { sharedImport } from "@mdeo/language-shared";
import type { LangiumDocument, AstNodeDescription, AstNode } from "langium";
import type { CancellationToken } from "vscode-jsonrpc";
import type { MetaModelType } from "../grammar/metamodelTypes.js";

const { DefaultScopeComputation } = sharedImport("langium");

/**
 * The scope computation for the Metamodel language.
 *
 * This scope computation exports only locally defined elements (classes, enums, and associations).
 * Import statements are not exported as symbols
 */
export class MetamodelScopeComputation extends DefaultScopeComputation {
    /**
     * Collects exported symbols from a metamodel document.
     * Only exports locally defined elements (classes, enums, and associations).
     *
     * @param document The Langium document to collect symbols from
     * @param cancelToken Optional cancellation token
     * @returns Promise resolving to an array of AST node descriptions for exported symbols
     */
    override collectExportedSymbols(
        document: LangiumDocument,
        cancelToken?: CancellationToken
    ): Promise<AstNodeDescription[]> {
        return this.collectExportedSymbolsForNode(
            document.parseResult.value,
            document,
            (root) => {
                const result: AstNode[] = [];
                const metamodelRoot = root as MetaModelType;
                for (const element of metamodelRoot.elements ?? []) {
                    result.push(element);
                }
                return result;
            },
            cancelToken
        );
    }
}
