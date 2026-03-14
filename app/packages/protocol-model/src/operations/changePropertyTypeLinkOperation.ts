import type { Operation } from "@eclipse-glsp/protocol";

/**
 * Operation to change the association-end property mapping of a model link.
 */
export interface ChangePropertyTypeLinkOperation extends Operation {
    /**
     * Operation kind discriminator.
     */
    kind: "changePropertyTypeLink";

    /**
     * Identifier of the link edge to update.
     */
    linkId: string;

    /**
     * New source end property name.
     */
    sourceProperty?: string;

    /**
     * New target end property name.
     */
    targetProperty?: string;

    /**
     * End that initiated the change.
     */
    endPosition?: "source" | "target";
}

/**
 * Namespace helpers for change-property-type-link operations.
 */
export namespace ChangePropertyTypeLinkOperation {
    /**
     * Operation kind constant.
     */
    export const KIND = "changePropertyTypeLink";

    /**
     * Payload for creating a change-property-type-link operation.
     */
    export interface Options {
        /**
         * Identifier of the link edge to update.
         */
        linkId: string;

        /**
         * New source end property name.
         */
        sourceProperty?: string;

        /**
         * New target end property name.
         */
        targetProperty?: string;

        /**
         * End that initiated the change.
         */
        endPosition?: "source" | "target";
    }

    /**
     * Creates a change-property-type-link operation.
     *
     * @param options Operation payload
     * @returns Operation instance
     */
    export function create(options: Options): ChangePropertyTypeLinkOperation {
        return {
            kind: KIND,
            isOperation: true,
            linkId: options.linkId,
            sourceProperty: options.sourceProperty,
            targetProperty: options.targetProperty,
            endPosition: options.endPosition
        };
    }

    /**
     * Checks whether an operation is a change-property-type-link operation.
     *
     * @param operation Operation to check
     * @returns True when operation kind matches
     */
    export function is(operation: Operation): operation is ChangePropertyTypeLinkOperation {
        return operation.kind === KIND;
    }
}
