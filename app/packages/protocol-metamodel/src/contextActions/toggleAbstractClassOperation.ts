import type { Operation } from "@eclipse-glsp/protocol";

/**
 * Operation to switch a class between abstract and concrete.
 */
export interface ToggleAbstractClassOperation extends Operation {
    /**
     * The operation kind discriminator.
     */
    kind: "toggleAbstractClass";

    /**
     * The identifier of the class to update.
     */
    classId: string;

    /**
     * The target abstractness for the class.
     *
     * If omitted, the server may derive the target state from current model data.
     */
    makeAbstract?: boolean;

    /**
     * Backward-compatible alias of {@link makeAbstract}.
     *
     * @deprecated Use {@link makeAbstract} instead.
     */
    targetAbstract?: boolean;
}

/**
 * Namespace helpers for {@link ToggleAbstractClassOperation}.
 */
export namespace ToggleAbstractClassOperation {
    /**
     * Discriminator constant for this operation type.
     */
    export const KIND = "toggleAbstractClass";

    /**
     * Parameters used to create a toggle-abstract-class operation.
     */
    export interface Options {
        /**
         * The identifier of the class to update.
         */
        classId: string;

        /**
         * The target abstractness for the class.
         */
        makeAbstract?: boolean;
    }

    /**
     * Create a {@link ToggleAbstractClassOperation}.
     *
     * @param options The operation payload.
     * @returns A new toggle-abstract-class operation.
     */
    export function create(options: Options): ToggleAbstractClassOperation {
        return {
            kind: KIND,
            isOperation: true,
            classId: options.classId,
            makeAbstract: options.makeAbstract,
            targetAbstract: options.makeAbstract
        };
    }

    /**
     * Check whether an operation is a {@link ToggleAbstractClassOperation}.
     *
     * @param operation The operation to inspect.
     * @returns True if the operation is a toggle-abstract-class operation.
     */
    export function is(operation: Operation): operation is ToggleAbstractClassOperation {
        return operation.kind === KIND;
    }
}
