import type { TypirLangiumSpecifics } from "typir-langium";
import type { ScopeProvider } from "../typir-extensions/scope/scopeProvider.js";
import type { CustomValueType } from "../typir-extensions/kinds/custom-value/custom-value-type.js";
import { isCustomValueType } from "../typir-extensions/kinds/custom-value/custom-value-type.js";
import { isCustomFunctionType } from "../typir-extensions/kinds/custom-function/custom-function-type.js";
import type { ExtendedTypirServices } from "../typir-extensions/service/extendedTypirServices.js";
import { sharedImport } from "@mdeo/language-shared";
import type { CompletionAcceptor, CompletionContext, CompletionProviderOptions } from "langium/lsp";
import type { CompletionItemKind as CompletionItemKindType } from "vscode-languageserver-protocol";
import type { MaybePromise } from "langium";
import type { ExpressionTypirServices } from "../type-system/services.js";
import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import type { ExpressionTypes } from "../grammar/expressionTypes.js";

const { DefaultCompletionProvider } = sharedImport("langium/lsp");
const { CompletionItemKind } = sharedImport("vscode-languageserver-protocol");

/**
 * Completion provider that extends Langium's default completion provider to support
 * completion for expression-level constructs that use Typir's scope system instead
 * of Langium's cross-reference scoping.
 *
 * Handles:
 * - IdentifierExpression names (variables, functions, etc. from Typir scopes)
 * - MemberAccessExpression / MemberCallExpression members (properties/methods from inferred types)
 */
export class ExpressionCompletionProvider extends DefaultCompletionProvider {
    private readonly typirScopeProvider: ScopeProvider<TypirLangiumSpecifics>;
    private readonly typirInference: ExtendedTypirServices<TypirLangiumSpecifics>["Inference"];
    private readonly reflection: AstReflection;
    override readonly completionOptions: CompletionProviderOptions;

    /**
     * Creates a new ExpressionCompletionProvider.
     *
     * @param services The combined Langium and Typir services
     * @param expressionTypes Grammar-derived AST type descriptors used to identify expression node types
     */
    constructor(
        services: {
            typir: ExpressionTypirServices<TypirLangiumSpecifics>;
        } & ExtendedLangiumServices,
        private readonly expressionTypes: ExpressionTypes
    ) {
        super(services);
        this.typirScopeProvider = services.typir.ScopeProvider;
        this.typirInference = services.typir.Inference;
        this.reflection = services.shared.AstReflection;
        this.completionOptions = {
            triggerCharacters: ["."]
        };
    }

    protected override completionFor(
        context: CompletionContext,
        next: { feature: any; type?: string; property?: string },
        acceptor: CompletionAcceptor
    ): MaybePromise<void> {
        if (next.type === this.expressionTypes.identifierExpressionType.name) {
            return this.completionForIdentifierExpression(context, acceptor);
        }
        if (
            next.type === this.expressionTypes.memberAccessExpressionType.name ||
            next.type === this.expressionTypes.memberCallExpressionType.name
        ) {
            return this.completionForMemberAccess(context, acceptor);
        }
        return super.completionFor(context, next, acceptor);
    }

    /**
     * Provides completion items for identifier expressions by querying the Typir scope provider.
     *
     * @param context The current completion context
     * @param acceptor The acceptor function to register completion items
     * @returns `void`
     */
    private completionForIdentifierExpression(context: CompletionContext, acceptor: CompletionAcceptor): void {
        const node = context.node;
        if (node == undefined) {
            return;
        }

        try {
            const boundScope = this.typirScopeProvider.getScope(node);
            const entries = boundScope.getEntries();
            for (const entry of entries) {
                let typeName: string | undefined;
                let kind: CompletionItemKindType = CompletionItemKind.Variable;
                try {
                    const inferredType = entry.inferType();
                    if (!Array.isArray(inferredType)) {
                        typeName = inferredType.getName();
                        if (isCustomFunctionType(inferredType)) {
                            kind = CompletionItemKind.Function;
                        }
                    }
                } catch {
                    // Type inference may fail for some entries
                }

                acceptor(context, {
                    label: entry.name,
                    kind,
                    detail: typeName,
                    sortText: "0"
                });
            }
        } catch {
            // Scope resolution may fail on incomplete documents during editing
        }
    }

    /**
     * Provides completion items for member access expressions by inferring the type
     * of the owner expression and listing its properties and methods.
     *
     * The context node may already be the member access/call expression itself (when
     * the AST was partially built) or may be the owner expression. We detect the
     * former by checking both the node's $type and whether the cursor is within the
     * node's source range, and in that case we use the underlying `expression` field
     * (the owner) for type inference. Checking only the type is not sufficient
     * because the same $type can appear in other positions.
     *
     * @param context The current completion context
     * @param acceptor The acceptor function to register completion items
     * @returns `void`
     */
    private completionForMemberAccess(context: CompletionContext, acceptor: CompletionAcceptor): void {
        const node = context.node;
        if (node == undefined) {
            return;
        }

        try {
            let ownerNode = node;

            if (
                this.reflection.isInstance(node, this.expressionTypes.memberAccessExpressionType) ||
                this.reflection.isInstance(node, this.expressionTypes.memberCallExpressionType)
            ) {
                const cst = node.$cstNode;
                const isInsideRange = cst != undefined && cst.offset <= context.offset && context.offset <= cst.end;
                if (isInsideRange) {
                    const underlying = node.expression;
                    if (underlying == undefined) {
                        return;
                    }
                    ownerNode = underlying;
                }
            }

            const ownerType = this.typirInference.inferType(ownerNode);
            if (Array.isArray(ownerType) || !isCustomValueType(ownerType)) {
                return;
            }

            this.addMemberCompletions(ownerType, context, acceptor);
        } catch {
            // Type inference may fail on incomplete documents during editing
        }
    }

    /**
     * Adds completion items for all properties and methods of a type (including inherited ones).
     *
     * @param valueType The resolved owner type whose members are enumerated
     * @param context The current completion context
     * @param acceptor The acceptor function to register completion items
     * @returns `void`
     */
    private addMemberCompletions(
        valueType: CustomValueType,
        context: CompletionContext,
        acceptor: CompletionAcceptor
    ): void {
        for (const prop of valueType.getAllProperties()) {
            acceptor(context, {
                label: prop.name,
                kind: CompletionItemKind.Property,
                sortText: "0"
            });
        }
        for (const method of valueType.getAllMethods()) {
            acceptor(context, {
                label: method.name,
                kind: CompletionItemKind.Method,
                sortText: "0"
            });
        }
    }
}
