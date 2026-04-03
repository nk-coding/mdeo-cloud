import type { TypirLangiumSpecifics } from "typir-langium";
import type { ScopeProvider } from "../typir-extensions/scope/scopeProvider.js";
import { isCustomValueType } from "../typir-extensions/kinds/custom-value/custom-value-type.js";
import { isCustomFunctionType } from "../typir-extensions/kinds/custom-function/custom-function-type.js";
import type { ExtendedTypirServices } from "../typir-extensions/service/extendedTypirServices.js";
import { sharedImport } from "@mdeo/language-shared";
import type { HoverProvider } from "langium/lsp";
import type { Hover, HoverParams } from "vscode-languageserver-protocol";
import type { MaybePromise, AstNode, LangiumDocument } from "langium";
import type { ExpressionTypirServices } from "../type-system/services.js";
import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import type { ExpressionTypes } from "../grammar/expressionTypes.js";
import { memberValueTypeToString, memberMethodDetailString } from "./expressionTypeRendering.js";
import type { TypeTypes } from "../grammar/typeTypes.js";

const { CstUtils, AstUtils, GrammarAST } = sharedImport("langium");

/**
 * Hover provider for expression-level constructs backed by Typir's scope and type system.
 *
 * Handles hover for three categories of tokens:
 * - **IdentifierExpression** — a reference to a variable or function; the type (or full function
 *   signature) is resolved via the Typir scope provider.
 * - **MemberAccess / MemberCallExpression** — a property or method accessed on an owner expression;
 *   the owner type is inferred and the matching member is looked up.
 * - **Declaration name tokens** — the `name` property of any declaration node (variable,
 *   function, parameter, for-loop variable, etc.); the type of the declaration node is inferred
 *   directly via Typir type inference.
 *
 * The displayed text mirrors the detail strings shown in the completion provider.
 */
export class ExpressionHoverProvider implements HoverProvider {
    private readonly typirScopeProvider: ScopeProvider<TypirLangiumSpecifics>;
    private readonly typirInference: ExtendedTypirServices<TypirLangiumSpecifics>["Inference"];
    protected readonly reflection: AstReflection;

    /**
     * Creates a new ExpressionHoverProvider.
     *
     * @param services The combined Langium and Typir services.
     * @param expressionTypes Grammar-derived AST type descriptors used to identify expression node types.
     * @param typeTypes Grammar-derived AST type descriptors used for type nodes (for rendering).
     */
    constructor(
        services: {
            typir: ExpressionTypirServices<TypirLangiumSpecifics>;
        } & ExtendedLangiumServices,
        protected readonly expressionTypes: ExpressionTypes,
        protected readonly typeTypes: TypeTypes
    ) {
        this.typirScopeProvider = services.typir.ScopeProvider;
        this.typirInference = services.typir.Inference;
        this.reflection = services.shared.AstReflection;
    }

    /**
     * Returns hover content for the position described by `params`, or `undefined` when no
     * relevant information is available.
     *
     * @param document The Langium document in which the hover was triggered.
     * @param params The LSP hover parameters containing the cursor position.
     * @returns A {@link Hover} object with markdown content, or `undefined`.
     */
    getHoverContent(document: LangiumDocument, params: HoverParams): MaybePromise<Hover | undefined> {
        const rootCstNode = document.parseResult?.value?.$cstNode;
        if (!rootCstNode) {
            return undefined;
        }

        const offset = document.textDocument.offsetAt(params.position);
        const cstNode = CstUtils.findDeclarationNodeAtOffset(rootCstNode, offset);
        if (!cstNode) {
            return undefined;
        }

        const astNode = cstNode.astNode;

        if (this.reflection.isInstance(astNode, this.expressionTypes.identifierExpressionType)) {
            if (cstNode.text === astNode.name) {
                return this.hoverForIdentifier(astNode, astNode.name);
            }
        }

        if (this.reflection.isInstance(astNode, this.expressionTypes.memberAccessExpressionType)) {
            if (cstNode.text === astNode.member && astNode.expression != undefined) {
                return this.hoverForMember(astNode.expression, astNode.member);
            }
        }

        if (this.reflection.isInstance(astNode, this.expressionTypes.memberCallExpressionType)) {
            if (cstNode.text === astNode.member && astNode.expression != undefined) {
                return this.hoverForMember(astNode.expression, astNode.member);
            }
        }

        const assignment = AstUtils.getContainerOfType(cstNode.grammarSource, GrammarAST.isAssignment);
        if (assignment?.feature === "name" && !this.reflection.isInstance(astNode, this.typeTypes.baseTypeType)) {
            return this.hoverForDeclaration(astNode, cstNode.text);
        }

        return undefined;
    }

    /**
     * Produces hover content for an identifier expression by looking it up in the Typir scope.
     *
     * Displays the inferred type as `name: TypeName` for variables/parameters, or the full
     * function signature string for functions.
     *
     * @param node The identifier expression AST node used as the scope anchor.
     * @param name The identifier name text as written in the source.
     * @returns A {@link Hover} with a markdown code block, or `undefined` when no type is found.
     */
    private hoverForIdentifier(node: AstNode, name: string): Hover | undefined {
        try {
            const boundScope = this.typirScopeProvider.getScope(node);
            const entry = boundScope.getEntry(name);
            if (!entry) {
                return undefined;
            }

            const inferredType = entry.inferType();
            if (Array.isArray(inferredType)) {
                return undefined;
            }

            const label = isCustomFunctionType(inferredType)
                ? inferredType.getName()
                : `${name}: ${inferredType.getName()}`;
            return makeCodeHover(label);
        } catch {
            return undefined;
        }
    }

    /**
     * Produces hover content for a member access or member call by inferring the type of
     * the owner expression and locating the named member.
     *
     * Displays `memberName: TypeName` for properties, or `fun memberName(params): ReturnType`
     * for methods.
     *
     * @param ownerNode The AST node representing the owner expression (the part before `.`).
     * @param memberName The member name as written in the source.
     * @returns A {@link Hover} with a markdown code block, or `undefined` when the member is not found.
     */
    private hoverForMember(ownerNode: AstNode, memberName: string): Hover | undefined {
        try {
            const ownerType = this.typirInference.inferType(ownerNode);
            if (Array.isArray(ownerType) || !isCustomValueType(ownerType)) {
                return undefined;
            }

            const typeArgs = ownerType.details.typeArgs;

            for (const prop of ownerType.getAllProperties()) {
                if (prop.name === memberName) {
                    const typeStr = memberValueTypeToString(prop.type, typeArgs);
                    return makeCodeHover(`${memberName}: ${typeStr}`);
                }
            }

            for (const method of ownerType.getAllMethods()) {
                if (method.name === memberName) {
                    const detail = memberMethodDetailString(method, typeArgs);
                    return makeCodeHover(`fun ${memberName}${detail}`);
                }
            }
        } catch {
            return undefined;
        }
        return undefined;
    }

    /**
     * Produces hover content for the `name` token of a declaration node by inferring the
     * node's type from Typir.
     *
     * Works for all declaration forms that assign to a `name` property: variables, functions,
     * parameters, for-loop variables, and any language-specific declarations.
     *
     * @param node The declaration AST node whose `name` token is hovered.
     * @param name The name text as written in the source.
     * @returns A {@link Hover} with a markdown code block, or `undefined` when type inference fails.
     */
    private hoverForDeclaration(node: AstNode, name: string): Hover | undefined {
        try {
            const inferredType = this.typirInference.inferType(node);
            if (Array.isArray(inferredType)) {
                return undefined;
            }

            const label = isCustomFunctionType(inferredType)
                ? inferredType.getName()
                : `${name}: ${inferredType.getName()}`;
            return makeCodeHover(label);
        } catch {
            return undefined;
        }
    }
}

/**
 * Wraps a display label in a markdown fenced code block hover response.
 *
 * @param label The text to display inside the code block.
 * @returns A {@link Hover} object with markdown content.
 */
function makeCodeHover(label: string): Hover {
    return { contents: { kind: "markdown", value: `\`\`\`\n${label}\n\`\`\`` } };
}
