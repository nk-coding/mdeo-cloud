import type { AstNode } from "langium";
import { BaseRequestClipboardDataActionHandler, sharedImport, AstReflectionKey } from "@mdeo/language-shared";
import type { AstReflection } from "@mdeo/language-common";
import {
    PatternObjectInstance,
    PatternObjectInstanceReference,
    PatternObjectInstanceDelete,
    PatternLink,
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    Pattern,
    BaseTransformationStatement
} from "../../../grammar/modelTransformationTypes.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Model-transformation-specific action handler for {@code RequestClipboardDataAction}.
 *
 * <p>Two categories of AST nodes can be copied:
 * <ul>
 *   <li><b>Pattern elements</b> – {@link PatternObjectInstance}, {@link PatternObjectInstanceReference},
 *       {@link PatternObjectInstanceDelete}, and {@link PatternLink}.</li>
 *   <li><b>Match nodes</b> – Any statement that extends {@link BaseTransformationStatement}
 *       and owns a pattern ({@link MatchStatement}, {@link IfMatchStatement}, etc.).</li>
 * </ul>
 *
 * <p>Selected AST nodes are deduplicated: if a pattern element's parent match statement
 * is also selected, the element is omitted (it will be included inside the match node).
 */
@injectable()
export class ModelTransformationRequestClipboardDataActionHandler extends BaseRequestClipboardDataActionHandler {
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;

    protected override getTopLevelAstNodes(selectedAstNodes: AstNode[]): AstNode[] {
        const topLevel: AstNode[] = [];
        const selectedSet = new Set(selectedAstNodes);

        const selectedStatements = new Set<AstNode>();
        for (const node of selectedAstNodes) {
            if (this.isMatchStatement(node)) {
                selectedStatements.add(node);
            }
        }

        for (const node of selectedAstNodes) {
            if (this.isMatchStatement(node)) {
                topLevel.push(node);
            } else if (this.isPatternElement(node)) {
                if (!this.isParentStatementSelected(node, selectedStatements)) {
                    topLevel.push(node);
                }
            }
        }

        return topLevel;
    }

    /**
     * Checks whether the given node is a match-type statement (MatchStatement,
     * IfMatchStatement, WhileMatchStatement, UntilMatchStatement, ForMatchStatement).
     */
    private isMatchStatement(node: AstNode): boolean {
        return (
            this.reflection.isInstance(node, MatchStatement) ||
            this.reflection.isInstance(node, IfMatchStatement) ||
            this.reflection.isInstance(node, WhileMatchStatement) ||
            this.reflection.isInstance(node, UntilMatchStatement) ||
            this.reflection.isInstance(node, ForMatchStatement)
        );
    }

    /**
     * Checks whether the given node is a pattern element (PatternObjectInstance,
     * PatternObjectInstanceReference, PatternObjectInstanceDelete, or PatternLink).
     */
    private isPatternElement(node: AstNode): boolean {
        return (
            this.reflection.isInstance(node, PatternObjectInstance) ||
            this.reflection.isInstance(node, PatternObjectInstanceReference) ||
            this.reflection.isInstance(node, PatternObjectInstanceDelete) ||
            this.reflection.isInstance(node, PatternLink)
        );
    }

    /**
     * Checks whether any ancestor match statement of the given pattern element
     * is in the set of selected statements.
     */
    private isParentStatementSelected(node: AstNode, selectedStatements: Set<AstNode>): boolean {
        let current: AstNode | undefined = node.$container;
        while (current) {
            if (selectedStatements.has(current)) {
                return true;
            }
            current = current.$container;
        }
        return false;
    }
}
