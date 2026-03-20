import type { Operation } from "@eclipse-glsp/protocol";
import type { AssociationEndKind } from "../types.js";

/**
 * Operation to change the decoration type at one end of an association.
 *
 * This operation is dispatched when a user selects a new association end type
 * from a context action menu on an association edge.
 */
export interface ChangeAssociationEndOperation extends Operation {
    /**
     * The operation kind discriminator.
     */
    kind: "changeAssociationEnd";

    /**
     * The identifier of the association to update.
     */
    associationId: string;

    /**
     * The association end to update.
     */
    endPosition: "source" | "target";

    /**
     * The new association end type to apply.
     */
    newEndType: AssociationEndKind;
}

/**
 * Namespace helpers for {@link ChangeAssociationEndOperation}.
 */
export namespace ChangeAssociationEndOperation {
    /**
     * Discriminator constant for this operation type.
     */
    export const KIND = "changeAssociationEnd";

    /**
     * Parameters used to create a change-association-end operation.
     */
    export interface CanonicalOptions {
        /**
         * The identifier of the association to update.
         */
        associationId: string;

        /**
         * The association end to update.
         */
        endPosition: "source" | "target";

        /**
         * The new association end type to apply.
         */
        newEndType: AssociationEndKind;
    }

    /**
     * Create a {@link ChangeAssociationEndOperation}.
     *
     * @param options The operation payload.
     * @returns A new change-association-end operation.
     */
    export function create(options: CanonicalOptions): ChangeAssociationEndOperation {
        return {
            kind: KIND,
            isOperation: true,
            associationId: options.associationId,
            endPosition: options.endPosition,
            newEndType: options.newEndType
        };
    }

    /**
     * Check whether an operation is a {@link ChangeAssociationEndOperation}.
     *
     * @param operation The operation to inspect.
     * @returns True if the operation is a change-association-end operation.
     */
    export function is(operation: Operation): operation is ChangeAssociationEndOperation {
        return operation.kind === KIND;
    }
}
