import type { Operation } from "@eclipse-glsp/protocol";
import type { NewLabelOperation } from "@mdeo/protocol-common";

/**
 * Operation to add a multiplicity label for an association end.
 *
 * This is a high-complexity context action that typically creates both
 * the multiplicity model node and its editable label.
 */
export interface AddAssociationMultiplicityOperation extends NewLabelOperation {
    /**
     * The operation kind discriminator.
     */
    kind: "addAssociationMultiplicity";

    /**
     * The identifier of the association where multiplicity is added.
     */
    associationId: string;

    /**
     * The association end that receives the multiplicity.
     */
    endPosition: "source" | "target";

    /**
     * The multiplicity value to apply.
     */
    multiplicityValue: string;

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
     * Backward-compatible alias of {@link multiplicityValue}.
     *
     * @deprecated Use {@link multiplicityValue} instead.
     */
    initialValue: string;

    /**
     * Backward-compatible alias of {@link NewLabelOperation.parentElementId}.
     *
     * @deprecated Use {@link NewLabelOperation.parentElementId} instead.
     */
    parentId: string;
}

/**
 * Namespace helpers for {@link AddAssociationMultiplicityOperation}.
 */
export namespace AddAssociationMultiplicityOperation {
    /**
     * Discriminator constant for this operation type.
     */
    export const KIND = "addAssociationMultiplicity";

    /**
     * Canonical payload for creating an add-association-multiplicity operation.
     */
    export interface CanonicalOptions {
        /**
         * The identifier of the association where multiplicity is added.
         */
        associationId: string;

        /**
         * The association end that receives the multiplicity.
         */
        endPosition: "source" | "target";

        /**
         * The multiplicity value to apply.
         */
        multiplicityValue?: string;
    }

    /**
     * Legacy payload for creating an add-association-multiplicity operation.
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
         * Legacy multiplicity value.
         */
        initialValue?: string;

        /**
         * Legacy parent identifier.
         */
        parentId?: string;
    }

    /**
     * Supported payload for creating this operation.
     */
    export type Options = CanonicalOptions | LegacyOptions;

    /**
     * Create an {@link AddAssociationMultiplicityOperation}.
     *
     * @param options The operation payload.
     * @returns A new add-association-multiplicity operation.
     */
    export function create(options: Options): AddAssociationMultiplicityOperation {
        const associationId = "associationId" in options ? options.associationId : options.edgeId;
        const endPosition = "endPosition" in options ? options.endPosition : options.end;
        const value =
            "associationId" in options
                ? ((options as CanonicalOptions).multiplicityValue ?? "")
                : ((options as LegacyOptions).initialValue ?? "");
        const parentId =
            "parentId" in options && (options as LegacyOptions).parentId
                ? (options as LegacyOptions).parentId!
                : associationId;

        return {
            kind: KIND,
            isOperation: true,
            associationId,
            endPosition,
            multiplicityValue: value,
            parentElementId: associationId,
            labelText: value,
            edgeId: associationId,
            end: endPosition,
            initialValue: value,
            parentId
        };
    }

    /**
     * Check whether an operation is an {@link AddAssociationMultiplicityOperation}.
     *
     * @param operation The operation to inspect.
     * @returns True if the operation is an add-association-multiplicity operation.
     */
    export function is(operation: Operation): operation is AddAssociationMultiplicityOperation {
        return operation.kind === KIND;
    }
}
