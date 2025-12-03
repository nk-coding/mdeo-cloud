import type { Type, UnionTypes } from "./types.js";
import type { Interface } from "../interface/types.js";

/**
 * Builder for creating union types that combine multiple alternative type definitions.
 * Union types allow modeling language constructs that can take one of several different forms.
 */
export class TypeBuilder {
    /**
     * Creates a new type builder.
     * 
     * @param name The unique name for this union type
     */
    constructor(readonly name: string) {}

    /**
     * Defines the alternative types that this union type can represent.
     * The resulting union type will accept any AST node that matches one of the provided types.
     * 
     * @template E Array of type or interface definitions to union together
     * @param types The alternative types that this union can represent
     * @returns A complete union type definition
     * 
     * @example
     * ```typescript
     * // Create a union of different literal types
     * const Literal = createType("Literal").types(
     *     StringLiteral,    // "hello world"
     *     NumberLiteral,    // 42
     *     BooleanLiteral    // true/false
     * );
     * 
     * // Create a union of expression types
     * const Expression = createType("Expression").types(
     *     Literal,              // Any literal value
     *     BinaryExpression,     // a + b, a * b, etc.
     *     UnaryExpression,      // -x, !x, etc.
     *     FunctionCall,         // func(args)
     *     VariableReference     // variable names
     * );
     * 
     * // The resulting type is a union of all alternative types
     * type ExpressionType = ASTType<typeof Expression>;
     * // ExpressionType = LiteralType | BinaryExpressionType | UnaryExpressionType | FunctionCallType | VariableReferenceType
     * ```
     */
    types<E extends (Type<any> | Interface<any>)[]>(
        ...types: E
    ): Type<UnionTypes<E>> {
        return {
            $type: "Type",
            name: this.name,
            type: {
                $type: "UnionType",
                types: types.map((type) => ({
                    $type: "SimpleType",
                    typeRef: () => type,
                })),
            },
        } as Type<UnionTypes<E>>;
    }
}
