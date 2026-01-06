import type { DiagramConfiguration, EdgeTypeHint, GModelElementConstructor, ShapeTypeHint } from "@eclipse-glsp/server";
import { sharedImport } from "@mdeo/language-shared";
import {
    MetamodelElementType,
    ClassNode,
    ClassLabel,
    PropertyLabel,
    AssociationEndLabel,
    InheritanceEdge,
    AssociationEdge
} from "./metamodelModelExtensions.js";

const { injectable } = sharedImport("inversify");
const { ServerLayoutKind, getDefaultMapping } = sharedImport("@eclipse-glsp/server");

/**
 * Configuration for metamodel diagrams defining layout behavior and element type mappings.
 * Specifies manual layout with client-side rendering and animated updates.
 */
@injectable()
export class MetamodelDiagramConfiguration implements DiagramConfiguration {
    readonly layoutKind = ServerLayoutKind.MANUAL;
    readonly animatedUpdate = true;
    readonly needsClientLayout = true;
    readonly needsServerLayout = false;

    get typeMapping(): Map<string, GModelElementConstructor> {
        const mapping = getDefaultMapping();
        mapping.set(MetamodelElementType.NODE_CLASS, ClassNode);
        mapping.set(MetamodelElementType.LABEL_CLASS_NAME, ClassLabel);
        mapping.set(MetamodelElementType.LABEL_PROPERTY, PropertyLabel);
        mapping.set(MetamodelElementType.LABEL_ASSOCIATION_END, AssociationEndLabel);
        mapping.set(MetamodelElementType.EDGE_INHERITANCE, InheritanceEdge);
        mapping.set(MetamodelElementType.EDGE_ASSOCIATION, AssociationEdge);
        return mapping;
    }

    get shapeTypeHints(): ShapeTypeHint[] {
        return [
            {
                elementTypeId: MetamodelElementType.NODE_CLASS,
                repositionable: true,
                deletable: true,
                resizable: true,
                reparentable: false
            }
        ];
    }

    get edgeTypeHints(): EdgeTypeHint[] {
        return [
            {
                elementTypeId: MetamodelElementType.EDGE_INHERITANCE,
                repositionable: true,
                deletable: true,
                routable: true,
                sourceElementTypeIds: [MetamodelElementType.NODE_CLASS],
                targetElementTypeIds: [MetamodelElementType.NODE_CLASS]
            },
            {
                elementTypeId: MetamodelElementType.EDGE_ASSOCIATION,
                repositionable: true,
                deletable: true,
                routable: true,
                sourceElementTypeIds: [MetamodelElementType.NODE_CLASS],
                targetElementTypeIds: [MetamodelElementType.NODE_CLASS]
            }
        ];
    }
}
