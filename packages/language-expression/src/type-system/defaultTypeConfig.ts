/**
 * Mapping of built-in primitive/type names to their corresponding
 * ClassType definitions from the typir type system.
 *
 * This interface is used by the expression language type system to
 * reference the canonical runtime `ClassType` instances for core types
 * (int, long, float, double, string, boolean, void).
 */
import type { BaseClassTypeRef, ClassType } from "../typir-extensions/config/type.js";

/**
 * DefaultTypeConfig describes the set of primitive and core types
 * available in the language-expression package and maps their canonical
 * names to `ClassType` objects.
 */
export interface DefaultTypeConfig {
    /**
     * The root Any type from which all other types derive.
     */
    Any: ClassType;
    /** 
     * 32-bit integer type
     */
    int: ClassType,
    /**
     * 64-bit integer type
     */
    long: ClassType,
    /**
     * single-precision floating point
     */
    float: ClassType,
    /**
     * double-precision floating point
     */
    double: ClassType,
    /**
     * immutable sequence of characters
     */
    string: ClassType,
    /**
     * boolean true/false
     */
    boolean: ClassType,
    /**
     * absence of value (used for procedures/functions with no return)
     */
    void: ClassType,
    /**
     * Additional types to register in the type system
     */
    additionalTypes: ClassType[],
    /**
     * Super types for lambda types
     */
    lambdaSuperTypes: BaseClassTypeRef[]
}