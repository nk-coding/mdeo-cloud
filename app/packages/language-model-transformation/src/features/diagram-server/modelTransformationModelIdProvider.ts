import { AstReflectionKey, BaseModelIdProvider, sharedImport } from "@mdeo/language-shared";
import type { AstNode, Reference } from "langium";
import {
    ModelTransformation,
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    IfExpressionStatement,
    WhileExpressionStatement,
    StopStatement,
    Pattern,
    PatternObjectInstance,
    PatternLink,
    PatternLinkEnd,
    PatternPropertyAssignment,
    WhereClause,
    PatternVariable,
    type PatternObjectInstanceType,
    type PatternLinkType,
    type PatternLinkEndType,
    type PatternPropertyAssignmentType,
    type WhereClauseType,
    type PatternVariableType,
    type MatchStatementType,
    type IfMatchStatementType,
    type WhileMatchStatementType,
    type UntilMatchStatementType,
    type ForMatchStatementType,
    type IfExpressionStatementType,
    type WhileExpressionStatementType,
    type StopStatementType,
    type PatternType
} from "../../grammar/modelTransformationTypes.js";
import type { AstReflection } from "@mdeo/language-common";

const { injectable, inject } = sharedImport("inversify");

/**
 * Provides unique IDs for model transformation AST nodes based on semantic information.
 * IDs are constructed to be deterministic and meaningful.
 */
@injectable()
export class ModelTransformationModelIdProvider extends BaseModelIdProvider {
    /**
     * Injected AST reflection service for type checking and model introspection.
     */
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;

    /**
     * Counter for generating unique statement indices.
     */
    private statementIndex = 0;

    /**
     * Counter for generating unique pattern element indices within a pattern.
     */
    private patternElementIndex = 0;

    /**
     * Gets the name/ID for an AST node.
     *
     * @param node The AST node
     * @returns The generated name/ID or undefined
     */
    getName(node: AstNode): string | undefined {
        // Root transformation
        if (this.reflection.isInstance(node, ModelTransformation)) {
            return this.getTransformationName();
        }

        // Statements
        if (this.reflection.isInstance(node, MatchStatement)) {
            return this.getMatchStatementName(node);
        }
        if (this.reflection.isInstance(node, IfMatchStatement)) {
            return this.getIfMatchStatementName(node);
        }
        if (this.reflection.isInstance(node, WhileMatchStatement)) {
            return this.getWhileMatchStatementName(node);
        }
        if (this.reflection.isInstance(node, UntilMatchStatement)) {
            return this.getUntilMatchStatementName(node);
        }
        if (this.reflection.isInstance(node, ForMatchStatement)) {
            return this.getForMatchStatementName(node);
        }
        if (this.reflection.isInstance(node, IfExpressionStatement)) {
            return this.getIfExpressionStatementName(node);
        }
        if (this.reflection.isInstance(node, WhileExpressionStatement)) {
            return this.getWhileExpressionStatementName(node);
        }
        if (this.reflection.isInstance(node, StopStatement)) {
            return this.getStopStatementName(node);
        }

        // Pattern
        if (this.reflection.isInstance(node, Pattern)) {
            return this.getPatternName(node);
        }

        // Pattern elements
        if (this.reflection.isInstance(node, PatternObjectInstance)) {
            return this.getPatternObjectInstanceName(node);
        }
        if (this.reflection.isInstance(node, PatternLink)) {
            return this.getPatternLinkName(node);
        }
        if (this.reflection.isInstance(node, PatternLinkEnd)) {
            return this.getPatternLinkEndName(node);
        }
        if (this.reflection.isInstance(node, PatternPropertyAssignment)) {
            return this.getPatternPropertyAssignmentName(node);
        }
        if (this.reflection.isInstance(node, WhereClause)) {
            return this.getWhereClauseName(node);
        }
        if (this.reflection.isInstance(node, PatternVariable)) {
            return this.getPatternVariableName(node);
        }

        return undefined;
    }

    /**
     * Generates ID for ModelTransformation root node.
     *
     * @returns The transformation graph ID
     */
    private getTransformationName(): string {
        return "transformation-graph";
    }

    /**
     * Gets the index of a statement within its container.
     *
     * @param node The statement node
     * @returns The index as a string
     */
    private getStatementIndex(node: AstNode): string {
        const container = node.$container;
        if (container && "$containerProperty" in node) {
            const prop = node.$containerProperty as string;
            const containerValue = (container as unknown as Record<string, unknown>)[prop];
            if (Array.isArray(containerValue)) {
                const index = containerValue.indexOf(node);
                if (index >= 0) {
                    return String(index);
                }
            }
        }
        return String(this.statementIndex++);
    }

    /**
     * Generates ID for MatchStatement.
     *
     * @param node The match statement
     * @returns The generated ID
     */
    private getMatchStatementName(node: MatchStatementType): string {
        const index = this.getStatementIndex(node);
        return `match_${index}`;
    }

    /**
     * Generates ID for IfMatchStatement.
     *
     * @param node The if-match statement
     * @returns The generated ID
     */
    private getIfMatchStatementName(node: IfMatchStatementType): string {
        const index = this.getStatementIndex(node);
        return `if_match_${index}`;
    }

    /**
     * Generates ID for WhileMatchStatement.
     *
     * @param node The while-match statement
     * @returns The generated ID
     */
    private getWhileMatchStatementName(node: WhileMatchStatementType): string {
        const index = this.getStatementIndex(node);
        return `while_match_${index}`;
    }

    /**
     * Generates ID for UntilMatchStatement.
     *
     * @param node The until-match statement
     * @returns The generated ID
     */
    private getUntilMatchStatementName(node: UntilMatchStatementType): string {
        const index = this.getStatementIndex(node);
        return `until_match_${index}`;
    }

    /**
     * Generates ID for ForMatchStatement.
     *
     * @param node The for-match statement
     * @returns The generated ID
     */
    private getForMatchStatementName(node: ForMatchStatementType): string {
        const index = this.getStatementIndex(node);
        return `for_match_${index}`;
    }

    /**
     * Generates ID for IfExpressionStatement.
     *
     * @param node The if-expression statement
     * @returns The generated ID
     */
    private getIfExpressionStatementName(node: IfExpressionStatementType): string {
        const index = this.getStatementIndex(node);
        return `if_expr_${index}`;
    }

    /**
     * Generates ID for WhileExpressionStatement.
     *
     * @param node The while-expression statement
     * @returns The generated ID
     */
    private getWhileExpressionStatementName(node: WhileExpressionStatementType): string {
        const index = this.getStatementIndex(node);
        return `while_expr_${index}`;
    }

    /**
     * Generates ID for StopStatement.
     *
     * @param node The stop statement
     * @returns The generated ID
     */
    private getStopStatementName(node: StopStatementType): string {
        const index = this.getStatementIndex(node);
        const keyword = node.keyword ?? "stop";
        return `${keyword}_${index}`;
    }

    /**
     * Generates ID for Pattern.
     *
     * @param node The pattern
     * @returns The generated ID
     */
    private getPatternName(node: PatternType): string {
        const container = node.$container;
        if (container) {
            const containerId = this.getName(container);
            if (containerId) {
                return `${containerId}_pattern`;
            }
        }
        return `pattern_${this.patternElementIndex++}`;
    }

    /**
     * Generates ID for PatternObjectInstance based on name and class.
     *
     * @param node The pattern object instance
     * @returns The generated ID
     */
    private getPatternObjectInstanceName(node: PatternObjectInstanceType): string {
        const name = node.name ?? "unnamed";
        const className = this.resolveClassName(node.class);
        return `${className}_${name}`;
    }

    /**
     * Generates ID for PatternLink based on source and target.
     *
     * @param node The pattern link
     * @returns The generated ID
     */
    private getPatternLinkName(node: PatternLinkType): string {
        const sourceEnd = this.formatPatternLinkEnd(node.source);
        const targetEnd = this.formatPatternLinkEnd(node.target);
        return `${sourceEnd}--${targetEnd}`;
    }

    /**
     * Formats a pattern link end for ID generation.
     *
     * @param linkEnd The pattern link end
     * @returns The formatted string
     */
    private formatPatternLinkEnd(linkEnd: PatternLinkEndType | undefined): string {
        if (linkEnd == undefined) {
            return "unresolved";
        }

        const objectRef = linkEnd.object;
        if (objectRef == undefined || objectRef.ref == undefined) {
            return "unresolved";
        }

        const obj = objectRef.ref as PatternObjectInstanceType;
        const objectName = obj.name ?? "unnamed";
        const property = linkEnd.property?.$refText ?? "";

        if (property) {
            return `${objectName}_${property}`;
        }
        return objectName;
    }

    /**
     * Generates ID for PatternLinkEnd.
     *
     * @param node The pattern link end
     * @returns The generated ID
     */
    private getPatternLinkEndName(node: PatternLinkEndType): string {
        const parent = node.$container;
        if (parent != undefined && this.reflection.isInstance(parent, PatternLink)) {
            const link = parent as PatternLinkType;
            const linkName = this.getPatternLinkName(link);
            const isSource = link.source === node;
            return `${linkName}_${isSource ? "source" : "target"}`;
        }
        return "linkEnd";
    }

    /**
     * Generates ID for PatternPropertyAssignment.
     *
     * @param node The pattern property assignment
     * @returns The generated ID
     */
    private getPatternPropertyAssignmentName(node: PatternPropertyAssignmentType): string {
        const propName = node.name?.$refText ?? node.name?.ref?.name ?? "unnamed";
        const parent = node.$container;

        if (parent != undefined && this.reflection.isInstance(parent, PatternObjectInstance)) {
            const parentObj = parent as PatternObjectInstanceType;
            const parentId = this.getPatternObjectInstanceName(parentObj);
            return `${parentId}_prop_${propName}`;
        }

        return propName;
    }

    /**
     * Generates ID for WhereClause.
     *
     * @param node The where clause
     * @returns The generated ID
     */
    private getWhereClauseName(node: WhereClauseType): string {
        const container = node.$container;
        if (container && "$containerProperty" in node) {
            const prop = node.$containerProperty as string;
            const containerValue = (container as unknown as Record<string, unknown>)[prop];
            if (Array.isArray(containerValue)) {
                const index = containerValue.indexOf(node);
                if (index >= 0) {
                    return `where_${index}`;
                }
            }
        }
        return `where_${this.patternElementIndex++}`;
    }

    /**
     * Generates ID for PatternVariable.
     *
     * @param node The pattern variable
     * @returns The generated ID
     */
    private getPatternVariableName(node: PatternVariableType): string {
        return `var_${node.name ?? "unnamed"}`;
    }

    /**
     * Resolves the class name from a class reference.
     *
     * @param classRef The class reference
     * @returns The resolved class name
     */
    private resolveClassName(classRef: Reference<AstNode> | undefined): string {
        if (classRef === undefined || classRef.error != undefined) {
            return "unresolved";
        }
        const resolved = classRef.ref;
        if (resolved && "name" in resolved && typeof resolved.name === "string") {
            return resolved.name;
        }
        return "unknown";
    }
}
