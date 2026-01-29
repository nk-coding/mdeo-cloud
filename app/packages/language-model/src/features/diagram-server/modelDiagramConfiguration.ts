import type { DiagramConfiguration, EdgeTypeHint, GModelElementConstructor, ShapeTypeHint } from "@eclipse-glsp/server";
import { sharedImport } from "@mdeo/language-shared";
import { ModelElementType } from "./model/elementTypes.js";
import { GObjectNode } from "./model/objectNode.js";
import { GObjectNameLabel } from "./model/objectNameLabel.js";
import { GPropertyLabel } from "./model/propertyLabel.js";
import { GLinkEdge } from "./model/linkEdge.js";
import { GLinkSourceLabel } from "./model/linkSourceLabel.js";
import { GLinkTargetLabel } from "./model/linkTargetLabel.js";

const { injectable } = sharedImport("inversify");
const { ServerLayoutKind, getDefaultMapping } = sharedImport("@eclipse-glsp/server");

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
        mapping.set(ModelElementType.LABEL_LINK_SOURCE, GLinkSourceLabel);
        mapping.set(ModelElementType.LABEL_LINK_TARGET, GLinkTargetLabel);

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
     *
     * @returns Array of edge type hints
     */
    get edgeTypeHints(): EdgeTypeHint[] {
        return [
            {
                elementTypeId: ModelElementType.EDGE_LINK,
                repositionable: true,
                deletable: true,
                routable: true,
                sourceElementTypeIds: [ModelElementType.NODE_OBJECT],
                targetElementTypeIds: [ModelElementType.NODE_OBJECT]
            }
        ];
    }
}
