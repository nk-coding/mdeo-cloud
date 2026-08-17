import {
    BaseOperationHandler,
    OperationHandlerCommand,
    GCompartment,
    GHorizontalDivider,
    sharedImport
} from "@mdeo/language-shared";
import type { Command, GModelElement } from "@eclipse-glsp/server";
import type { AddWhereClauseOperation } from "@mdeo/protocol-model-transformation";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";
import type { ContextActionRequestContext, ContextItemProvider } from "@mdeo/language-shared";
import type { ContextItem } from "@mdeo/protocol-common";
import { InsertNewLabelAction } from "@mdeo/protocol-common";
import { GMatchNodeCompartments } from "../model/matchNodeCompartments.js";
import { GWhereClauseLabel } from "../model/whereClauseLabel.js";
import { getPatternFromMatchNode } from "../modelTransformationPatternUtils.js";
import {
    PatternApplicationCondition,
    type PatternApplicationConditionType,
    type PatternType
} from "../../../grammar/modelTransformationTypes.js";
import type { AstNode } from "langium";

const { injectable } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");

/**
 * Prefix used to build unique IDs for new-where-clause placeholder labels.
 * The full label ID is `${NEW_WHERE_CLAUSE_LABEL_PREFIX}${matchNodeId}`.
 */
export const NEW_WHERE_CLAUSE_LABEL_PREFIX = "__new-label-whereclause-";

/**
 * Handler for adding where clause entries on match nodes and on their application condition
 * blocks.
 *
 * A clause added on a `forbid` / `require` block constrains that block: the block only holds
 * when its graph is found *and* its clauses are satisfied. The clause is written into the
 * block, so it may read the block's own nodes as well as everything the match binds.
 */
@injectable()
export class AddWhereClauseOperationHandler extends BaseOperationHandler implements ContextItemProvider {
    override readonly operationType = "addWhereClause";

    /**
     * Creates a command for add-where-clause operations.
     *
     * The clause is written into the match pattern, or into the application condition block
     * named by {@link AddWhereClauseOperation.conditionIndex}.
     *
     * @param operation The requested operation
     * @returns A command applying the insertion, or `undefined` when the target cannot be
     *          resolved
     */
    override async createCommand(operation: AddWhereClauseOperation): Promise<Command | undefined> {
        const text = operation.labelText?.trim();
        if (!text || text.length === 0) {
            return undefined;
        }

        const gmodelElement = this.modelState.index.get(operation.matchNodeId);
        if (gmodelElement == undefined) {
            return undefined;
        }

        const astNode = this.index.getAstNode(gmodelElement);
        if (astNode == undefined) {
            return undefined;
        }

        const patternNode = getPatternFromMatchNode(astNode, this.reflection);
        if (patternNode == undefined) {
            return undefined;
        }

        const container = this.resolveContainer(patternNode as PatternType, operation.conditionIndex);
        if (container == undefined) {
            return undefined;
        }
        const cstNode = container.$cstNode;
        if (cstNode == undefined) {
            return undefined;
        }

        const openBrace = GrammarUtils.findNodeForKeyword(cstNode, "{");
        const closeBrace = GrammarUtils.findNodeForKeyword(cstNode, "}");
        if (openBrace == undefined || closeBrace == undefined) {
            return undefined;
        }

        const edit = this.insertIntoScope(openBrace, closeBrace, true, text);
        return new OperationHandlerCommand(this.modelState, edit, undefined);
    }

    /**
     * Returns context items for adding where clauses on match nodes and on the members of
     * their condition blocks.
     *
     * On a member of a block the clause is added to that block, which is the only place from
     * which the block's own nodes can be read.
     *
     * @param element The selected element
     * @param _context Additional request context
     * @returns Context actions for this handler
     */
    getContextItems(element: GModelElement, _context: ContextActionRequestContext): ContextItem[] {
        if (element.type === ModelTransformationElementType.NODE_MATCH) {
            return [
                {
                    id: `add-where-clause-${element.id}`,
                    label: "Add Where Clause",
                    icon: "funnel-plus",
                    sortString: "d",
                    action: this.buildInsertWhereClauseAction(element)
                }
            ];
        }

        const condition = this.findConditionOfElement(element);
        const matchNode = this.findMatchNode(element);
        if (condition == undefined || matchNode == undefined) {
            return [];
        }
        const index = this.getConditions(condition.$container as PatternType).indexOf(condition);
        if (index < 0) {
            return [];
        }

        return [
            {
                id: `add-where-clause-${element.id}`,
                label: `Add Where Clause to ${this.describeCondition(condition, index)}`,
                icon: "funnel-plus",
                sortString: "d",
                action: this.buildInsertWhereClauseAction(matchNode, index)
            }
        ];
    }

    /**
     * Returns the condition block the selected element is declared in.
     *
     * @param element The selected element
     * @returns The block, or `undefined` when the element belongs to the match pattern
     */
    private findConditionOfElement(element: GModelElement): PatternApplicationConditionType | undefined {
        if (
            element.type !== ModelTransformationElementType.NODE_PATTERN_INSTANCE &&
            element.type !== ModelTransformationElementType.EDGE_PATTERN_LINK &&
            element.type !== ModelTransformationElementType.LABEL_WHERE_CLAUSE
        ) {
            return undefined;
        }

        const astNode = this.index.getAstNode(element);
        let current: AstNode | undefined = astNode?.$container;
        while (current != undefined) {
            if (this.reflection.isInstance(current, PatternApplicationCondition)) {
                return current as PatternApplicationConditionType;
            }
            current = current.$container;
        }
        return undefined;
    }

    /**
     * Walks up to the match node the selected element is rendered in.
     *
     * @param element The selected element
     * @returns The enclosing match node, or `undefined`
     */
    private findMatchNode(element: GModelElement): GModelElement | undefined {
        let current: GModelElement | undefined = element;
        while (current != undefined) {
            if (current.type === ModelTransformationElementType.NODE_MATCH) {
                return current;
            }
            current = current.parent as GModelElement | undefined;
        }
        return undefined;
    }

    /**
     * Returns the application condition blocks of a pattern, in declaration order.
     *
     * @param pattern The pattern to inspect
     * @returns The blocks of the pattern
     */
    private getConditions(pattern: PatternType | undefined): PatternApplicationConditionType[] {
        return ((pattern?.elements ?? []) as AstNode[]).filter((element) =>
            this.reflection.isInstance(element, PatternApplicationCondition)
        ) as PatternApplicationConditionType[];
    }

    /**
     * Builds the label shown for a block in the context rail.
     *
     * @param condition The block
     * @param index Its index within the pattern, used when the block is unnamed
     * @returns The label text
     */
    private describeCondition(condition: PatternApplicationConditionType, index: number): string {
        const kind = condition.kind === "require" ? "Require" : "Forbid";
        return condition.name != undefined ? `${kind} ${condition.name}` : `${kind} #${index + 1}`;
    }

    /**
     * Resolves the container the new clause is written into.
     *
     * @param pattern The pattern of the match node
     * @param conditionIndex The index of the block to write into, if any
     * @returns The pattern or the block, or `undefined` when the index does not exist
     */
    private resolveContainer(
        pattern: PatternType,
        conditionIndex: number | undefined
    ): PatternType | PatternApplicationConditionType | undefined {
        if (conditionIndex == undefined) {
            return pattern;
        }
        const conditions = this.getConditions(pattern);
        return conditionIndex >= 0 && conditionIndex < conditions.length ? conditions[conditionIndex] : undefined;
    }

    /**
     * Builds an {@link InsertNewLabelAction} for inserting a new where-clause label on the
     * given match node.
     *
     * Where-clauses must appear **before** variables in the `__compartments` container.
     * The method handles three cases:
     * - No `__compartments` container: creates the full container tree.
     * - `__compartments` exists but `__where-clauses` does not: inserts a new `__where-clauses`
     *   compartment **before** any existing `__variables` compartment (at index 1, after the
     *   top-divider), and also inserts an inter-compartment divider.
     * - `__where-clauses` already exists: appends the label to the end of that compartment.
     *
     * @param element The match-node GModel element.
     * @param conditionIndex The condition block the clause is written into, if any. It travels
     *        with the label so that the commit knows where the text belongs.
     * @returns The {@link InsertNewLabelAction} to dispatch.
     */
    private buildInsertWhereClauseAction(element: GModelElement, conditionIndex?: number): InsertNewLabelAction {
        const nodeId = element.id;
        const labelId = `${NEW_WHERE_CLAUSE_LABEL_PREFIX}${nodeId}`;

        const label = GWhereClauseLabel.builder()
            .id(labelId)
            .text("")
            .isNewLabel(true)
            .newLabelOperationKind(
                conditionIndex != undefined ? `add-where-clause:${conditionIndex}` : "add-where-clause"
            )
            .newLabelParentElementId(nodeId)
            .build();
        label.editMode = true;

        const compartmentsId = `${nodeId}__compartments`;
        const whereClausesId = `${nodeId}__where-clauses`;
        const variablesId = `${nodeId}__variables`;

        const compartmentsContainer = element.children.find((c) => c.id === compartmentsId);

        if (compartmentsContainer == undefined) {
            const topDivider = GHorizontalDivider.builder()
                .type(ModelTransformationElementType.DIVIDER)
                .id(`${nodeId}__compartments-top-divider`)
                .build();

            const whereClausesCompartment = GCompartment.builder()
                .type(ModelTransformationElementType.COMPARTMENT)
                .id(whereClausesId)
                .build();
            whereClausesCompartment.children.push(label);

            const container = GMatchNodeCompartments.builder().id(compartmentsId).build();
            container.children.push(topDivider);
            container.children.push(whereClausesCompartment);

            return InsertNewLabelAction.create({
                parentElementId: nodeId,
                insertIndex: element.children.length,
                templates: [container],
                labelId
            });
        }

        const whereClausesCompartment = compartmentsContainer.children.find((c) => c.id === whereClausesId);

        if (whereClausesCompartment != undefined) {
            return InsertNewLabelAction.create({
                parentElementId: whereClausesId,
                insertIndex: whereClausesCompartment.children.length,
                templates: [label],
                labelId
            });
        }

        const newWhereClausesCompartment = GCompartment.builder()
            .type(ModelTransformationElementType.COMPARTMENT)
            .id(whereClausesId)
            .build();
        newWhereClausesCompartment.children.push(label);

        const templates: GModelElement[] = [newWhereClausesCompartment];

        const variablesCompartment = compartmentsContainer.children.find((c) => c.id === variablesId);
        if (variablesCompartment != undefined) {
            const divider = GHorizontalDivider.builder()
                .type(ModelTransformationElementType.DIVIDER)
                .id(`${nodeId}__compartment-divider-1`)
                .build();
            templates.push(divider);
        }

        return InsertNewLabelAction.create({
            parentElementId: compartmentsId,
            insertIndex: 1,
            templates,
            labelId
        });
    }
}
