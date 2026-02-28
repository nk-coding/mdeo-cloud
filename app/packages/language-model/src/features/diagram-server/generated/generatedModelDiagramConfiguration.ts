import type { DiagramConfiguration, EdgeTypeHint, GModelElementConstructor, ShapeTypeHint } from "@eclipse-glsp/server";
import { sharedImport } from "@mdeo/language-shared";
import { ModelElementType } from "../model/elementTypes.js";
import { GObjectNode } from "../model/objectNode.js";
import { GObjectNameLabel } from "../model/objectNameLabel.js";
import { GPropertyLabel } from "../model/propertyLabel.js";
import { GLinkEdge } from "../model/linkEdge.js";
import { GLinkEndNode } from "../model/linkEndNode.js";
import { GLinkEndLabel } from "../model/linkEndLabel.js";

const { injectable } = sharedImport("inversify");
const { ServerLayoutKind, getDefaultMapping } = sharedImport("@eclipse-glsp/server");

/**
 * Configuration for generated model diagrams.
 * Read-only diagram configuration with no deletable or repositionable elements.
 */
@injectable()
export class GeneratedModelDiagramConfiguration implements DiagramConfiguration {
    readonly layoutKind = ServerLayoutKind.MANUAL;
    readonly animatedUpdate = true;
    readonly needsClientLayout = true;
    readonly needsServerLayout = false;

    /**
     * Returns the mapping of element type IDs to their corresponding GModelElement constructors.
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
     * Returns shape type hints. Nodes are repositionable but not deletable (read-only).
     */
    get shapeTypeHints(): ShapeTypeHint[] {
        return [
            {
                elementTypeId: ModelElementType.NODE_OBJECT,
                repositionable: true,
                deletable: false,
                resizable: true,
                reparentable: false
            }
        ];
    }

    /**
     * Returns edge type hints. Edges are not deletable (read-only).
     */
    get edgeTypeHints(): EdgeTypeHint[] {
        return [
            {
                elementTypeId: ModelElementType.EDGE_LINK,
                repositionable: true,
                deletable: false,
                routable: true,
                sourceElementTypeIds: [ModelElementType.NODE_OBJECT],
                targetElementTypeIds: [ModelElementType.NODE_OBJECT]
            }
        ];
    }
}
