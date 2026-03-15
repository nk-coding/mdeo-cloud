import { BaseOperationHandler, OperationHandlerCommand, sharedImport } from "@mdeo/language-shared";
import {
    Association,
    AssociationEnd,
    SingleMultiplicity,
    type AssociationType,
    type AssociationEndType,
    type SingleMultiplicityType,
    type MultiplicityType
} from "../../../grammar/metamodelTypes.js";
import type { AddAssociationMultiplicityOperation } from "@mdeo/protocol-metamodel";
import { MetamodelElementType } from "@mdeo/protocol-metamodel";
import type { AstNode } from "langium";
import type { Command, GModelElement } from "@eclipse-glsp/server";
import type { ContextActionRequestContext, ContextItemProvider, GEdge } from "@mdeo/language-shared";
import type { ContextItem } from "@mdeo/protocol-common";
import { EdgeAttachmentPosition, InsertNewLabelAction } from "@mdeo/protocol-common";
import { isImportedElement, parseMultiplicity } from "./metamodelHandlerUtils.js";
import { GAssociationMultiplicityNode } from "../model/associationMultiplicityNode.js";
import { GAssociationMultiplicityLabel } from "../model/associationMultiplicityLabel.js";
import { NodeLayoutMetadataUtil } from "../metadataTypes.js";

const { injectable } = sharedImport("inversify");

/**
 * Prefix used to build unique IDs for new-multiplicity placeholder labels.
 * The full label ID is `${NEW_MULTIPLICITY_LABEL_PREFIX}${edgeId}-${end}`.
 */
export const NEW_MULTIPLICITY_LABEL_PREFIX = "__new-multiplicity-label-";

/**
 * Handler for adding multiplicity labels to association ends.
 */
@injectable()
export class AddAssociationMultiplicityOperationHandler extends BaseOperationHandler implements ContextItemProvider {
    override readonly operationType = "addAssociationMultiplicity";

    /**
     * Creates a workspace-edit command that adds default multiplicity to one association end.
     *
     * @param operation The add-association-multiplicity operation
     * @returns A command wrapping the workspace edit, or undefined
     */
    override async createCommand(operation: AddAssociationMultiplicityOperation): Promise<Command | undefined> {
        const edgeId = operation.associationId ?? operation.edgeId;
        const gmodelElement = this.modelState.index.get(edgeId);
        if (gmodelElement == undefined) {
            return undefined;
        }

        const astNode = this.index.getAstNode(gmodelElement);
        if (astNode == undefined || !this.reflection.isInstance(astNode, Association)) {
            return undefined;
        }

        const assocNode = astNode as AssociationType;
        const endPosition = operation.endPosition ?? operation.end;
        const endAst: AssociationEndType | undefined =
            endPosition === "source"
                ? (assocNode.source as AssociationEndType)
                : (assocNode.target as AssociationEndType);

        if (endAst == undefined || endAst.multiplicity != undefined) {
            return undefined;
        }

        const endCstNode = endAst.$cstNode;
        if (endCstNode == undefined) {
            return undefined;
        }

        const requestedValue = operation.multiplicityValue?.trim() ?? "";
        const multiplicityAst: MultiplicityType =
            parseMultiplicity(requestedValue) ??
            ({
                $type: SingleMultiplicity.name,
                value: "*"
            } as SingleMultiplicityType);

        const newEndAst: AssociationEndType = {
            $type: AssociationEnd.name,
            class: endAst.class,
            name: endAst.name,
            multiplicity: multiplicityAst
        };

        const edit = await this.replaceCstNode(endCstNode, newEndAst as unknown as AstNode);
        return new OperationHandlerCommand(this.modelState, edit, undefined);
    }

    /**
     * Returns context items for adding source/target multiplicities on association edges.
     *
     * @param element The selected element
     * @param _context Request context
     * @returns Context actions provided by this handler
     */
    getContextItems(element: GModelElement, _context: ContextActionRequestContext): ContextItem[] {
        if (element.type !== MetamodelElementType.EDGE_ASSOCIATION) {
            return [];
        }

        if (isImportedElement(element)) {
            return [];
        }

        const edge = element as GEdge;
        const association = this.getAssociationForEdge(edge.id);
        const items: ContextItem[] = [];

        for (const end of ["source", "target"] as const) {
            const endAst = end === "source" ? association?.source : association?.target;
            const existingMultiplicity = endAst?.multiplicity;
            if (existingMultiplicity != undefined || endAst?.name == undefined) {
                continue;
            }

            const position = end === "source" ? EdgeAttachmentPosition.END : EdgeAttachmentPosition.START;
            const nodeEnd = end === "source" ? "target" : "source";
            items.push({
                id: `add-multiplicity-${edge.id}-${end}`,
                label: `Add Multiplicity (${end})`,
                icon: "square-asterisk",
                sortString: "b",
                position,
                action: this.buildInsertMultiplicityAction(edge, end, nodeEnd)
            });
        }

        return items;
    }

    /**
     * Builds an {@link InsertNewLabelAction} whose template contains a new
     * {@link GAssociationMultiplicityNode} wrapping a new
     * {@link GAssociationMultiplicityLabel} in edit mode.
     *
     * The wrapper node is placed at the end of the edge's children, matching
     * how the {@link MetamodelGModelFactory} positions real multiplicity nodes.
     *
     * @param edge The association edge GModel element.
     * @param end Which end of the association ("source" or "target").
     * @param nodeEnd The `end` property to set on the wrapper node (opposite of `end`).
     * @returns The {@link InsertNewLabelAction} to dispatch.
     */
    private buildInsertMultiplicityAction(
        edge: GEdge,
        end: "source" | "target",
        nodeEnd: "source" | "target"
    ): InsertNewLabelAction {
        const wrapperId = `__new-multiplicity-${edge.id}-${end}`;
        const labelId = `${NEW_MULTIPLICITY_LABEL_PREFIX}${edge.id}-${end}`;
        const defaultMeta = NodeLayoutMetadataUtil.create(0, 0);

        const label = GAssociationMultiplicityLabel.builder()
            .id(labelId)
            .text("")
            .isNewLabel(true)
            .newLabelOperationKind(`multiplicity-edit-${end}`)
            .newLabelParentElementId(edge.id)
            .build();

        const wrapperNode = GAssociationMultiplicityNode.builder().id(wrapperId).end(nodeEnd).meta(defaultMeta).build();
        wrapperNode.children.push(label);

        return InsertNewLabelAction.create({
            parentElementId: edge.id,
            insertIndex: edge.children.length,
            templates: [wrapperNode],
            labelId
        });
    }

    /**
     * Resolves the association AST node for a diagram edge id.
     *
     * @param edgeId The diagram edge identifier
     * @returns The association AST node if available
     */
    private getAssociationForEdge(edgeId: string): AssociationType | undefined {
        const edge = this.modelState.index.get(edgeId);
        if (edge == undefined) {
            return undefined;
        }

        const astNode = this.index.getAstNode(edge);
        if (astNode == undefined || !this.reflection.isInstance(astNode, Association)) {
            return undefined;
        }

        return astNode as AssociationType;
    }
}
