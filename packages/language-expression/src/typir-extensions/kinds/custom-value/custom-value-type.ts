import type { Type as TypirType, TypeDetails, TypirProblem, TypirSpecifics } from "typir";
import type { CustomClassType } from "../custom-class/custom-class-type.js";
import type { CustomFunctionType } from "../custom-function/custom-function-type.js";
import type { ExtendedTypirServices, Provider } from "../../service/extendedTypirServices.js";
import type { BaseClassTypeRef, FunctionType, ValueType } from "../../config/type.js";

/**
 * Type details for custom value types.
 * Contains type arguments and super types that apply to both class and lambda types.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface CustomValueTypeDetail<Specifics extends TypirSpecifics> extends TypeDetails<Specifics> {
    /**
     * Map of generic type parameter names to their resolved concrete types
     */
    typeArgs: Map<string, CustomValueType>;

    /**
     * References to super types that this type extends
     */
    superTypes: BaseClassTypeRef[];
}

/**
 * Constructor interface for custom value types.
 * Represents the class itself (static side), not instances.
 *
 * @template T The specific type details extending CustomValueTypeDetail
 */
export interface CustomValueTypeConstructor {
    new <T extends CustomValueTypeDetail<TypirSpecifics> = CustomValueTypeDetail<TypirSpecifics>>(
        identifier: string,
        details: T,
        services: ExtendedTypirServices<TypirSpecifics>,
        isNullable: boolean
    ): CustomValueType<T>;
}

/**
 * Interface for custom value types (classes and lambdas).
 * Provides property and method lookup with inheritance support.
 *
 * @template T The specific type details extending CustomValueTypeDetail
 */
export interface CustomValueType<
    T extends CustomValueTypeDetail<TypirSpecifics> = CustomValueTypeDetail<TypirSpecifics>
> extends TypirType {
    /**
     * Resolved super class types that this type extends
     */
    readonly superClasses: CustomClassType[];

    /**
     * The type details including type arguments and super types
     */
    readonly details: T;

    /**
     * The extended Typir services available for this type
     */
    readonly services: ExtendedTypirServices<TypirSpecifics>;

    /**
     * Get the nullable version of this type
     */
    get asNullable(): CustomValueType;

    /**
     * Get the non-nullable version of this type
     */
    get asNonNullable(): CustomValueType;

    /**
     * Whether this type can be null
     */
    get isNullable(): boolean;

    /**
     * Get the type of a property by name, including inherited properties.
     * Uses caching to avoid repeated lookups.
     *
     * @param fieldName The name of the property to look up
     * @returns The property's custom value type, or undefined if not found
     */
    getProperty(fieldName: string): CustomValueType | undefined;

    /**
     * Get the type of a method by name, including inherited methods.
     * Uses caching to avoid repeated lookups.
     *
     * @param methodName The name of the method to look up
     * @returns The method's custom function type, or undefined if not found
     */
    getMethod(methodName: string): CustomFunctionType | undefined;

    /**
     * Get the property type for a specific property name (without inheritance).
     * Must be implemented by subclasses to provide local property lookup.
     *
     * @param propertyName The name of the property
     * @returns The property's value type, or undefined if not found
     */
    getLocalPropertyType(propertyName: string): ValueType | undefined;

    /**
     * Get the method type for a specific method name (without inheritance).
     * Must be implemented by subclasses to provide local method lookup.
     *
     * @param methodName The name of the method
     * @returns The method's function type, or undefined if not found
     */
    getLocalMethodType(methodName: string): FunctionType | undefined;

    /**
     * Register this type as a subtype of its super classes.
     * Called during initialization to establish the type hierarchy.
     */
    registerSubtypes(): void;
}

export const CustomValueTypeProvider: Provider<CustomValueTypeConstructor> = (services) => {
    const { Type } = services.context.typir;

    /**
     * Abstract base class for custom value types (classes and lambdas).
     *
     * @template T The specific type details extending CustomValueTypeDetail
     */
    class CustomValueTypeImplementation<
            T extends CustomValueTypeDetail<TypirSpecifics> = CustomValueTypeDetail<TypirSpecifics>
        >
        extends Type
        implements CustomValueType<T>
    {
        readonly superClasses: CustomClassType[];

        private readonly cachedProperties: Map<string, CustomValueType> = new Map();

        private readonly cachedMethods: Map<string, CustomFunctionType> = new Map();

        get asNullable(): CustomValueType {
            throw new Error("Method not implemented.");
        }

        get asNonNullable(): CustomValueType {
            throw new Error("Method not implemented.");
        }

        get isNullable(): boolean {
            throw new Error("Method not implemented.");
        }

        /**
         * Creates a new custom value type.
         *
         * @param identifier The unique identifier for this type
         * @param details The type details including type arguments and super types
         * @param services Extended Typir services for type operations
         * @param isNullable Whether this type is nullable
         */
        constructor(
            identifier: string,
            readonly details: T,
            readonly services: ExtendedTypirServices<TypirSpecifics>,
            isNullable: boolean
        ) {
            super(identifier, details);
            this.superClasses = (details.superTypes || []).map(
                (superTypeRef) =>
                    services.TypeDefinitions.resolveCustomClassOrLambdaType(
                        {
                            ...superTypeRef,
                            isNullable
                        },
                        details.typeArgs
                    ) as CustomClassType
            );
        }

        getLocalPropertyType(propertyName: string): ValueType | undefined {
            throw new Error("Method not implemented.");
        }

        getLocalMethodType(methodName: string): FunctionType | undefined {
            throw new Error("Method not implemented.");
        }

        registerSubtypes() {
            for (const superClass of this.superClasses) {
                this.services.Subtype.markAsSubType(this, superClass);
            }
            if (!this.isNullable) {
                this.services.Subtype.markAsSubType(this, this.asNullable as unknown as TypirType);
            }
        }

        getProperty(fieldName: string): CustomValueType | undefined {
            if (this.cachedProperties.has(fieldName)) {
                return this.cachedProperties.get(fieldName);
            }
            const propertyType = this.getLocalPropertyType(fieldName);
            if (propertyType == undefined) {
                for (const superClass of this.superClasses) {
                    const superProperty = superClass.getProperty(fieldName);
                    if (superProperty != undefined) {
                        this.cachedProperties.set(fieldName, superProperty);
                        return superProperty;
                    }
                }
                return undefined;
            }
            const resolvedType = this.services.TypeDefinitions.resolveCustomClassOrLambdaType(
                propertyType,
                this.details.typeArgs as Map<string, CustomValueType>
            );
            this.cachedProperties.set(fieldName, resolvedType);
            return resolvedType;
        }

        getMethod(methodName: string): CustomFunctionType | undefined {
            if (this.cachedMethods.has(methodName)) {
                return this.cachedMethods.get(methodName);
            }
            const methodType = this.getLocalMethodType(methodName);
            if (methodType == undefined) {
                for (const superClass of this.superClasses) {
                    const superMethod = superClass.getMethod(methodName);
                    if (superMethod != undefined) {
                        this.cachedMethods.set(methodName, superMethod);
                        return superMethod;
                    }
                }
                return undefined;
            }
            const resolvedMethodType = this.services.TypeDefinitions.resolveCustomFunctionType(
                methodType,
                `${this.getIdentifier()}.${methodName}`,
                this.details.typeArgs as Map<string, CustomValueType>
            );
            this.cachedMethods.set(methodName, resolvedMethodType);
            return resolvedMethodType;
        }

        override getName(): string {
            throw new Error("Method not implemented.");
        }

        override getUserRepresentation(): string {
            throw new Error("Method not implemented.");
        }

        override analyzeTypeEqualityProblems(otherType: TypirType): TypirProblem[] {
            throw new Error("Method not implemented.");
        }
    }
    return CustomValueTypeImplementation;
};
