import { BaseOperationHandler, GCompartment, OperationHandlerCommand, sharedImport } from "@mdeo/language-shared";
import type { Command, GModelElement } from "@eclipse-glsp/server";
import { AddHeaderDeclarationOperation, ModelTransformationElementType } from "@mdeo/protocol-model-transformation";
import { InsertNewLabelAction } from "@mdeo/protocol-common";
import type { ModelTransformationType } from "../../../grammar/modelTransformationTypes.js";
import { GDeclarationLabel } from "../model/declarationLabel.js";
import {
    declarationsCompartmentId,
    headerInsertEdit,
    headerLineKind,
    NEW_DECLARATION_LABEL_PREFIX
} from "../modelTransformationHeader.js";

const { injectable } = sharedImport("inversify");

/**
 * Handler adding a line to the header of a transformation — an `in` / `out` declaration (or a
 * path import) — when a new row of the start node or of an end node is committed. The line goes
 * where the serializer puts it: imports after `using` and the other imports, declarations after
 * the imports, inputs before outputs.
 */
@injectable()
export class AddHeaderDeclarationOperationHandler extends BaseOperationHandler {
    override readonly operationType = AddHeaderDeclarationOperation.KIND;

    /**
     * Creates a workspace-edit command inserting the committed line into the header.
     *
     * @param operation The add-header-declaration operation
     * @returns A command wrapping the workspace edit, or undefined when there is nothing to add
     */
    override async createCommand(operation: AddHeaderDeclarationOperation): Promise<Command | undefined> {
        const text = operation.labelText?.trim();
        const root = this.modelState.sourceModel as ModelTransformationType | undefined;
        const document = root?.$document;
        if (!text || root == undefined || document == undefined) {
            return undefined;
        }
        const edit = headerInsertEdit(root, document, text);
        if (edit == undefined) {
            return undefined;
        }
        return new OperationHandlerCommand(
            this.modelState,
            { changes: { [document.uri.toString()]: [edit] } },
            undefined
        );
    }
}

/**
 * Builds the action that adds a new, prefilled declaration row to a node and starts editing it:
 * an input to the start node, an output to the primary successful end node. Committing the row
 * adds the line through {@link AddHeaderDeclarationOperation}.
 *
 * The row goes below the node's other rows; a node without rows gets its rows compartment with
 * it (and so turns from its plain symbol into a box while the row is edited).
 *
 * @param node The start node (for an input) or the primary end node (for an output)
 * @param text The prefilled line, e.g. `in input1: int`
 * @returns The action, or undefined when the text is no declaration
 */
export function buildInsertDeclarationAction(node: GModelElement, text: string): InsertNewLabelAction | undefined {
    const kind = headerLineKind(text);
    if (kind !== "in" && kind !== "out") {
        return undefined;
    }
    const labelId = `${NEW_DECLARATION_LABEL_PREFIX}${kind}`;
    const label = GDeclarationLabel.builder()
        .id(labelId)
        .text(text)
        .isNewLabel(true)
        .newLabelOperationKind(AddHeaderDeclarationOperation.NEW_LABEL_KIND)
        .newLabelParentElementId(node.id)
        .build();

    const compartmentId = declarationsCompartmentId(node.id);
    const existing = node.children.find((child) => child.id === compartmentId);
    if (existing != undefined) {
        return InsertNewLabelAction.create({
            parentElementId: compartmentId,
            insertIndex: existing.children.length,
            templates: [label],
            labelId
        });
    }

    const compartment = GCompartment.builder()
        .type(ModelTransformationElementType.COMPARTMENT)
        .id(compartmentId)
        .build();
    compartment.children.push(label);
    return InsertNewLabelAction.create({
        parentElementId: node.id,
        insertIndex: node.children.length,
        templates: [compartment],
        labelId
    });
}
