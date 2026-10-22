import {
    BaseOperationHandler,
    GCompartment,
    GHorizontalDivider,
    OperationHandlerCommand,
    sharedImport
} from "@mdeo/language-shared";
import type { Command, GModelElement } from "@eclipse-glsp/server";
import { AddHeaderDeclarationOperation, ModelTransformationElementType } from "@mdeo/protocol-model-transformation";
import { InsertNewLabelAction } from "@mdeo/protocol-common";
import type { ModelTransformationType } from "../../../grammar/modelTransformationTypes.js";
import { GHeaderRowLabel } from "../model/headerNode.js";
import { headerInsertEdit, headerLineKind, NEW_HEADER_LABEL_PREFIX } from "../modelTransformationHeader.js";

const { injectable } = sharedImport("inversify");

/**
 * Handler adding a line to the header of a transformation — a path import or an `in` / `out`
 * declaration — when a new row of the header box is committed. The line goes where the
 * serializer puts it: imports after `using` and the other imports, declarations after the
 * imports, inputs before outputs.
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
 * Builds the action that adds a new, prefilled row to the header box and starts editing it;
 * committing the row adds the line through {@link AddHeaderDeclarationOperation}.
 *
 * An import row goes below the other imports, an input below the other inputs and an output
 * below all declarations, which is where the line will be inserted. A section that does not
 * exist yet is inserted together with its row.
 *
 * @param header The header box
 * @param text The prefilled line, e.g. `in input1: int`
 * @returns The action, or undefined when the text is no header line
 */
export function buildInsertHeaderLineAction(header: GModelElement, text: string): InsertNewLabelAction | undefined {
    const kind = headerLineKind(text);
    if (kind == undefined) {
        return undefined;
    }
    const section = kind === "import" ? "imports" : "declarations";
    const labelId = `${NEW_HEADER_LABEL_PREFIX}${section}`;
    const builder = kind === "import" ? GHeaderRowLabel.importBuilder() : GHeaderRowLabel.declarationBuilder();
    const label = builder
        .id(labelId)
        .text(text)
        .isNewLabel(true)
        .newLabelOperationKind(AddHeaderDeclarationOperation.NEW_LABEL_KIND)
        .newLabelParentElementId(header.id)
        .build();

    const sectionId = `${header.id}__${section}`;
    const existing = header.children.find((child) => child.id === sectionId);
    if (existing != undefined) {
        let insertIndex = existing.children.length;
        if (kind === "in") {
            const lastInput = existing.children.findLastIndex((row) =>
                /^\s*in\b/.test((row as { text?: string }).text ?? "")
            );
            insertIndex = lastInput >= 0 ? lastInput + 1 : 0;
        }
        return InsertNewLabelAction.create({ parentElementId: sectionId, insertIndex, templates: [label], labelId });
    }

    const divider = GHorizontalDivider.builder()
        .type(ModelTransformationElementType.DIVIDER)
        .id(`${header.id}__${section}-divider`)
        .build();
    const compartment = GCompartment.builder().type(ModelTransformationElementType.COMPARTMENT).id(sectionId).build();
    compartment.children.push(label);
    // the imports follow the title, the declarations come last
    const insertIndex = kind === "import" ? 1 : header.children.length;
    return InsertNewLabelAction.create({
        parentElementId: header.id,
        insertIndex,
        templates: [divider, compartment],
        labelId
    });
}
