import { GLabelView, sharedImport } from "@mdeo/editor-shared";
import type { GLabel } from "@mdeo/editor-shared";
import { AddPropertyValueComparisonOperation } from "@mdeo/protocol-model-transformation";
import type { Operation } from "@eclipse-glsp/protocol";

const { injectable } = sharedImport("inversify");

/**
 * View for rendering pattern property labels in model transformation diagrams.
 * Handles commit for newly created property value/comparison labels by dispatching
 * {@link AddPropertyValueComparisonOperation}.
 *
 * The property name is encoded in {@code newLabelOperationKind} as
 * {@code "property-value-comparison-edit:<propertyName>"}.
 */
@injectable()
export class GPatternPropertyLabelView extends GLabelView {
    /**
     * Returns additional CSS classes for pattern property labels.
     *
     * @param model The label model
     * @returns Record of CSS class names and their enabled state
     */
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        return {
            ...super.getClasses(model),
            "text-sm": true
        };
    }

    /**
     * Creates the operation to dispatch when a new pattern property label is committed.
     *
     * The full edited text (e.g. {@code propName = expr} or {@code propName == expr}) is
     * forwarded verbatim so the server can insert it at the correct source location.
     *
     * @param model The label model element
     * @param editText The committed edit text
     * @returns The operation to dispatch
     */
    protected override createNewLabelOperation(model: Readonly<GLabel>, editText: string): Operation {
        if (model.newLabelOperationKind !== "property-value-comparison-edit") {
            return super.createNewLabelOperation(model, editText);
        }

        return AddPropertyValueComparisonOperation.create({
            instanceId: model.parentElementId!,
            labelText: editText
        });
    }
}
