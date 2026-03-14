import type { GLabel } from "@mdeo/editor-shared";
import { GLabelView, sharedImport } from "@mdeo/editor-shared";
import { AddAssociationMultiplicityOperation } from "@mdeo/protocol-metamodel";
import type { Operation } from "@eclipse-glsp/protocol";

const { injectable } = sharedImport("inversify");

/**
 * Label view for association multiplicity labels.
 * Handles commit for newly created multiplicity labels by dispatching
 * {@link AddAssociationMultiplicityOperation}.
 */
@injectable()
export class GAssociationMultiplicityLabelView extends GLabelView {
    /**
     * Creates the operation to dispatch when a new multiplicity label is committed.
     *
     * @param model The label model element
     * @param editText The committed edit text
     * @returns The operation to dispatch
     */
    protected override createNewLabelOperation(model: Readonly<GLabel>, editText: string): Operation {
        if (model.newLabelOperationKind === "multiplicity-edit-source") {
            return AddAssociationMultiplicityOperation.create({
                associationId: model.parentElementId!,
                endPosition: "source",
                multiplicityValue: editText
            });
        }

        if (model.newLabelOperationKind === "multiplicity-edit-target") {
            return AddAssociationMultiplicityOperation.create({
                associationId: model.parentElementId!,
                endPosition: "target",
                multiplicityValue: editText
            });
        }

        return super.createNewLabelOperation(model, editText);
    }
}
