import {
    type ClassType,
    type FunctionType,
    FunctionSignature,
    type Parameter,
    type ValueType,
    type ClassTypeRef,
    type GenericTypeRef,
    type LambdaType,
    type BaseClassTypeRef,
    type Property,
    type Method,
    type VoidType,
    type ReturnType
} from "./type.js";

/**
 * Builder for creating ClassType references with a fluent API.
 *
 * @example
 * ```typescript
 * // Simple type reference
 * typeRef('builtin', 'string').nullable().build()
 *
 * // Type reference with type arguments
 * typeRef('builtin', 'Collection').withTypeArgs(new Map([['T', genericTypeRef('T')]])).build()
 * ```
 */
export class ValueTypeBuilder {
    private package: string;
    private type: string;
    private isNullable: boolean = false;
    private typeArgs?: Record<string, ValueType>;

    constructor(pkg: string, type: string) {
        this.package = pkg;
        this.type = type;
    }

    /**
     * Mark this type reference as nullable.
     */
    nullable(): this {
        this.isNullable = true;
        return this;
    }

    /**
     * Add type arguments for a generic type.
     * @param args A map of generic parameter names to their concrete types
     */
    withTypeArgs(args: Record<string, ValueType>): this {
        this.typeArgs = args;
        return this;
    }

    /**
     * Build the ClassTypeRef.
     */
    build(): ClassTypeRef {
        const result: ClassTypeRef = {
            package: this.package,
            type: this.type,
            isNullable: this.isNullable
        };
        if (this.typeArgs) {
            result.typeArgs = this.typeArgs;
        }
        return result;
    }
}

/**
 * Create a new ValueTypeBuilder for a class type reference.
 * @param pkg The package identifier (e.g., 'builtin', 'class', 'enum')
 * @param type The type identifier (e.g., 'string', 'number', 'Collection')
 */
export function typeRef(pkg: string, type: string): ValueTypeBuilder {
    return new ValueTypeBuilder(pkg, type);
}

/**
 * Create a generic type reference.
 * @param name The name of the generic type parameter (e.g., 'T', 'U')
 * @param nullable Whether the generic type is nullable (defaults to false)
 */
export function genericTypeRef(name: string, nullable: boolean = false): GenericTypeRef {
    const result: GenericTypeRef = {
        generic: name
    };
    if (nullable) {
        result.isNullable = true;
    }
    return result;
}

/**
 * Create a void type.
 */
export function voidType(): VoidType {
    return { kind: "void" };
}

/**
 * Builder for creating lambda types with a fluent API.
 *
 * @example
 * ```typescript
 * lambdaType()
 *   .param('x', typeRef('builtin', 'number').build())
 *   .param('y', typeRef('builtin', 'number').build())
 *   .returns(typeRef('builtin', 'number').build())
 * ```
 */
export class LambdaTypeBuilder {
    private parameters: Parameter[] = [];
    private isNullable: boolean = false;

    /**
     * Add a parameter to the lambda type.
     * @param name The parameter name
     * @param type The parameter type
     */
    param(name: string, type: ValueType): this {
        this.parameters.push({ name, type });
        return this;
    }

    /**
     * Mark this lambda type as nullable.
     */
    nullable(): this {
        this.isNullable = true;
        return this;
    }

    /**
     * Set the return type and build the lambda type.
     * @param type The return type
     */
    returns(type: ReturnType): LambdaType {
        const result: LambdaType = {
            parameters: this.parameters,
            returnType: type,
            isNullable: this.isNullable
        };
        return result;
    }
}

/**
 * Create a new LambdaTypeBuilder.
 */
export function lambdaType(): LambdaTypeBuilder {
    return new LambdaTypeBuilder();
}

/**
 * Builder for creating function signatures with a fluent API.
 *
 * @example
 * ```typescript
 * signature()
 *   .param('x', typeRef('builtin', 'number').build())
 *   .param('y', typeRef('builtin', 'number').build())
 *   .returns(typeRef('builtin', 'number').build())
 *   .build()
 * ```
 */
export class SignatureBuilder {
    private parameters: Parameter[] = [];
    private returnType?: ReturnType;
    private genericNames?: string[];
    private isVarArgs: boolean = false;

    /**
     * Add a parameter to the signature.
     * @param name The parameter name
     * @param type The parameter type
     */
    param(name: string, type: ValueType): this {
        this.parameters.push({ name, type });
        return this;
    }

    /**
     * Set the return type for the signature.
     * @param type The return type
     */
    returns(type: ReturnType): this {
        this.returnType = type;
        return this;
    }

    /**
     * Add generic type parameters to the signature.
     * @param names The names of the generic type parameters (e.g., ['T', 'U'])
     */
    generics(...names: string[]): this {
        this.genericNames = names;
        return this;
    }

    /**
     * Mark this signature as accepting variable arguments.
     */
    varArgs(): this {
        this.isVarArgs = true;
        return this;
    }

    /**
     * Build the FunctionSignature.
     */
    build(): FunctionSignature {
        if (!this.returnType) {
            throw new Error("Signature must have a return type");
        }

        const result: FunctionSignature = {
            parameters: this.parameters,
            returnType: this.returnType
        };

        if (this.genericNames && this.genericNames.length > 0) {
            result.generics = this.genericNames;
        }

        if (this.isVarArgs) {
            result.isVarArgs = true;
        }

        return result;
    }
}

/**
 * Create a new SignatureBuilder.
 */
export function signature(): SignatureBuilder {
    return new SignatureBuilder();
}

/**
 * Type for tracking registered signature names in the builder
 * Use an empty object literal type, not Record<string, never>
 */
type SignatureNames = object;

/**
 * Builder for creating function/method types with multiple signatures.
 *
 * @template T The type tracking registered signature names
 * @example
 * ```typescript
 * method()
 *   .signature('default', sig => sig
 *     .param('x', typeRef('builtin', 'number').build())
 *     .returns(typeRef('builtin', 'string').build())
 *   )
 *   .build()
 * ```
 */
export class MethodBuilder<T extends SignatureNames = SignatureNames> {
    private signatures: Record<string, FunctionSignature> = {};

    /**
     * Add a signature to this method/function.
     *
     * @param name The signature name (must not already exist), leave empty to fall back to an empty string representing default
     * @param builder A function that configures a SignatureBuilder
     */
    signature(builder: (sig: SignatureBuilder) => SignatureBuilder): MethodBuilder<Record<string, never>>;
    signature<K extends string>(
        name: EnsureNotMember<T, K>,
        builder: (sig: SignatureBuilder) => SignatureBuilder
    ): MethodBuilder<T & Record<K, never>>;
    signature<K extends string>(
        nameOrBuilder: EnsureNotMember<T, K> | ((sig: SignatureBuilder) => SignatureBuilder),
        builder?: (sig: SignatureBuilder) => SignatureBuilder
    ): MethodBuilder<T & Record<K, never>> {
        const sig = builder
            ? builder(new SignatureBuilder())
            : (nameOrBuilder as (sig: SignatureBuilder) => SignatureBuilder)(new SignatureBuilder());
        const name = builder ? (nameOrBuilder as string) : FunctionSignature.DEFAULT_SIGNATURE;
        this.signatures[name] = sig.build();
        return this as unknown as MethodBuilder<T & Record<K, never>>;
    }

    /**
     * Build the FunctionType.
     */
    build(): FunctionType {
        if (Object.keys(this.signatures).length === 0) {
            throw new Error("Method must have at least one signature");
        }
        return {
            signatures: this.signatures
        };
    }
}

/**
 * Create a new MethodBuilder.
 */
export function method(): MethodBuilder {
    return new MethodBuilder();
}

/**
 * Type for tracking registered member names in the builder
 * Use an empty object literal type, not Record<string, never>
 */
type MemberNames = object;

/**
 * Utility type to ensure a string is not already a member
 * If K is already in T, returns never (causing a type error)
 * Otherwise returns K
 */
type EnsureNotMember<T, K extends string> = K extends keyof T ? never : K;

/**
 * Type representing the built ClassType with specific member names
 */
type TypedClassType<T extends MemberNames> = ClassType & {
    _memberNames?: T;
};

/**
 * Builder for creating ClassType definitions with a fluent API and compile-time member tracking.
 *
 * @template T The type tracking registered member names
 * @example
 * ```typescript
 * classType('string', 'builtin')
 *   .property('length', typeRef('builtin', 'number').build())
 *   .method('upper', m => m
 *     .signature(sig => sig
 *       .returns(typeRef('builtin', 'string').build())
 *     )
 *   )
 *   .extends('any')
 *   .build()
 * ```
 */
export class ClassTypeBuilder<T extends MemberNames = MemberNames> {
    private name: string;
    private package: string;
    properties: Record<string, Property> = {};
    methods: Record<string, Method> = {};
    private genericNames?: string[];
    private superTypes: BaseClassTypeRef[] = [];
    private _isVirtual: boolean = false;

    constructor(name: string, pkg: string = "builtin") {
        this.name = name;
        this.package = pkg;
    }

    /**
     * Add a property to the class.
     *
     * @param name The property name (must not already exist as a member)
     * @param type The property type
     * @param readonly Whether this property is readonly (defaults to false)
     */
    property<K extends string>(
        name: EnsureNotMember<T, K>,
        type: ValueType,
        readonly: boolean = false
    ): ClassTypeBuilder<T & Record<K, never>> {
        this.properties[name] = {
            name,
            isProperty: true,
            readonly,
            type
        };
        return this as unknown as ClassTypeBuilder<T & Record<K, never>>;
    }

    /**
     * Add a method to the class.
     *
     * @param name The method name (must not already exist as a member)
     * @param builder A function that configures a MethodBuilder
     */
    method<K extends string>(
        name: EnsureNotMember<T, K>,
        builder: (method: MethodBuilder) => MethodBuilder
    ): ClassTypeBuilder<T & Record<K, never>> {
        const methodBuilder = builder(new MethodBuilder());
        this.methods[name] = {
            name,
            isProperty: false,
            type: methodBuilder.build()
        };
        return this as unknown as ClassTypeBuilder<T & Record<K, never>>;
    }

    /**
     * Add generic type parameters to the class.
     *
     * @param names The names of the generic type parameters (e.g., ['T', 'U'])
     */
    generics(...names: string[]): this {
        this.genericNames = names;
        return this;
    }

    /**
     * Add a super type that this class extends.
     * @param type The type identifier of the super type
     * @param typeArgs Optional type arguments for the super type
     * @param pkg The package of the super type (defaults to 'builtin')
     */
    extends(type: string, typeArgs?: Record<string, ValueType>, pkg: string = "builtin"): this {
        const superType: BaseClassTypeRef = { package: pkg, type };
        if (typeArgs) {
            superType.typeArgs = typeArgs;
        }
        this.superTypes.push(superType);
        return this;
    }

    /**
     * Clear all super types from this class.
     */
    clearExtends(): this {
        this.superTypes = [];
        return this;
    }

    /**
     * Mark this class type as virtual.
     * A virtual type cannot be used as a value directly (e.g., you cannot declare a variable of this type),
     * but its members can still be accessed (e.g., `myEnum.SOME_VALUE`).
     */
    virtual(): this {
        this._isVirtual = true;
        return this;
    }

    /**
     * Create a new builder based on this type, keeping only specified members.
     *
     * @param memberNames Array of member names to keep
     */
    keepMembers<K extends keyof T & string>(...memberNames: readonly K[]): ClassTypeBuilder<Pick<T, K>> {
        const keepSet = new Set<string>(memberNames as unknown as string[]);
        for (const existingName of Object.keys(this.properties)) {
            if (!keepSet.has(existingName)) {
                delete this.properties[existingName];
            }
        }
        for (const existingName of Object.keys(this.methods)) {
            if (!keepSet.has(existingName)) {
                delete this.methods[existingName];
            }
        }
        return this as ClassTypeBuilder<Pick<T, K>>;
    }

    /**
     * Create a new builder based on this type, omitting specified members.
     *
     * @param memberNames Array of member names to omit
     */
    omitMembers<K extends keyof T & string>(...memberNames: readonly K[]): ClassTypeBuilder<Omit<T, K>> {
        const omitSet = new Set<string>(memberNames as unknown as string[]);
        for (const existingName of Object.keys(this.properties)) {
            if (omitSet.has(existingName)) {
                delete this.properties[existingName];
            }
        }
        for (const existingName of Object.keys(this.methods)) {
            if (omitSet.has(existingName)) {
                delete this.methods[existingName];
            }
        }
        return this as ClassTypeBuilder<Omit<T, K>>;
    }

    /**
     * Build the ClassType with typed members.
     */
    build(): TypedClassType<T> {
        const result: ClassType = {
            name: this.name,
            package: this.package,
            properties: this.properties,
            methods: this.methods
        };

        if (this.genericNames && this.genericNames.length > 0) {
            result.generics = this.genericNames;
        }

        if (this.superTypes.length > 0) {
            result.superTypes = this.superTypes;
        }

        if (this._isVirtual) {
            result.isVirtual = true;
        }

        return result as TypedClassType<T>;
    }
}

/**
 * Create a new ClassTypeBuilder.
 *
 * @param name The name of the class type
 * @param pkg The package where the type is defined (defaults to 'builtin')
 */
export function classType(name: string, pkg: string = "builtin"): ClassTypeBuilder {
    return new ClassTypeBuilder(name, pkg);
}

/**
 * Create a new ClassTypeBuilder based on an existing ClassType.
 * This allows copying a type and then filtering its members.
 * Note: The type information about member names is lost when copying from a runtime ClassType.
 * To preserve type safety, use the builder directly or the typed result from .build().
 *
 * @param existingType The existing class type to copy from
 * @param name Optional new name for the class type (defaults to existing type's name)
 * @param pkg Optional new package for the class type (defaults to existing type's package)
 */
export function classTypeFrom(
    existingType: ClassType,
    name = existingType.name,
    pkg = existingType.package
): ClassTypeBuilder<any> {
    const builder = new ClassTypeBuilder<any>(name, pkg);

    for (const [propName, prop] of Object.entries(existingType.properties)) {
        builder.properties[propName] = prop;
    }
    for (const [methodName, meth] of Object.entries(existingType.methods)) {
        builder.methods[methodName] = meth;
    }

    if (existingType.generics) {
        builder.generics(...existingType.generics);
    }

    if (existingType.superTypes) {
        for (const superType of existingType.superTypes) {
            builder.extends(superType.type, superType.typeArgs, superType.package);
        }
    }

    return builder;
}

/**
 * Builder for creating global function definitions with a fluent API.
 *
 * Global functions are top-level functions that are available in the global scope
 * without requiring a class or object context.
 *
 * @example
 * ```typescript
 * globalFunction('println')
 *   .signature(sig => sig
 *     .param('message', typeRef('builtin', 'string').build())
 *     .returns(voidType())
 *   )
 *   .build()
 * ```
 */
export class GlobalFunctionBuilder {
    private name: string;
    private signatures: Record<string, FunctionSignature> = {};

    constructor(name: string) {
        this.name = name;
    }

    /**
     * Add a signature to this global function.
     *
     * @param builder A function that configures a SignatureBuilder
     */
    signature(builder: (sig: SignatureBuilder) => SignatureBuilder): this;
    /**
     * Add a named signature to this global function.
     *
     * @param name The signature name (overload key)
     * @param builder A function that configures a SignatureBuilder
     */
    signature<K extends string>(name: K, builder: (sig: SignatureBuilder) => SignatureBuilder): this;
    signature<K extends string>(
        nameOrBuilder: K | ((sig: SignatureBuilder) => SignatureBuilder),
        builder?: (sig: SignatureBuilder) => SignatureBuilder
    ): this {
        const sig = builder
            ? builder(new SignatureBuilder())
            : (nameOrBuilder as (sig: SignatureBuilder) => SignatureBuilder)(new SignatureBuilder());
        const name = builder ? (nameOrBuilder as string) : FunctionSignature.DEFAULT_SIGNATURE;
        this.signatures[name] = sig.build();
        return this;
    }

    /**
     * Build the global function as a Method.
     */
    build(): Method {
        if (Object.keys(this.signatures).length === 0) {
            throw new Error("Global function must have at least one signature");
        }
        return {
            name: this.name,
            isProperty: false,
            type: {
                signatures: this.signatures
            }
        };
    }
}

/**
 * Create a new GlobalFunctionBuilder.
 *
 * @param name The name of the global function
 */
export function globalFunction(name: string): GlobalFunctionBuilder {
    return new GlobalFunctionBuilder(name);
}

/**
 * Builder for creating global property definitions with a fluent API.
 *
 * Global properties are top-level constants or values that are available
 * in the global scope without requiring a class or object context.
 *
 * @example
 * ```typescript
 * globalProperty('PI', typeRef('builtin', 'double').build())
 * ```
 */
export function globalProperty(name: string, type: ValueType, readonly: boolean = true): Property {
    return {
        name,
        isProperty: true,
        readonly,
        type
    };
}
