import type {
    AstNode,
    AstNodeDescription,
    AstReflection,
    LangiumDocument,
    ReferenceInfo,
    Scope,
    Stream
} from "langium";
import type { PluginContext } from "../../plugin/pluginContext.js";

/**
 * Creates a local scope for resolving references within the current document.
 *
 * This function constructs a hierarchical scope by traversing up the AST tree from the reference
 * location, collecting all local symbols (variables, parameters, etc.) that are visible at each level.
 * The resulting scope respects lexical scoping rules where inner scopes can shadow outer scopes.
 * Adapüted from Langium's DefaultScopeProvider implementation.
 *
 * @param context The plugin context containing Langium utilities
 * @param referenceInfo Information about the reference being resolved, including the container node and property
 * @param document The Langium document containing local symbol information
 * @param astReflection AST reflection service for type checking and subtype relationships
 * @param outerScope An optional outer scope to chain with the local scope
 * @returns A scope containing all visible local symbols at the reference location, filtered by type.
 *          Returns EMPTY_SCOPE if no local symbols are available in the document.
 */
export function createLocalScope(
    { langium }: PluginContext,
    referenceInfo: ReferenceInfo,
    document: LangiumDocument,
    astReflection: AstReflection,
    outerScope?: Scope
): Scope {
    const localSymbols = document.localSymbols;
    if (localSymbols == undefined) {
        return langium.EMPTY_SCOPE;
    }

    const referenceType = astReflection.getReferenceType(referenceInfo);
    let currentNode: AstNode | undefined = referenceInfo.container;
    const scopes: Stream<AstNodeDescription>[] = [];
    do {
        if (localSymbols.has(currentNode)) {
            scopes.push(
                localSymbols.getStream(currentNode).filter((desc) => astReflection.isSubtype(desc.type, referenceType))
            );
        }
        currentNode = currentNode.$container;
    } while (currentNode);

    if (scopes.length === 0) {
        return langium.EMPTY_SCOPE;
    }

    let result: Scope = outerScope ?? langium.EMPTY_SCOPE;

    for (let i = scopes.length - 1; i >= 0; i--) {
        result = new langium.StreamScope(scopes[i], result);
    }
    return result;
}
