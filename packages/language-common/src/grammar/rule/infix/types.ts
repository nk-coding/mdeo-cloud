/**
 * Defines the associativity behavior for infix operators in the grammar.
 *
 * Associativity determines how operators of the same precedence level are grouped
 * when they appear together in an expression.
 *
 * @example
 * ```typescript
 * // LEFT associativity: a + b + c → (a + b) + c
 * // RIGHT associativity: a = b = c → a = (b = c)
 * ```
 */
export enum Associativity {
    /**
     * Left associativity groups operators from left to right.
     * This is the most common associativity for arithmetic operators.
     *
     * @example `a + b + c` is parsed as `(a + b) + c`
     */
    LEFT = 1,

    /**
     * Right associativity groups operators from right to left.
     * This is common for assignment operators and exponentiation.
     *
     * @example `a = b = c` is parsed as `a = (b = c)`
     */
    RIGHT = 2
}
