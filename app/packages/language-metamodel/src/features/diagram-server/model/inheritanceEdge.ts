import { GEdge, GEdgeBuilder } from "@mdeo/language-shared";
import { MetamodelElementType } from "./elementTypes.js";

/**
 * Edge representing an inheritance relationship (extends).
 */
export class InheritanceEdge extends GEdge {
    /**
     * Creates a builder for InheritanceEdge instances.
     *
     * @returns A new InheritanceEdgeBuilder
     */
    static override builder(): InheritanceEdgeBuilder {
        return new InheritanceEdgeBuilder(InheritanceEdge).type(MetamodelElementType.EDGE_INHERITANCE);
    }
}

/**
 * Builder for InheritanceEdge instances.
 * Provides fluent API for constructing inheritance edges.
 */
export class InheritanceEdgeBuilder<E extends InheritanceEdge = InheritanceEdge> extends GEdgeBuilder<E> {}
