import { GCompartment, GCompartmentBuilder } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

/**
 * Container node that wraps the bottom compartments of a match node.
 * Contains the where-clause and variable compartments, along with the horizontal
 * dividers between them. This allows the match node view to determine the total
 * height of the compartment block from a single node's bounds.
 */
export class GMatchNodeCompartments extends GCompartment {
    /**
     * Creates a builder for GMatchNodeCompartments instances.
     *
     * @returns A new GMatchNodeCompartmentsBuilder
     */
    static override builder(): GMatchNodeCompartmentsBuilder {
        return new GMatchNodeCompartmentsBuilder(GMatchNodeCompartments).type(
            ModelTransformationElementType.MATCH_NODE_COMPARTMENTS
        );
    }
}

/**
 * Builder for GMatchNodeCompartments instances.
 */
export class GMatchNodeCompartmentsBuilder<
    T extends GMatchNodeCompartments = GMatchNodeCompartments
> extends GCompartmentBuilder<T> {}
