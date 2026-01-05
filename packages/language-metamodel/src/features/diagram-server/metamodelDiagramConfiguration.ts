import type { DiagramConfiguration, EdgeTypeHint, GModelElementConstructor, ShapeTypeHint } from "@eclipse-glsp/server";
import { sharedImport } from "@mdeo/language-shared";
import {
    MetamodelElementType,
    MetaClassNode,
    MetaClassLabel,
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

    get typeMapping(): Map<string, GModelElementConstructor> {
        const mapping = getDefaultMapping();
        mapping.set(MetamodelElementType.NODE_METACLASS, MetaClassNode);
        mapping.set(MetamodelElementType.LABEL_METACLASS_NAME, MetaClassLabel);
        mapping.set(MetamodelElementType.LABEL_PROPERTY, PropertyLabel);
        mapping.set(MetamodelElementType.LABEL_ASSOCIATION_END, AssociationEndLabel);
        mapping.set(MetamodelElementType.EDGE_INHERITANCE, InheritanceEdge);
        mapping.set(MetamodelElementType.EDGE_ASSOCIATION, AssociationEdge);
        return mapping;
    }

    get shapeTypeHints(): ShapeTypeHint[] {
        return [
            {
                elementTypeId: MetamodelElementType.NODE_METACLASS,
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
                sourceElementTypeIds: [MetamodelElementType.NODE_METACLASS],
                targetElementTypeIds: [MetamodelElementType.NODE_METACLASS]
            },
            {
                elementTypeId: MetamodelElementType.EDGE_ASSOCIATION,
                repositionable: true,
                deletable: true,
                routable: true,
                sourceElementTypeIds: [MetamodelElementType.NODE_METACLASS],
                targetElementTypeIds: [MetamodelElementType.NODE_METACLASS]
            }
        ];
    }
}
