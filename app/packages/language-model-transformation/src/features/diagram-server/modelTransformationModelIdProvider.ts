import { AstReflectionKey, BaseModelIdProvider, sharedImport, type ModelIdRegistry } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import {
    ModelTransformation,
    BaseTransformationStatement,
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
    PatternObjectInstanceReference,
    PatternObjectInstanceDelete,
    PatternLink,
    PatternLinkEnd,
    PatternPropertyAssignment,
    WhereClause,
    PatternVariable,
    type PatternObjectInstanceType,
    type PatternObjectInstanceReferenceType,
    type PatternObjectInstanceDeleteType,
    type PatternLinkType,
    type PatternLinkEndType,
    type PatternPropertyAssignmentType,
    type WhereClauseType,
    type PatternVariableType,
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
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;

    getName(node: AstNode, registry: ModelIdRegistry): string | undefined {
        if (this.reflection.isInstance(node, ModelTransformation)) {
            return "transformation-graph";
        }

        if (
            this.reflection.isInstance(node, MatchStatement) ||
            this.reflection.isInstance(node, IfMatchStatement) ||
            this.reflection.isInstance(node, WhileMatchStatement) ||
            this.reflection.isInstance(node, UntilMatchStatement) ||
            this.reflection.isInstance(node, ForMatchStatement) ||
            this.reflection.isInstance(node, IfExpressionStatement) ||
            this.reflection.isInstance(node, WhileExpressionStatement) ||
            this.reflection.isInstance(node, StopStatement)
        ) {
            return this.getHierarchicalStatementName(node, registry);
        }

        if (this.reflection.isInstance(node, Pattern)) {
            return this.getPatternName(node, registry);
        }

        if (this.reflection.isInstance(node, PatternObjectInstance)) {
            return node.name ?? "unnamed";
        }
        if (this.reflection.isInstance(node, PatternObjectInstanceReference)) {
            return this.getPatternObjectInstanceReferenceName(node, registry);
        }
        if (this.reflection.isInstance(node, PatternObjectInstanceDelete)) {
            return this.getPatternObjectInstanceDeleteName(node, registry);
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

    private findParentStatement(container: AstNode): AstNode | undefined {
        if (this.reflection.isInstance(container, BaseTransformationStatement)) {
            return container;
        }
        if (this.reflection.isInstance(container, ModelTransformation)) {
            return container;
        }
        const parentContainer = (container as AstNode).$container;
        if (parentContainer != null) {
            return this.findParentStatement(parentContainer);
        }
        return undefined;
    }

    private getHierarchicalStatementName(node: AstNode, registry: ModelIdRegistry): string {
        const container = node.$container;
        if (!container || !("$containerProperty" in node)) {
            return "0";
        }
        const prop = node.$containerProperty as string;
        const containerValue = (container as unknown as Record<string, unknown>)[prop];
        let index = 0;
        if (Array.isArray(containerValue)) {
            const idx = containerValue.indexOf(node);
            if (idx >= 0) {
                index = idx;
            }
        }

        const parent = this.findParentStatement(container);
        if (parent == null || this.reflection.isInstance(parent, ModelTransformation)) {
            return String(index);
        }

        const parentName = registry.getName(parent);
        if (parentName !== undefined) {
            return `${parentName}_${index}`;
        }
        return String(index);
    }

    private getPatternName(node: PatternType, registry: ModelIdRegistry): string {
        const container = node.$container;
        if (container) {
            const parent = this.findParentStatement(container);
            if (parent != null && this.reflection.isInstance(parent, BaseTransformationStatement)) {
                return registry.getName(parent) ?? "pattern";
            }
        }
        return "pattern";
    }

    private getPatternObjectInstanceReferenceName(
        node: PatternObjectInstanceReferenceType,
        registry: ModelIdRegistry
    ): string {
        const instanceRefName = node.instance.ref?.name ?? node.instance.$refText ?? "unnamed";
        const container = node.$container;
        if (container) {
            const parent = this.findParentStatement(container);
            if (parent != null && !this.reflection.isInstance(parent, ModelTransformation)) {
                const parentName = registry.getName(parent);
                if (parentName !== undefined) {
                    return `${parentName}_ref_${instanceRefName}`;
                }
            }
        }
        return `ref_${instanceRefName}`;
    }

    private getPatternObjectInstanceDeleteName(
        node: PatternObjectInstanceDeleteType,
        registry: ModelIdRegistry
    ): string {
        const instanceRefName = node.instance.ref?.name ?? node.instance.$refText ?? "unnamed";
        const container = node.$container;
        if (container) {
            const parent = this.findParentStatement(container);
            if (parent != null && !this.reflection.isInstance(parent, ModelTransformation)) {
                const parentName = registry.getName(parent);
                if (parentName !== undefined) {
                    return `${parentName}_ref_${instanceRefName}`;
                }
            }
        }
        return `ref_${instanceRefName}`;
    }

    private getPatternLinkName(node: PatternLinkType): string {
        const sourceEnd = this.formatPatternLinkEnd(node.source);
        const targetEnd = this.formatPatternLinkEnd(node.target);
        return `${sourceEnd}--${targetEnd}`;
    }

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

    private getPatternLinkEndName(node: PatternLinkEndType): string {
        const parent = node.$container;
        if (parent != undefined && this.reflection.isInstance(parent, PatternLink)) {
            const link = parent as PatternLinkType;
            const linkName = this.getPatternLinkName(link);
            const isSource = link.source === node;
            return `${linkName}@${isSource ? "source" : "target"}`;
        }
        return "linkEnd";
    }

    private getPatternPropertyAssignmentName(node: PatternPropertyAssignmentType): string {
        const propName = node.name?.$refText ?? node.name?.ref?.name ?? "unnamed";
        const parent = node.$container;

        if (parent != undefined && this.reflection.isInstance(parent, PatternObjectInstance)) {
            const parentObj = parent as PatternObjectInstanceType;
            return `${parentObj.name ?? "unnamed"}@${propName}`;
        }

        return propName;
    }

    private getWhereClauseName(node: WhereClauseType): string {
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
        return "where";
    }

    private getPatternVariableName(node: PatternVariableType): string {
        return node.name ?? "unnamed";
    }
}
