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
 * A void type reference.
 * Used for functions/methods that don't return a value.
 */
export interface VoidType {
    /**
     * Marker to distinguish void type from other types
     */
    readonly kind: 'void';
}

/**
 * A return type can be either a value type or void.
 */
export type ReturnType = ValueType | VoidType;

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
     * The return type of the callable. Use VoidType for void callables
     */
    returnType: ReturnType;
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

/**
 * Namespace containing type guard for ClassTypeRef
 */
export namespace ClassTypeRef {
    /**
     * Type guard to check if a ReturnType is a ClassTypeRef.
     * A ClassTypeRef has a 'type' property (which is a string identifier).
     * 
     * @param type The type to check
     * @returns true if the type is a ClassTypeRef
     */
    export function is(type: ReturnType): type is ClassTypeRef {
        return typeof type === 'object' && type !== null && 'type' in type;
    }
}

/**
 * Namespace containing type guard for GenericTypeRef
 */
export namespace GenericTypeRef {
    /**
     * Type guard to check if a ReturnType is a GenericTypeRef.
     * A GenericTypeRef has a 'generic' property (which is a string identifier).
     * 
     * @param type The type to check
     * @returns true if the type is a GenericTypeRef
     */
    export function is(type: ReturnType): type is GenericTypeRef {
        return typeof type === 'object' && type !== null && 'generic' in type;
    }
}

/**
 * Namespace containing type guard for LambdaType
 */
export namespace LambdaType {
    /**
     * Type guard to check if a ReturnType is a LambdaType.
     * A LambdaType has 'parameters' and 'returnType' properties.
     * 
     * @param type The type to check
     * @returns true if the type is a LambdaType
     */
    export function is(type: ReturnType): type is LambdaType {
        return typeof type === 'object' && type !== null && 'parameters' in type;
    }
}

/**
 * Namespace containing type guard for VoidType
 */
export namespace VoidType {
    /**
     * Type guard to check if a ReturnType is a VoidType.
     * A VoidType has a 'kind' property with the value 'void'.
     * 
     * @param type The type to check
     * @returns true if the type is a VoidType
     */
    export function is(type: ReturnType): type is VoidType {
        return typeof type === 'object' && type !== null && 'kind' in type && type.kind === 'void';
    }
}
