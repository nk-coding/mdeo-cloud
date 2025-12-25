/**
 * A reference to a generic type parameter.
 * Used when referring to a generic type variable like 'T' or 'U'.
 */
export interface GenericTypeRef {
    /**
     * The name of the generic type parameter
     */
    generic: string;
    /**
     * Whether this type reference can be null.
     * - If true, the type parameter can be null
     * - If false, the type parameter cannot be null
     * - If undefined, keeps the nullability as defined in the generic declaration
     */
    isNullable?: boolean;
}

export interface BaseClassTypeRef {
    /**
     * The type being referenced (a string identifier like "builtin.string")
     */
    type: string;
    /**
     * Optional type arguments for generic types (e.g., {'T': {type: 'String', isNullable: false}})
     */
    typeArgs?: Map<string, ValueType>;
}

/**
 * A reference to a concrete type, optionally with type arguments.
 */
export interface ClassTypeRef extends BaseClassTypeRef {
    /**
     * Whether this type reference can be null
     */
    isNullable: boolean;
}

/**
 * A value type can be either a reference to a concrete type, a reference to a generic type parameter, or a lambda type.
 */
export type ValueType = ClassTypeRef | GenericTypeRef | LambdaType;

/**
 * A method or function parameter.
 */
export interface Parameter {
    /**
     * The name of the parameter
     */
    name: string;
    /**
     * The type of the parameter
     */
    type: ValueType;
}

/**
 * Common interface for functions and lambdas
 */
export interface CallableType {
    /**
     * The return type of the callable. Omit for void callables
     */
    returnType: ValueType;
    /**
     * The parameters of the callable
     */
    parameters: Parameter[];
}

/**
 * A lambda type (anonymous function type)
 */
export interface LambdaType extends CallableType {
    /**
     * Whether this lambda type can be null
     */
    isNullable: boolean;
}

export interface FunctionSignature extends CallableType {
    /**
     * Optional list of generic type parameter names (e.g., ['T', 'U'])
     */
    generics?: string[];
    /**
     * Whether this callable accepts variable arguments (varargs)
     */
    isVarArgs?: boolean;
}

/**
 * A named function or method definition with its signature.
 * This extends CallableType and adds support for generic type parameters.
 */
export interface FunctionType {
    signatures: FunctionSignature[];
}

/**
 * Represents a member (property or function) of a class type.
 */
export interface Member {
    /**
     * The name of the member
     */
    name: string;
    /**
     * Whether this is a property (true) or function/method (false)
     */
    isProperty: boolean;
    /**
     * Whether this property is readonly (only applicable for properties)
     */
    readonly?: boolean;
    /**
     * The type of the member (ValueType for properties, FunctionType for functions)
     */
    type: ValueType | FunctionType;
}

/**
 * A type definition with its members (properties and methods) and type hierarchy.
 */
export interface ClassType {
    /**
     * The name of the type
     */
    name: string;
    /**
     * The package where the type is defined.
     * Forms the fully qualified identifier together with the name (package.name)
     */
    package: string;
    /**
     * A mapping of member names to their definitions (unified properties and methods)
     */
    members: Map<string, Member>;
    /**
     * Optional list of generic type parameter names (e.g., ['T', 'U'])
     */
    generics?: string[];
    /**
     * Optional list of super types that this type extends
     */
    superTypes?: BaseClassTypeRef[];
}
