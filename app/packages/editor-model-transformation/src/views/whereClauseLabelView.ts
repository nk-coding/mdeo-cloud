import { GLabelView, sharedImport } from "@mdeo/editor-shared";
import type { GLabel } from "@mdeo/editor-shared";
import { AddWhereClauseOperation } from "@mdeo/protocol-model-transformation";
import type { Operation } from "@eclipse-glsp/protocol";

const { injectable } = sharedImport("inversify");

/**
 * Prefix marking a label that commits into a new where clause.
 *
 * A clause that belongs to an application condition block encodes the block's index in the
 * operation kind, as {@code "add-where-clause:<index>"}.
 */
const ADD_WHERE_CLAUSE_KIND = "add-where-clause";

/**
 * View for rendering where clause labels in model transformation diagrams.
 * Handles commit for newly created where clause labels by dispatching
 * {@link AddWhereClauseOperation}.
 */
@injectable()
export class GWhereClauseLabelView extends GLabelView {
    /**
     * Creates the operation to dispatch when a new where clause label is committed.
     *
     * The full edited text (e.g. {@code where a.b == c.d}) is forwarded verbatim so
     * the server can insert it at the correct source location. When the label was created
     * for a condition block, the block's index travels with the operation.
     *
     * @param model The label model element
     * @param editText The committed edit text
     * @returns The operation to dispatch
     */
    protected override createNewLabelOperation(model: Readonly<GLabel>, editText: string): Operation {
        const kind = model.newLabelOperationKind;
        if (kind == undefined || (kind !== ADD_WHERE_CLAUSE_KIND && !kind.startsWith(`${ADD_WHERE_CLAUSE_KIND}:`))) {
            return super.createNewLabelOperation(model, editText);
        }

        const encodedIndex = kind.slice(ADD_WHERE_CLAUSE_KIND.length + 1);
        const conditionIndex = encodedIndex.length > 0 ? Number.parseInt(encodedIndex, 10) : undefined;

        return AddWhereClauseOperation.create({
            matchNodeId: model.parentElementId!,
            labelText: editText,
            conditionIndex: conditionIndex != undefined && !Number.isNaN(conditionIndex) ? conditionIndex : undefined
        });
    }
}
