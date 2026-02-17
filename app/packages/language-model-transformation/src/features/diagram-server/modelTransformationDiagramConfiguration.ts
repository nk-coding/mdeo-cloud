import type { DiagramConfiguration, EdgeTypeHint, GModelElementConstructor, ShapeTypeHint } from "@eclipse-glsp/server";
import { sharedImport } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./model/elementTypes.js";
import { GStartNode } from "./model/startNode.js";
import { GEndNode } from "./model/endNode.js";
import { GMatchNode } from "./model/matchNode.js";
import { GDiamondNode } from "./model/diamondNode.js";
import { GMergeNode } from "./model/mergeNode.js";
import { GControlFlowEdge } from "./model/controlFlowEdge.js";
import { GControlFlowLabelNode } from "./model/controlFlowLabelNode.js";
import { GControlFlowLabel } from "./model/controlFlowLabel.js";
import { GPatternInstanceNode } from "./model/patternInstanceNode.js";
import { GPatternInstanceNameLabel } from "./model/patternInstanceNameLabel.js";
import { GPatternPropertyLabel } from "./model/patternPropertyLabel.js";
import { GPatternLinkEdge } from "./model/patternLinkEdge.js";
import { GPatternLinkEndNode } from "./model/patternLinkEndNode.js";
import { GPatternLinkEndLabel } from "./model/patternLinkEndLabel.js";
import { GWhereClauseLabel } from "./model/whereClauseLabel.js";
import { GVariableLabel } from "./model/variableLabel.js";

const { injectable } = sharedImport("inversify");
const { ServerLayoutKind, getDefaultMapping } = sharedImport("@eclipse-glsp/server");

/**
 * Configuration for model transformation diagrams defining layout behavior and element type mappings.
 * Specifies manual layout with client-side rendering and animated updates.
 */
@injectable()
export class ModelTransformationDiagramConfiguration implements DiagramConfiguration {
    readonly layoutKind = ServerLayoutKind.MANUAL;
    readonly animatedUpdate = true;
    readonly needsClientLayout = true;
    readonly needsServerLayout = false;

    /**
     * Returns the mapping of element type IDs to their corresponding GModelElement constructors.
     *
     * @returns Map from type ID to GModelElementConstructor
     */
    get typeMapping(): Map<string, GModelElementConstructor> {
        const mapping = getDefaultMapping();

        // Outer control flow graph nodes
        mapping.set(ModelTransformationElementType.NODE_START, GStartNode);
        mapping.set(ModelTransformationElementType.NODE_END, GEndNode);
        mapping.set(ModelTransformationElementType.NODE_MATCH, GMatchNode);
        mapping.set(ModelTransformationElementType.NODE_DIAMOND, GDiamondNode);
        mapping.set(ModelTransformationElementType.NODE_MERGE, GMergeNode);

        // Control flow edges and labels
        mapping.set(ModelTransformationElementType.EDGE_CONTROL_FLOW, GControlFlowEdge);
        mapping.set(ModelTransformationElementType.NODE_CONTROL_FLOW_LABEL, GControlFlowLabelNode);
        mapping.set(ModelTransformationElementType.LABEL_CONTROL_FLOW, GControlFlowLabel);

        // Inner pattern elements - instances
        mapping.set(ModelTransformationElementType.NODE_PATTERN_INSTANCE, GPatternInstanceNode);
        mapping.set(ModelTransformationElementType.LABEL_PATTERN_INSTANCE_NAME, GPatternInstanceNameLabel);
        mapping.set(ModelTransformationElementType.LABEL_PATTERN_PROPERTY, GPatternPropertyLabel);

        // Inner pattern elements - links
        mapping.set(ModelTransformationElementType.EDGE_PATTERN_LINK, GPatternLinkEdge);
        mapping.set(ModelTransformationElementType.NODE_PATTERN_LINK_END, GPatternLinkEndNode);
        mapping.set(ModelTransformationElementType.LABEL_PATTERN_LINK_END, GPatternLinkEndLabel);

        // Inner pattern elements - constraints
        mapping.set(ModelTransformationElementType.LABEL_WHERE_CLAUSE, GWhereClauseLabel);
        mapping.set(ModelTransformationElementType.LABEL_VARIABLE, GVariableLabel);

        return mapping;
    }

    /**
     * Returns the shape type hints for model transformation diagram elements.
     * For readonly diagrams, elements are not repositionable, deletable, or resizable.
     *
     * @returns Array of shape type hints
     */
    get shapeTypeHints(): ShapeTypeHint[] {
        return [
            {
                elementTypeId: ModelTransformationElementType.NODE_START,
                repositionable: false,
                deletable: false,
                resizable: false,
                reparentable: false
            },
            {
                elementTypeId: ModelTransformationElementType.NODE_END,
                repositionable: false,
                deletable: false,
                resizable: false,
                reparentable: false
            },
            {
                elementTypeId: ModelTransformationElementType.NODE_MATCH,
                repositionable: false,
                deletable: false,
                resizable: false,
                reparentable: false
            },
            {
                elementTypeId: ModelTransformationElementType.NODE_DIAMOND,
                repositionable: false,
                deletable: false,
                resizable: false,
                reparentable: false
            },
            {
                elementTypeId: ModelTransformationElementType.NODE_MERGE,
                repositionable: false,
                deletable: false,
                resizable: false,
                reparentable: false
            },
            {
                elementTypeId: ModelTransformationElementType.NODE_PATTERN_INSTANCE,
                repositionable: false,
                deletable: false,
                resizable: false,
                reparentable: false
            }
        ];
    }

    /**
     * Returns the edge type hints for model transformation diagram elements.
     *
     * @returns Array of edge type hints
     */
    get edgeTypeHints(): EdgeTypeHint[] {
        return [
            {
                elementTypeId: ModelTransformationElementType.EDGE_CONTROL_FLOW,
                repositionable: false,
                deletable: false,
                routable: false,
                sourceElementTypeIds: [
                    ModelTransformationElementType.NODE_START,
                    ModelTransformationElementType.NODE_MATCH,
                    ModelTransformationElementType.NODE_DIAMOND,
                    ModelTransformationElementType.NODE_MERGE
                ],
                targetElementTypeIds: [
                    ModelTransformationElementType.NODE_END,
                    ModelTransformationElementType.NODE_MATCH,
                    ModelTransformationElementType.NODE_DIAMOND,
                    ModelTransformationElementType.NODE_MERGE
                ]
            },
            {
                elementTypeId: ModelTransformationElementType.EDGE_PATTERN_LINK,
                repositionable: false,
                deletable: false,
                routable: false,
                sourceElementTypeIds: [ModelTransformationElementType.NODE_PATTERN_INSTANCE],
                targetElementTypeIds: [ModelTransformationElementType.NODE_PATTERN_INSTANCE]
            }
        ];
    }
}
