import type { Operation } from "@eclipse-glsp/protocol";

/**
 * Match node kinds that can be converted through context actions.
 */
export type MatchNodeConversionKind = "match" | "if-match" | "for-match" | "while-match" | "until-match";

/**
 * Operation to convert an existing match node/statement into another match form.
 */
export interface ConvertMatchNodeOperation extends Operation {
    /**
     * Operation kind discriminator.
     */
    kind: "convertMatchNode";

    /**
     * Identifier of the match node to convert.
     */
    nodeId: string;

    /**
     * Target match kind.
     */
    targetKind: MatchNodeConversionKind;
}

/**
 * Namespace helpers for convert-match-node operations.
 */
export namespace ConvertMatchNodeOperation {
    /**
     * Operation kind constant.
     */
    export const KIND = "convertMatchNode";

    /**
     * Payload for creating a convert-match-node operation.
     */
    export interface Options {
        /**
         * Identifier of the match node to convert.
         */
        nodeId: string;

        /**
         * Target match kind.
         */
        targetKind: MatchNodeConversionKind;
    }

    /**
     * Creates a convert-match-node operation.
     *
     * @param options Operation payload
     * @returns Operation instance
     */
    export function create(options: Options): ConvertMatchNodeOperation {
        return {
            kind: KIND,
            isOperation: true,
            nodeId: options.nodeId,
            targetKind: options.targetKind
        };
    }

    /**
     * Checks whether an operation is a convert-match-node operation.
     *
     * @param operation Operation to check
     * @returns True when operation kind matches
     */
    export function is(operation: Operation): operation is ConvertMatchNodeOperation {
        return operation.kind === KIND;
    }
}
