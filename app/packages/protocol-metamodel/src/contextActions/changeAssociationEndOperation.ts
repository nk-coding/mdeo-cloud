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

    /**
     * Backward-compatible alias of {@link associationId}.
     *
     * @deprecated Use {@link associationId} instead.
     */
    edgeId: string;

    /**
     * Backward-compatible alias of {@link endPosition}.
     *
     * @deprecated Use {@link endPosition} instead.
     */
    end: "source" | "target";

    /**
     * Backward-compatible alias of {@link newEndType}.
     *
     * @deprecated Use {@link newEndType} instead.
     */
    newKind: AssociationEndKind;
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
     * Legacy operation payload shape for backward compatibility.
     */
    export interface LegacyOptions {
        /**
         * Legacy association identifier.
         */
        edgeId: string;

        /**
         * Legacy end selector.
         */
        end: "source" | "target";

        /**
         * Legacy association end type.
         */
        newKind: AssociationEndKind;
    }

    /**
     * Supported payload for creating this operation.
     */
    export type Options = CanonicalOptions | LegacyOptions;

    /**
     * Create a {@link ChangeAssociationEndOperation}.
     *
     * @param options The operation payload.
     * @returns A new change-association-end operation.
     */
    export function create(options: Options): ChangeAssociationEndOperation {
        const associationId = "associationId" in options ? options.associationId : options.edgeId;
        const endPosition = "endPosition" in options ? options.endPosition : options.end;
        const newEndType = "newEndType" in options ? options.newEndType : options.newKind;

        return {
            kind: KIND,
            isOperation: true,
            associationId,
            endPosition,
            newEndType,
            edgeId: associationId,
            end: endPosition,
            newKind: newEndType
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
