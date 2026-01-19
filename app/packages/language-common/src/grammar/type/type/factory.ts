import type { SerializableExternalReference } from "../../serialization/grammarSerializer.js";
import { TypeBuilder } from "./builders.js";
import type { Type } from "./types.js";
import type { AstNode } from "langium";

/**
 * Creates a new type builder for defining union types in the grammar.
 *
 * Union types allow defining language constructs that can be one of several
 * alternative forms. They are useful for modeling polymorphic language elements
 * where different syntax patterns should all be treated as variants of the same
 * conceptual type.
 *
 * @param name The unique name for this union type in the grammar
 * @returns A new TypeBuilder instance for configuring the union type
 *
 * @example
 * ```typescript
 * // Create a union type for different expression forms
 * const Expression = createType("Expression").types(
 *     NumberLiteral,      // e.g., 42
 *     StringLiteral,      // e.g., "hello"
 *     BinaryExpression,   // e.g., a + b
 *     FunctionCall        // e.g., func()
 * );
 *
 * // Create a union type for statement variants
 * const Statement = createType("Statement").types(
 *     IfStatement,        // if (condition) { ... }
 *     WhileStatement,     // while (condition) { ... }
 *     AssignmentStatement // variable = value
 * );
 *
 * // The resulting type can be used in parser rules
 * type ExpressionType = ASTType<typeof Expression>;
 * // ExpressionType = NumberLiteralType | StringLiteralType | BinaryExpressionType | FunctionCallType
 * ```
 */
export function createType(name: string): TypeBuilder {
    return new TypeBuilder(name);
}

/**
 * Creates an external reference to a type from another grammar.
 * Used during language composition to reference types defined in other partial languages.
 *
 * @template T The AST node union type that the referenced type represents
 * @param name The name of the external type being referenced
 * @returns A type external reference
 *
 * @example
 * ```typescript
 * // Reference an Expression type from another grammar
 * const ExternalExpression = createExternalType<ExpressionType>("Expression");
 * ```
 */
export function createExternalType<T extends AstNode>(name: string): Type<T> {
    const externalRef: SerializableExternalReference = {
        $externalRef: true,
        kind: "Type",
        name
    };

    return {
        $type: "Type",
        name,
        toType: () => externalRef
    };
}
