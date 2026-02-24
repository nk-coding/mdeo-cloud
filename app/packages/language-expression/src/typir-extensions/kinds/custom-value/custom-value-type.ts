import type { Type as TypirType, TypeDetails, TypirProblem, TypirSpecifics } from "typir";
import type { CustomClassType } from "../custom-class/custom-class-type.js";
import type { ExtendedTypirServices } from "../../service/extendedTypirServices.js";
import type { BaseClassTypeRef, Property, Method, ValueType } from "../../config/type.js";
import { sharedImport } from "@mdeo/language-shared";

const { Type } = sharedImport("typir");

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
        isNullable: boolean,
        superTypes: BaseClassTypeRef[]
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
     * All super classes including inherited ones
     */
    readonly allSuperClasses: CustomClassType[];

    /**
     * The type details including type arguments and super types
     */
    readonly details: T;

    /**
     * The type definition associated with this value type
     */
    readonly definition: ValueType;

    /**
     * The extended Typir services available for this type
     */
    readonly services: ExtendedTypirServices<TypirSpecifics>;

    /**
     * Get the nullable version of this type
     */
    readonly asNullable: CustomValueType;

    /**
     * Get the non-nullable version of this type
     */
    readonly asNonNullable: CustomValueType;

    /**
     * Whether this type can be null
     */
    readonly isNullable: boolean;

    /**
     * Get a property by name, including inherited properties.
     *
     * @param memberName The name of the property to look up
     * @returns The property, or undefined if not found
     */
    getProperty(memberName: string): Property | undefined;

    /**
     * Get a method by name, including inherited methods.
     *
     * @param memberName The name of the method to look up
     * @returns The method, or undefined if not found
     */
    getMethod(memberName: string): Method | undefined;

    /**
     * Get a property for a specific member name (without inheritance).
     * Must be implemented by subclasses to provide local property lookup.
     *
     * @param memberName The name of the property
     * @returns The property, or undefined if not found
     */
    getLocalProperty(memberName: string): Property | undefined;

    /**
     * Get a method for a specific member name (without inheritance).
     * Must be implemented by subclasses to provide local method lookup.
     *
     * @param memberName The name of the method
     * @returns The method, or undefined if not found
     */
    getLocalMethod(memberName: string): Method | undefined;

    /**
     * Register this type as a subtype of its super classes.
     * Called during initialization to establish the type hierarchy.
     */
    registerSubtypesAndConversion(): void;
}

/**
 * Abstract base class for custom value types (classes and lambdas).
 *
 * @template T The specific type details extending CustomValueTypeDetail
 */
export class CustomValueTypeImplementation<
    T extends CustomValueTypeDetail<TypirSpecifics> = CustomValueTypeDetail<TypirSpecifics>
>
    extends Type
    implements CustomValueType<T>
{
    readonly superClasses: CustomClassType[];

    private _allSuperClasses: CustomClassType[] | undefined = undefined;

    get allSuperClasses(): CustomClassType[] {
        if (this._allSuperClasses == undefined) {
            const allSupers = new Set<CustomClassType>();
            for (const superClass of this.superClasses) {
                allSupers.add(superClass);
                superClass.allSuperClasses.forEach((indirectSuperClass) => allSupers.add(indirectSuperClass));
            }
            this._allSuperClasses = [...allSupers];
        }
        return this._allSuperClasses;
    }

    private readonly cachedProperties: Map<string, Property> = new Map();
    private readonly cachedMethods: Map<string, Method> = new Map();

    get asNullable(): CustomValueType {
        throw new Error("Method not implemented.");
    }

    get asNonNullable(): CustomValueType {
        throw new Error("Method not implemented.");
    }

    get isNullable(): boolean {
        throw new Error("Method not implemented.");
    }

    get definition(): ValueType {
        throw new Error("Method not implemented.");
    }

    /**
     * Creates a new custom value type.
     *
     * @param identifier The unique identifier for this type
     * @param details The type details including type arguments and super types
     * @param services Extended Typir services for type operations
     * @param isNullable Whether this type is nullable
     * @param superTypes Resolved super class types
     */
    constructor(
        identifier: string,
        readonly details: T,
        readonly services: ExtendedTypirServices<TypirSpecifics>,
        isNullable: boolean,
        superTypes: BaseClassTypeRef[]
    ) {
        super(identifier, details);
        this.superClasses = (superTypes || []).map(
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

    getLocalProperty(_memberName: string): Property | undefined {
        throw new Error("Method not implemented.");
    }

    getLocalMethod(_memberName: string): Method | undefined {
        throw new Error("Method not implemented.");
    }

    registerSubtypesAndConversion() {
        for (const superClass of this.superClasses) {
            this.services.Subtype.markAsSubType(this, superClass);
        }
        if (!this.isNullable) {
            this.services.Subtype.markAsSubType(this, this.asNullable);
        }
        if (this.isNullable) {
            const nullType = this.services.factory.CustomNull.getOrCreate();
            this.services.Conversion.markAsConvertible(nullType, this, "IMPLICIT_EXPLICIT");
        }
    }

    getProperty(memberName: string): Property | undefined {
        if (this.cachedProperties.has(memberName)) {
            return this.cachedProperties.get(memberName);
        }
        const local = this.getLocalProperty(memberName);
        if (local !== undefined) {
            this.cachedProperties.set(memberName, local);
            return local;
        }
        for (const superClass of this.superClasses) {
            const found = superClass.getProperty(memberName);
            if (found !== undefined) {
                this.cachedProperties.set(memberName, found);
                return found;
            }
        }
        return undefined;
    }

    getMethod(memberName: string): Method | undefined {
        if (this.cachedMethods.has(memberName)) {
            return this.cachedMethods.get(memberName);
        }
        const local = this.getLocalMethod(memberName);
        if (local !== undefined) {
            this.cachedMethods.set(memberName, local);
            return local;
        }
        for (const superClass of this.superClasses) {
            const found = superClass.getMethod(memberName);
            if (found !== undefined) {
                this.cachedMethods.set(memberName, found);
                return found;
            }
        }
        return undefined;
    }

    override getName(): string {
        throw new Error("Method not implemented.");
    }

    override getUserRepresentation(): string {
        throw new Error("Method not implemented.");
    }

    override analyzeTypeEqualityProblems(_otherType: TypirType): TypirProblem[] {
        throw new Error("Method not implemented.");
    }
}

/**
 * Type guard to check if a value is a CustomValueType.
 *
 * @param type The value to check
 * @returns true if the value is a CustomValueType
 */
export function isCustomValueType(type: unknown): type is CustomValueType {
    return type instanceof CustomValueTypeImplementation;
}
