import type { Type as TypirType, TypeDetails, TypirProblem, TypirSpecifics } from "typir";
import type { CustomClassType } from "../custom-class/custom-class-type.js";
import type { ExtendedTypirServices, Provider } from "../../service/extendedTypirServices.js";
import type { BaseClassTypeRef, Member } from "../../config/type.js";

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
     * All super classes including inherited ones
     */
    readonly allSuperClasses: CustomClassType[];

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
     * Get a member (property or method) by name, including inherited members.
     * Uses caching to avoid repeated lookups.
     *
     * @param memberName The name of the member to look up
     * @returns The member wrapper object, or undefined if not found
     */
    getMember(memberName: string): Member | undefined;

    /**
     * Get a member for a specific member name (without inheritance).
     * Must be implemented by subclasses to provide local member lookup.
     *
     * @param memberName The name of the member
     * @returns The member wrapper object, or undefined if not found
     */
    getLocalMember(memberName: string): Member | undefined;

    /**
     * Register this type as a subtype of its super classes.
     * Called during initialization to establish the type hierarchy.
     */
    registerSubtypesAndConversion(): void;
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

        private readonly cachedMembers: Map<string, Member> = new Map();

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

        getLocalMember(_memberName: string): Member | undefined {
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

        getMember(memberName: string): Member | undefined {
            if (this.cachedMembers.has(memberName)) {
                return this.cachedMembers.get(memberName);
            }
            const member = this.getLocalMember(memberName);
            if (member == undefined) {
                for (const superClass of this.superClasses) {
                    const superMember = superClass.getMember(memberName);
                    if (superMember != undefined) {
                        this.cachedMembers.set(memberName, superMember);
                        return superMember;
                    }
                }
                return undefined;
            }
            this.cachedMembers.set(memberName, member);
            return member;
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
    return CustomValueTypeImplementation;
};
