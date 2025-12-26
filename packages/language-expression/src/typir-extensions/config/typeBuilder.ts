import type {
    ClassType,
    FunctionType,
    FunctionSignature,
    Parameter,
    ValueType,
    ClassTypeRef,
    GenericTypeRef,
    LambdaType,
    BaseClassTypeRef,
    Member
} from "./type.js";

/**
 * Builder for creating ClassType references with a fluent API.
 *
 * @example
 * ```typescript
 * // Simple type reference
 * typeRef('string').nullable().build()
 *
 * // Type reference with type arguments
 * typeRef('Collection').withTypeArgs(new Map([['T', genericTypeRef('T')]])).build()
 * ```
 */
export class ValueTypeBuilder {
    private type: string;
    private isNullable: boolean = false;
    private typeArgs?: Map<string, ValueType>;

    constructor(type: string) {
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
    withTypeArgs(args: Map<string, ValueType>): this {
        this.typeArgs = args;
        return this;
    }

    /**
     * Build the ClassTypeRef.
     */
    build(): ClassTypeRef {
        const result: ClassTypeRef = {
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
 * @param type The type identifier (e.g., 'string', 'number', 'Collection')
 */
export function typeRef(type: string): ValueTypeBuilder {
    return new ValueTypeBuilder(type);
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
 * Builder for creating lambda types with a fluent API.
 *
 * @example
 * ```typescript
 * lambdaType()
 *   .param('x', typeRef('number').build())
 *   .param('y', typeRef('number').build())
 *   .returns(typeRef('number').build())
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
    returns(type: ValueType): LambdaType {
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
 *   .param('x', typeRef('number').build())
 *   .param('y', typeRef('number').build())
 *   .returns(typeRef('number').build())
 *   .build()
 * ```
 */
export class SignatureBuilder {
    private parameters: Parameter[] = [];
    private returnType?: ValueType;
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
    returns(type: ValueType): this {
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
 * Builder for creating function/method types with multiple signatures.
 *
 * @example
 * ```typescript
 * method()
 *   .signature(sig => sig
 *     .param('x', typeRef('number').build())
 *     .returns(typeRef('string').build())
 *   )
 *   .build()
 * ```
 */
export class MethodBuilder {
    private signatures: FunctionSignature[] = [];

    /**
     * Add a signature to this method/function.
     * @param builder A function that configures a SignatureBuilder
     */
    signature(builder: (sig: SignatureBuilder) => SignatureBuilder): this {
        const sig = builder(new SignatureBuilder());
        this.signatures.push(sig.build());
        return this;
    }

    /**
     * Build the FunctionType.
     */
    build(): FunctionType {
        if (this.signatures.length === 0) {
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
    members: Map<string, Member> & { _memberNames?: T };
};

/**
 * Builder for creating ClassType definitions with a fluent API and compile-time member tracking.
 *
 * @template T The type tracking registered member names
 * @example
 * ```typescript
 * classType('string', 'builtin')
 *   .property('length', typeRef('number').build())
 *   .method('upper', m => m
 *     .signature(sig => sig
 *       .returns(typeRef('string').build())
 *     )
 *   )
 *   .extends('any')
 *   .build()
 * ```
 */
export class ClassTypeBuilder<T extends MemberNames = MemberNames> {
    private name: string;
    private package: string;
    protected members: Map<string, Member> = new Map();
    private genericNames?: string[];
    private superTypes: BaseClassTypeRef[] = [];

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
        this.members.set(name, {
            name,
            isProperty: true,
            readonly,
            type
        });
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
        this.members.set(name, {
            name,
            isProperty: false,
            type: methodBuilder.build()
        });
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
     */
    extends(type: string, typeArgs?: Map<string, ValueType>): this {
        const superType: BaseClassTypeRef = { type };
        if (typeArgs) {
            superType.typeArgs = typeArgs;
        }
        this.superTypes.push(superType);
        return this;
    }

    /**
     * Create a new builder based on this type, keeping only specified members.
     *
     * @param memberNames Array of member names to keep
     */
    keepMembers<K extends keyof T & string>(...memberNames: readonly K[]): ClassTypeBuilder<Pick<T, K>> {
        const builder = new ClassTypeBuilder<Pick<T, K>>(this.name, this.package);
        builder.genericNames = this.genericNames;
        builder.superTypes = [...this.superTypes];

        for (const memberName of memberNames) {
            const member = this.members.get(memberName);
            if (member) {
                builder.members.set(memberName, member);
            }
        }

        return builder;
    }

    /**
     * Create a new builder based on this type, omitting specified members.
     *
     * @param memberNames Array of member names to omit
     */
    omitMembers<K extends keyof T & string>(...memberNames: readonly K[]): ClassTypeBuilder<Omit<T, K>> {
        const builder = new ClassTypeBuilder<Omit<T, K>>(this.name, this.package);
        builder.genericNames = this.genericNames;
        builder.superTypes = [...this.superTypes];

        const omitSet = new Set(memberNames);
        for (const [memberName, member] of this.members.entries()) {
            if (!omitSet.has(memberName as K)) {
                builder.members.set(memberName, member);
            }
        }

        return builder;
    }

    /**
     * Build the ClassType with typed members.
     */
    build(): TypedClassType<T> {
        const result: ClassType = {
            name: this.name,
            package: this.package,
            members: this.members
        };

        if (this.genericNames && this.genericNames.length > 0) {
            result.generics = this.genericNames;
        }

        if (this.superTypes.length > 0) {
            result.superTypes = this.superTypes;
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
 */
export function classTypeFrom(existingType: ClassType): ClassTypeBuilder<any> {
    const builder = new ClassTypeBuilder<any>(existingType.name, existingType.package);

    for (const [memberName, member] of existingType.members.entries()) {
        if (member.isProperty) {
            (builder as any).members.set(memberName, member);
        } else {
            (builder as any).members.set(memberName, member);
        }
    }

    if (existingType.generics) {
        builder.generics(...existingType.generics);
    }

    if (existingType.superTypes) {
        for (const superType of existingType.superTypes) {
            builder.extends(superType.type, superType.typeArgs);
        }
    }

    return builder;
}
