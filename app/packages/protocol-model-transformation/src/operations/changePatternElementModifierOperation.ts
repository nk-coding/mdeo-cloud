import type { Operation } from "@eclipse-glsp/protocol";
import type { PatternModifierKind } from "../types.js";

/**
 * Operation to change the modifier of a pattern node or pattern link.
 */
export interface ChangePatternElementModifierOperation extends Operation {
    /**
     * Operation kind discriminator.
     */
    kind: "changePatternElementModifier";

    /**
     * Identifier of the element to update.
     */
    elementId: string;

    /**
     * New modifier to apply.
     */
    modifier: PatternModifierKind;
}

/**
 * Namespace helpers for change-pattern-element-modifier operations.
 */
export namespace ChangePatternElementModifierOperation {
    /**
     * Operation kind constant.
     */
    export const KIND = "changePatternElementModifier";

    /**
     * Payload for creating a change-pattern-element-modifier operation.
     */
    export interface Options {
        /**
         * Identifier of the element to update.
         */
        elementId: string;

        /**
         * New modifier to apply.
         */
        modifier: PatternModifierKind;
    }

    /**
     * Creates a change-pattern-element-modifier operation.
     *
     * @param options Operation payload
     * @returns Operation instance
     */
    export function create(options: Options): ChangePatternElementModifierOperation {
        return {
            kind: KIND,
            isOperation: true,
            elementId: options.elementId,
            modifier: options.modifier
        };
    }

    /**
     * Checks whether an operation is a change-pattern-element-modifier operation.
     *
     * @param operation Operation to check
     * @returns True when operation kind matches
     */
    export function is(operation: Operation): operation is ChangePatternElementModifierOperation {
        return operation.kind === KIND;
    }
}
