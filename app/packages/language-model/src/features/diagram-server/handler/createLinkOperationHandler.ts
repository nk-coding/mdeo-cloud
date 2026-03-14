import type { GNode, ModelState, GModelIndex } from "@mdeo/language-shared";
import { BaseCreateEdgeOperationHandler, sharedImport, type CreateEdgeResult } from "@mdeo/language-shared";
import type { CreateEdgeOperation } from "@mdeo/protocol-common";
import type { PartialObjectInstance } from "../../../grammar/modelPartialTypes.js";
import { ObjectInstance, Link, LinkEnd } from "../../../grammar/modelTypes.js";
import type { LinkType, LinkEndType } from "../../../grammar/modelTypes.js";
import { ModelElementType } from "@mdeo/protocol-model";
import { LinkAssociationResolver, type LinkAssociationDisambiguation } from "../linkAssociationResolver.js";
import type { AstNode } from "langium";

const { injectable, inject } = sharedImport("inversify");
const { ModelState: ModelStateKey, GModelIndex: GModelIndexKey } = sharedImport("@eclipse-glsp/server");

/**
 * Handles create-edge operations for model links.
 */
@injectable()
export class CreateLinkOperationHandler extends BaseCreateEdgeOperationHandler {
    override readonly operationType = "createEdge";
    override label = "Create edge";
    readonly elementTypeIds = [ModelElementType.EDGE_LINK];

    @inject(ModelStateKey)
    protected readonly localModelState!: ModelState;

    @inject(GModelIndexKey)
    protected readonly localIndex!: GModelIndex;

    protected override async createEdge(
        operation: CreateEdgeOperation,
        sourceElement: GNode,
        targetElement: GNode
    ): Promise<CreateEdgeResult | undefined> {
        if (operation.elementTypeId !== ModelElementType.EDGE_LINK) {
            return undefined;
        }

        const sourceObject = this.resolveObjectInstance(sourceElement);
        const targetObject = this.resolveObjectInstance(targetElement);
        if (!sourceObject || !targetObject) {
            return undefined;
        }

        const sourceClass = sourceObject.class?.ref;
        const targetClass = targetObject.class?.ref;
        if (!sourceClass || !targetClass) {
            return undefined;
        }

        const associationResolver = new LinkAssociationResolver(
            this.localModelState.languageServices.shared.AstReflection
        );
        const candidates = associationResolver.findCandidates(sourceClass, targetClass);
        if (candidates.length === 0) {
            return undefined;
        }

        const params = operation.schema.params as LinkAssociationDisambiguation | undefined;
        const candidate = associationResolver.selectCandidate(candidates, params);
        if (!candidate) {
            return undefined;
        }

        const sourceProperty = params?.sourceProperty;
        const targetProperty = params?.targetProperty;

        const sourceName = sourceObject.name;
        const targetName = targetObject.name;
        if (!sourceName || !targetName) {
            return undefined;
        }

        const linkNode: LinkType = {
            $type: Link.name,
            source: {
                $type: LinkEnd.name,
                object: { $refText: sourceName },
                property: sourceProperty ? { $refText: sourceProperty } : undefined
            } as LinkEndType,
            target: {
                $type: LinkEnd.name,
                object: { $refText: targetName },
                property: targetProperty ? { $refText: targetProperty } : undefined
            } as LinkEndType
        };

        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (!rootCstNode) {
            throw new Error("Root CST node is not available.");
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        const linkText = await this.serializeNode(linkNode);
        const workspaceEdit = await this.createInsertAfterNodeEdit(rootCstNode, linkText, !isEmpty);

        const edgeId = this.computeLinkId(sourceObject, targetObject, sourceProperty, targetProperty);

        return {
            edgeId,
            edgeType: ModelElementType.EDGE_LINK,
            workspaceEdit
        };
    }

    /**
     * Resolves a diagram node to an ObjectInstance AST node.
     *
     * @param node The diagram node
     * @returns The resolved object instance, or undefined if the node is not an object instance
     */
    protected resolveObjectInstance(node: GNode): PartialObjectInstance | undefined {
        const astNode = this.localIndex.getAstNode(node) as AstNode | undefined;
        if (
            !astNode ||
            !this.localModelState.languageServices.shared.AstReflection.isInstance(astNode, ObjectInstance)
        ) {
            return undefined;
        }
        return astNode as PartialObjectInstance;
    }

    /**
     * Computes link IDs compatible with ModelModelIdProvider logic.
     * Format: Link_{objectName}[_{property}]--{objectName}[_{property}]
     *
     * @param sourceObject The source object instance
     * @param targetObject The target object instance
     * @param sourceProperty Optional disambiguated source-end property
     * @param targetProperty Optional disambiguated target-end property
     * @returns A stable link identifier compatible with server-side id provider logic
     */
    protected computeLinkId(
        sourceObject: PartialObjectInstance,
        targetObject: PartialObjectInstance,
        sourceProperty: string | undefined,
        targetProperty: string | undefined
    ): string {
        const sourceName = sourceObject.name ?? "unnamed";
        const targetName = targetObject.name ?? "unnamed";

        const sourceEnd = sourceProperty ? `${sourceName}_${sourceProperty}` : sourceName;
        const targetEnd = targetProperty ? `${targetName}_${targetProperty}` : targetName;

        return `${Link.name}_${sourceEnd}--${targetEnd}`;
    }
}
