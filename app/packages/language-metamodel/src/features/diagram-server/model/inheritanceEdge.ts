import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "@mdeo/protocol-metamodel";

/**
 * Edge representing an inheritance relationship (extends).
 */
export class GInheritanceEdge extends GEdge {
    /**
     * Creates a builder for GInheritanceEdge instances.
     *
     * @returns A new GInheritanceEdgeBuilder
     */
    static builder(): GInheritanceEdgeBuilder {
        return new GInheritanceEdgeBuilder(GInheritanceEdge).type(MetamodelElementType.EDGE_INHERITANCE);
    }
}

/**
 * Builder for GInheritanceEdge instances.
 * Provides fluent API for constructing inheritance edges.
 */
export class GInheritanceEdgeBuilder<E extends GInheritanceEdge = GInheritanceEdge> extends GEdgeBuilder<E> {}
