import type { DiagramConfiguration, EdgeTypeHint, GModelElementConstructor, ShapeTypeHint } from "@eclipse-glsp/server";
import { sharedImport } from "@mdeo/language-shared";
import { ModelElementType } from "@mdeo/protocol-model";
import { GObjectNode } from "./model/objectNode.js";
import { GObjectNameLabel } from "./model/objectNameLabel.js";
import { GPropertyLabel } from "./model/propertyLabel.js";
import { GLinkEdge } from "./model/linkEdge.js";
import { GLinkEndNode } from "./model/linkEndNode.js";
import { GLinkEndLabel } from "./model/linkEndLabel.js";

const { injectable } = sharedImport("inversify");
const { ServerLayoutKind, getDefaultMapping, DefaultTypes } = sharedImport("@eclipse-glsp/server");

/**
 * Configuration for model diagrams defining layout behavior and element type mappings.
 * Specifies manual layout with client-side rendering and animated updates.
 */
@injectable()
export class ModelDiagramConfiguration implements DiagramConfiguration {
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

        mapping.set(ModelElementType.NODE_OBJECT, GObjectNode);
        mapping.set(ModelElementType.LABEL_OBJECT_NAME, GObjectNameLabel);
        mapping.set(ModelElementType.LABEL_PROPERTY, GPropertyLabel);
        mapping.set(ModelElementType.EDGE_LINK, GLinkEdge);
        mapping.set(ModelElementType.NODE_LINK_END, GLinkEndNode);
        mapping.set(ModelElementType.LABEL_LINK_END, GLinkEndLabel);

        return mapping;
    }

    /**
     * Returns the shape type hints for model diagram elements.
     *
     * @returns Array of shape type hints
     */
    get shapeTypeHints(): ShapeTypeHint[] {
        return [
            {
                elementTypeId: DefaultTypes.GRAPH,
                repositionable: false,
                deletable: false,
                resizable: false,
                reparentable: false,
                containableElementTypeIds: [ModelElementType.NODE_OBJECT]
            },
            {
                elementTypeId: ModelElementType.NODE_OBJECT,
                repositionable: true,
                deletable: true,
                resizable: true,
                reparentable: false
            }
        ];
    }

    /**
     * Returns the edge type hints for model diagram elements.
     * Does NOT specify the link edge so that this can be handled correctly by the canConnect logic in the editor
     *
     * @returns Array of edge type hints
     */
    get edgeTypeHints(): EdgeTypeHint[] {
        return [];
    }
}
