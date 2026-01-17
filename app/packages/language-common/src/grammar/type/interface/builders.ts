import type { InterfaceDeclaration, Interface, CreateInterfaceReturnType } from "./types.js";
import { isOptional } from "./helpers.js";
import { mapType } from "./mappings.js";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import type { GrammarAST } from "langium";

/**
 * Internal function that creates the final interface definition with all configuration applied.
 * This function converts the TypeScript-style interface declaration into Langium's internal
 * grammar AST format.
 *
 * @template T The interface attribute declaration type
 * @template E Array of interfaces this interface extends
 * @param name The unique name for this interface
 * @param attrs The attribute declarations for this interface
 * @param extendsInterfaces Array of interfaces this interface extends from
 * @returns A complete interface definition ready for grammar generation
 */
function createInterfaceInternal<T extends InterfaceDeclaration, E extends Interface<any>[]>(
    name: string,
    attrs: T,
    extendsInterfaces: E
): CreateInterfaceReturnType<T, E> {
    const serializableNode: SerializableGrammarNode<GrammarAST.Interface> = {
        $type: "Interface",
        name,
        attributes: Object.entries(attrs).map(([attrName, attrType]) => ({
            $type: "TypeAttribute",
            name: attrName,
            isOptional: isOptional(attrType),
            type: mapType(attrType)
        })),
        superTypes: extendsInterfaces.map((inter) => () => inter.toType())
    };

    return {
        name,
        toType: () => serializableNode
    } as CreateInterfaceReturnType<T, E>;
}

/**
 * Builder for interfaces that have inheritance configured but no attributes yet.
 * This builder is created when `.extends()` is called and allows defining attributes
 * for an interface that inherits from other interfaces.
 *
 * @template E Array of interfaces this interface extends
 */
export class InterfaceBuilderExtends<E extends Interface<any>[]> {
    /**
     * Creates a builder for an interface with inheritance.
     *
     * @param name The unique name for this interface
     * @param extendsInterfaces Array of interfaces this interface extends
     */
    constructor(
        private readonly name: string,
        private readonly extendsInterfaces: E
    ) {}

    /**
     * Defines the attributes for this interface. The resulting interface will
     * inherit all attributes from the extended interfaces plus the new attributes.
     *
     * @template T The interface attribute declaration type
     * @param attributes Object mapping attribute names to their types
     * @returns A complete interface definition with inheritance and attributes
     *
     * @example
     * ```typescript
     * const Employee = createInterface("Employee")
     *     .extends(Person)  // Inherit from Person interface
     *     .attrs({
     *         employeeId: String,         // Additional attribute
     *         department: String,         // Additional attribute
     *         salary: Optional(Number)    // Optional additional attribute
     *     });
     * ```
     */
    attrs<T extends InterfaceDeclaration>(attributes: T): CreateInterfaceReturnType<T, E> {
        return createInterfaceInternal<T, E>(this.name, attributes, this.extendsInterfaces);
    }
}

/**
 * Full builder for creating interfaces from scratch. This is the entry point
 * for interface creation and allows either defining attributes directly or
 * configuring inheritance first.
 */
export class InterfaceBuilderFull {
    /**
     * Creates a new interface builder.
     *
     * @param name The unique name for this interface
     */
    constructor(private readonly name: string) {}

    /**
     * Defines the attributes for this interface without inheritance.
     *
     * @template T The interface attribute declaration type
     * @param attributes Object mapping attribute names to their types
     * @returns A complete interface definition with the specified attributes
     *
     * @example
     * ```typescript
     * const Person = createInterface("Person").attrs({
     *     name: String,                   // Required string property
     *     age: Number,                    // Required number property
     *     email: Optional(String),        // Optional string property
     *     hobbies: [String],              // Array of strings
     *     spouse: Ref(() => Person)       // Reference to another Person
     * });
     * ```
     */
    attrs<T extends InterfaceDeclaration>(attributes: T): CreateInterfaceReturnType<T, []> {
        return createInterfaceInternal(this.name, attributes, []);
    }

    /**
     * Configures this interface to extend from other interfaces.
     * Interface inheritance allows combining attributes from multiple parent interfaces.
     *
     * @template E Array of interfaces to extend
     * @param interfaces The interfaces this interface should inherit from
     * @returns A builder for defining additional attributes on the extended interface
     *
     * @example
     * ```typescript
     * // Single inheritance
     * const Manager = createInterface("Manager")
     *     .extends(Employee)
     *     .attrs({
     *         teamSize: Number,
     *         budget: Number
     *     });
     *
     * // Multiple inheritance
     * const Developer = createInterface("Developer")
     *     .extends(Employee, TechnicalPerson)
     *     .attrs({
     *         programmingLanguages: [String],
     *         seniority: Union("Junior", "Mid", "Senior")
     *     });
     * ```
     */
    extends<E extends Interface<any>[]>(...interfaces: E): InterfaceBuilderExtends<E> {
        return new InterfaceBuilderExtends<E>(this.name, interfaces);
    }
}
