import type { DiagramConfiguration, EdgeTypeHint, GModelElementConstructor, ShapeTypeHint } from "@eclipse-glsp/server";
import { sharedImport } from "@mdeo/language-shared";
import { MetamodelElementType } from "./model/elementTypes.js";
import { GClassNode } from "./model/classNode.js";
import { GClassLabel } from "./model/classLabel.js";
import { GPropertyLabel } from "./model/propertyLabel.js";
import { GInheritanceEdge } from "./model/inheritanceEdge.js";
import { GAssociationEdge } from "./model/associationEdge.js";
import { GAssociationMultiplicityLabel } from "./model/associationMultiplicityLabel.js";
import { GAssociationPropertyLabel } from "./model/associationPropertyLabel.js";
import { GAssociationPropertyNode } from "./model/associationPropertyNode.js";
import { GAssociationMultiplicityNode } from "./model/associationMultiplicityNode.js";

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
        mapping.set(MetamodelElementType.NODE_CLASS, GClassNode);
        mapping.set(MetamodelElementType.NODE_ASSOCIATION_PROPERTY, GAssociationPropertyNode);
        mapping.set(MetamodelElementType.NODE_ASSOCIATION_MULTIPLICITY, GAssociationMultiplicityNode);

        mapping.set(MetamodelElementType.LABEL_CLASS_NAME, GClassLabel);
        mapping.set(MetamodelElementType.LABEL_PROPERTY, GPropertyLabel);
        mapping.set(MetamodelElementType.LABEL_ASSOCIATION_MULTIPLICITY, GAssociationMultiplicityLabel);
        mapping.set(MetamodelElementType.LABEL_ASSOCIATION_PROPERTY, GAssociationPropertyLabel);

        mapping.set(MetamodelElementType.EDGE_INHERITANCE, GInheritanceEdge);
        mapping.set(MetamodelElementType.EDGE_ASSOCIATION, GAssociationEdge);
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
