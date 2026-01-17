import type { SerializableExternalReference } from "../../serialization/grammarSerializer.js";
import { InterfaceBuilderFull } from "./builders.js";
import type { Interface } from "./types.js";
import type { AstNode } from "langium";

/**
 * Creates a new interface builder for defining structured AST node types.
 *
 * Interfaces are the primary way to define the shape and properties of language
 * constructs in the grammar. They specify what attributes an AST node should have
 * and how those attributes relate to other parts of the language.
 *
 * @param name The unique name for this interface in the grammar
 * @returns A new InterfaceBuilderFull instance for configuring the interface
 *
 * @example
 * ```typescript
 * // Create a simple interface for a person
 * const Person = createInterface("Person").attrs({
 *     name: String,
 *     age: Number,
 *     isActive: Boolean
 * });
 *
 * // Create an interface with optional and array properties
 * const Employee = createInterface("Employee").attrs({
 *     id: String,
 *     name: String,
 *     department: Optional(String),       // Optional property
 *     skills: [String],                   // Array of strings
 *     manager: Ref(() => Employee)        // Reference to another employee
 * });
 *
 * // Create an interface that extends another
 * const Manager = createInterface("Manager")
 *     .extends(Employee)
 *     .attrs({
 *         team: [Ref(() => Employee)],    // Array of employee references
 *         budget: Number
 *     });
 * ```
 */
export function createInterface(name: string): InterfaceBuilderFull {
    return new InterfaceBuilderFull(name);
}

/**
 * Creates an external reference to an interface from another grammar.
 * Used during language composition to reference interfaces defined in other partial languages.
 *
 * @template T The AST node type that the referenced interface represents
 * @param name The name of the external interface being referenced
 * @returns An interface external reference
 *
 * @example
 * ```typescript
 * // Reference a Person interface from another grammar
 * const ExternalPerson = createExternalInterface<PersonType>("Person");
 * ```
 */
export function createExternalInterface<T extends AstNode>(name: string): Interface<T> {
    const externalRef: SerializableExternalReference = {
        $externalRef: true,
        kind: "Type",
        name
    };

    return {
        name,
        toType: () => externalRef
    };
}
