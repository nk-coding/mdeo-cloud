import type { GModelRoot } from "@eclipse-glsp/server";
import type { LayoutOperation } from "@mdeo/protocol-common";
import { BaseLayoutEngine, GEdge, GNode, sharedImport } from "@mdeo/language-shared";
import type { ElkExtendedEdge, ElkNode } from "elkjs";
import { GAssociationPropertyNode } from "./model/associationPropertyNode.js";
import { GAssociationMultiplicityNode } from "./model/associationMultiplicityNode.js";

const { injectable } = sharedImport("inversify");

/**
 * Layout engine for metamodel diagrams.
 */
@injectable()
export class MetamodelLayoutEngine extends BaseLayoutEngine {
    protected override transformToElk(model: GModelRoot, operation: LayoutOperation): ElkNode {
        const bounds = operation.bounds;
        const nodes: ElkNode[] = [];
        const edges: ElkExtendedEdge[] = [];
        for (const child of model.children) {
            if (child instanceof GNode) {
                nodes.push({
                    id: child.id,
                    width: bounds[child.id]?.width,
                    height: bounds[child.id]?.height
                });
            } else if (child instanceof GEdge) {
                edges.push({
                    id: child.id,
                    sources: [child.sourceId],
                    targets: [child.targetId],
                    labels: child.children
                        .filter(
                            (label) =>
                                label instanceof GAssociationPropertyNode ||
                                label instanceof GAssociationMultiplicityNode
                        )
                        .map((label) => ({
                            text: "_",
                            width: bounds[label.id]?.width,
                            height: bounds[label.id]?.height,
                            layoutOptions: {
                                "elk.edgeLabels.placement": label.end === "source" ? "HEAD" : "TAIL"
                            }
                        }))
                });
            }
        }
        return {
            id: model.id,
            layoutOptions: {
                "elk.algorithm": "layered",
                "elk.direction": "DOWN",
                "elk.layered.spacing.nodeNodeBetweenLayers": "40",
                "elk.spacing.nodeNode": "30"
            },
            children: nodes,
            edges: edges
        };
    }
}
