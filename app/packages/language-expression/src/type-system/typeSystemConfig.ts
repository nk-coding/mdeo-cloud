/**
 * Mapping of built-in primitive/type names to their corresponding
 * ClassType definitions from the typir type system.
 *
 * This interface is used by the expression language type system to
 * reference the canonical runtime `ClassType` instances for core types
 * (int, long, float, double, string, boolean, void).
 */
import type { BaseClassTypeRef, ClassType, ClassTypeRef, ValueType } from "../typir-extensions/config/type.js";

/**
 * DefaultTypeNames enumerates the canonical names of primitive and core types
 * used in the language-expression type system.
 */
export enum DefaultTypeNames {
    Any = "Any",
    Int = "int",
    Long = "long",
    Float = "float",
    Double = "double",
    String = "string",
    Boolean = "boolean",
    Iterable = "Iterable"
}

/**
 * DefaultTypeConfig describes the set of primitive and core types
 * available in the language-expression package and maps their canonical
 * names to `ClassType` objects.
 */
export interface TypeSystemConfig {
    /**
     * The root Any type from which all other types derive.
     */
    [DefaultTypeNames.Any]: ClassType;
    /**
     * 32-bit integer type
     */
    [DefaultTypeNames.Int]: ClassType;
    /**
     * 64-bit integer type
     */
    [DefaultTypeNames.Long]: ClassType;
    /**
     * single-precision floating point
     */
    [DefaultTypeNames.Float]: ClassType;
    /**
     * double-precision floating point
     */
    [DefaultTypeNames.Double]: ClassType;
    /**
     * immutable sequence of characters
     */
    [DefaultTypeNames.String]: ClassType;
    /**
     * boolean true/false
     */
    [DefaultTypeNames.Boolean]: ClassType;
    /**
     * The iterable type for types which can be iterated, should have exactly one generic type param
     */
    [DefaultTypeNames.Iterable]: ClassType;
    /**
     * Additional types to register in the type system
     */
    additionalTypes: ClassType[];
    /**
     * Super types for lambda types
     */
    lambdaSuperTypes: BaseClassTypeRef[];
    /**
     * Creates a List type reference for the given element type.
     *
     * @param elementType The type of elements in the list
     * @returns A ClassTypeRef representing List<elementType>
     */
    createListType(elementType: ValueType): ClassTypeRef;
}
