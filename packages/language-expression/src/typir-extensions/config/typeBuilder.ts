import type {
    ClassType,
    FunctionType,
    FunctionSignature,
    Parameter,
    ValueType,
    ClassTypeRef,
    GenericTypeRef,
    LambdaType,
    BaseClassTypeRef
} from "./type.js";

/**
 * Builder for creating ValueType references with a fluent API.
 * Supports creating class type references, generic type references, and lambda types.
 *
 * @example
 * ```typescript
 * // Simple type reference
 * typeRef('string').nullable().build()
 *
 * // Generic type reference
 * typeRef().generic('T').build()
 *
 * // Lambda type
 * typeRef().lambda()
 *   .param('x', typeRef('number').build())
 *   .returns(typeRef('number').build())
 *   .build()
 * ```
 */
export class ValueTypeBuilder {
    private type?: string;
    private isNullable: boolean = false;
    private genericName?: string;
    private typeArgs?: Map<string, ValueType>;
    private isLambda: boolean = false;
    private parameters: Parameter[] = [];
    private returnType?: ValueType;

    constructor(type?: string) {
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
     * Set this as a generic type reference.
     * @param name The name of the generic type parameter (e.g., 'T')
     */
    generic(name: string): this {
        this.genericName = name;
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
     * Start building a lambda type.
     */
    lambda(): this {
        this.isLambda = true;
        this.parameters = [];
        return this;
    }

    /**
     * Add a parameter to a lambda type.
     * @param name The parameter name
     * @param type The parameter type
     */
    param(name: string, type: ValueType): this {
        this.parameters.push({ name, type });
        return this;
    }

    /**
     * Set the return type for a lambda type.
     * @param type The return type
     */
    returns(type: ValueType): this {
        this.returnType = type;
        return this;
    }

    /**
     * Build the ValueType.
     */
    build(): ValueType {
        if (this.genericName) {
            const result: GenericTypeRef = {
                generic: this.genericName
            };
            if (this.isNullable) {
                result.isNullable = true;
            }
            return result;
        }

        if (this.isLambda) {
            if (!this.returnType) {
                throw new Error("Lambda type must have a return type");
            }
            const result: LambdaType = {
                parameters: this.parameters,
                returnType: this.returnType,
                isNullable: this.isNullable
            };
            return result;
        }

        if (!this.type) {
            throw new Error("Type reference must have a type or be a generic");
        }

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
 * Create a new ValueTypeBuilder.
 * @param type The type identifier (e.g., 'string', 'number')
 */
export function typeRef(type?: string): ValueTypeBuilder {
    return new ValueTypeBuilder(type);
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
 * Builder for creating ClassType definitions with a fluent API.
 *
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
export class ClassTypeBuilder {
    private name: string;
    private package: string;
    private properties: Map<string, ValueType> = new Map();
    private methods: Map<string, FunctionType> = new Map();
    private genericNames?: string[];
    private superTypes: BaseClassTypeRef[] = [];

    constructor(name: string, pkg: string = "builtin") {
        this.name = name;
        this.package = pkg;
    }

    /**
     * Add a property to the class.
     * @param name The property name
     * @param type The property type
     */
    property(name: string, type: ValueType): this {
        this.properties.set(name, type);
        return this;
    }

    /**
     * Add a method to the class.
     * @param name The method name
     * @param builder A function that configures a MethodBuilder
     */
    method(name: string, builder: (method: MethodBuilder) => MethodBuilder): this {
        const methodBuilder = builder(new MethodBuilder());
        this.methods.set(name, methodBuilder.build());
        return this;
    }

    /**
     * Add generic type parameters to the class.
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
     * Build the ClassType.
     */
    build(): ClassType {
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

        return result;
    }
}

/**
 * Create a new ClassTypeBuilder.
 * @param name The name of the class type
 * @param pkg The package where the type is defined (defaults to 'builtin')
 */
export function classType(name: string, pkg: string = "builtin"): ClassTypeBuilder {
    return new ClassTypeBuilder(name, pkg);
}
