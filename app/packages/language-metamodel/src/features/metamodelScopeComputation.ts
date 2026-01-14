import { sharedImport } from "@mdeo/language-shared";
import type { LangiumDocument, AstNodeDescription, AstNode } from "langium";
import type { CancellationToken } from "vscode-jsonrpc";
import type { MetaModelType } from "../grammar/metamodelTypes.js";

const { DefaultScopeComputation } = sharedImport("langium");

/**
 * The scope computation for the Metamodel language
 * Overrides the default export symbol collection to include imported classes.
 */
export class MetamodelScopeComputation extends DefaultScopeComputation {
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
                for (const cls of metamodelRoot.classesAndAssociations) {
                    result.push(cls);
                }
                for (const imp of metamodelRoot.imports) {
                    result.push(...imp.imports);
                }
                return result;
            },
            cancelToken
        );
    }
}
