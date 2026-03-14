import { AssociationEndKind } from "@mdeo/protocol-metamodel";
import { MetamodelAssociationOperators } from "../../../grammar/metamodelTypes.js";

/**
 * Maps each association operator to source and target end kinds.
 */
export const OPERATOR_TO_KINDS: Record<string, { sourceKind: AssociationEndKind; targetKind: AssociationEndKind }> = {
    [MetamodelAssociationOperators.NAVIGABLE_TO_TARGET]: {
        sourceKind: AssociationEndKind.NONE,
        targetKind: AssociationEndKind.ARROW
    },
    [MetamodelAssociationOperators.NAVIGABLE_TO_SOURCE]: {
        sourceKind: AssociationEndKind.ARROW,
        targetKind: AssociationEndKind.NONE
    },
    [MetamodelAssociationOperators.BIDIRECTIONAL]: {
        sourceKind: AssociationEndKind.ARROW,
        targetKind: AssociationEndKind.ARROW
    },
    [MetamodelAssociationOperators.COMPOSITION_SOURCE_NAVIGABLE_TARGET]: {
        sourceKind: AssociationEndKind.COMPOSITION,
        targetKind: AssociationEndKind.ARROW
    },
    [MetamodelAssociationOperators.COMPOSITION_SOURCE]: {
        sourceKind: AssociationEndKind.COMPOSITION,
        targetKind: AssociationEndKind.NONE
    },
    [MetamodelAssociationOperators.COMPOSITION_TARGET_NAVIGABLE_SOURCE]: {
        sourceKind: AssociationEndKind.ARROW,
        targetKind: AssociationEndKind.COMPOSITION
    },
    [MetamodelAssociationOperators.COMPOSITION_TARGET]: {
        sourceKind: AssociationEndKind.NONE,
        targetKind: AssociationEndKind.COMPOSITION
    }
};

/**
 * Maps source/target end-kind pairs to the corresponding association operator.
 */
export const KINDS_TO_OPERATOR: Record<string, MetamodelAssociationOperators> = {
    [`${AssociationEndKind.NONE},${AssociationEndKind.ARROW}`]: MetamodelAssociationOperators.NAVIGABLE_TO_TARGET,
    [`${AssociationEndKind.ARROW},${AssociationEndKind.NONE}`]: MetamodelAssociationOperators.NAVIGABLE_TO_SOURCE,
    [`${AssociationEndKind.ARROW},${AssociationEndKind.ARROW}`]: MetamodelAssociationOperators.BIDIRECTIONAL,
    [`${AssociationEndKind.COMPOSITION},${AssociationEndKind.ARROW}`]:
        MetamodelAssociationOperators.COMPOSITION_SOURCE_NAVIGABLE_TARGET,
    [`${AssociationEndKind.COMPOSITION},${AssociationEndKind.NONE}`]: MetamodelAssociationOperators.COMPOSITION_SOURCE,
    [`${AssociationEndKind.ARROW},${AssociationEndKind.COMPOSITION}`]:
        MetamodelAssociationOperators.COMPOSITION_TARGET_NAVIGABLE_SOURCE,
    [`${AssociationEndKind.NONE},${AssociationEndKind.COMPOSITION}`]: MetamodelAssociationOperators.COMPOSITION_TARGET
};
