import type { Operation } from "@eclipse-glsp/protocol";
import type { NewLabelOperation } from "@mdeo/protocol-common";

/**
 * Operation to add a where clause to a match node.
 */
export interface AddWhereClauseOperation extends NewLabelOperation {
    /**
     * Operation kind discriminator.
     */
    kind: "addWhereClause";

    /**
     * Identifier of the match node to add the where clause to.
     */
    matchNodeId: string;
}

/**
 * Namespace helpers for add-where-clause operations.
 */
export namespace AddWhereClauseOperation {
    /**
     * Operation kind constant.
     */
    export const KIND = "addWhereClause";

    /**
     * Payload for creating an add-where-clause operation.
     */
    export interface Options {
        /**
         * Identifier of the match node to add the where clause to.
         */
        matchNodeId: string;

        /**
         * The full edited where-clause text (e.g. {@code where a.b == c.d}).
         * The server reads this verbatim and inserts it into the source file.
         */
        labelText?: string;
    }

    /**
     * Creates an add-where-clause operation.
     *
     * @param options Operation payload
     * @returns Operation instance
     */
    export function create(options: Options): AddWhereClauseOperation {
        return {
            kind: KIND,
            isOperation: true,
            matchNodeId: options.matchNodeId,
            parentElementId: options.matchNodeId,
            labelText: options.labelText ?? ""
        };
    }

    /**
     * Checks whether an operation is an add-where-clause operation.
     *
     * @param operation Operation to check
     * @returns True when operation kind matches
     */
    export function is(operation: Operation): operation is AddWhereClauseOperation {
        return operation.kind === KIND;
    }
}
