import type { TypirSpecifics, TypirProblem, Type as TypirType, TypeEqualityProblem } from "typir";
import type { CustomClassDetails, CustomClassKind } from "./custom-class-kind.js";
import type { CustomValueType } from "../custom-value/custom-value-type.js";
import type { Member } from "../../config/type.js";
import type { Provider } from "../../service/extendedTypirServices.js";

/**
 * Constructor interface for custom class types.
 * Represents the class itself (static side), not instances.
 */
export interface CustomClassTypeConstructor {
    new (kind: CustomClassKind<TypirSpecifics>, details: CustomClassDetails<TypirSpecifics>): CustomClassType;
}

/**
 * Interface for custom class types.
 * Represents a concrete class type with properties, methods, and type arguments.
 */
export interface CustomClassType extends CustomValueType<CustomClassDetails<TypirSpecifics>> {
    /**
     * The kind that created this type
     */
    readonly kind: CustomClassKind<TypirSpecifics>;
}

export const CustomClassTypeProvider: Provider<CustomClassTypeConstructor> = (services) => {
    const CustomValueTypeClass = services.factory.CustomValues.CustomValueType;
    const { checkValueForConflict, createKindConflict, TypeEqualityProblem } = services.context.typir;

    /**
     * Custom class type implementation.
     * Represents a concrete class type with properties, methods, and type arguments.
     */
    class CustomClassTypeImplementation
        extends CustomValueTypeClass<CustomClassDetails<TypirSpecifics>>
        implements CustomClassType
    {
        declare readonly kind: CustomClassKind<TypirSpecifics>;

        /**
         * Cache for the nullable/non-nullable variant of this type
         */
        private nullInvertedTypeCache: CustomClassType | undefined = undefined;

        /**
         * Creates a new custom class type.
         * Automatically registers subtype relationships and initializes the type.
         *
         * @param kind The kind that created this type
         * @param details The class type details
         */
        constructor(kind: CustomClassKind<TypirSpecifics>, details: CustomClassDetails<TypirSpecifics>) {
            super(buildCustomClassIdentifier(details), details, kind.services, details.isNullable);
            this.kind = kind;
            this.registerSubtypes();
            this.defineTheInitializationProcessOfThisType({});
        }

        override get isNullable(): boolean {
            return this.details.isNullable;
        }

        override get asNullable(): CustomClassType {
            if (this.isNullable) {
                return this;
            }
            if (this.nullInvertedTypeCache != undefined) {
                return this.nullInvertedTypeCache;
            }
            const nullableType = this.kind.services.factory.CustomClasses.getOrCreate({
                ...this.details,
                isNullable: true
            });
            this.nullInvertedTypeCache = nullableType;
            return nullableType;
        }

        override get asNonNullable(): CustomClassType {
            if (!this.isNullable) {
                return this;
            }
            if (this.nullInvertedTypeCache != undefined) {
                return this.nullInvertedTypeCache;
            }
            const nonNullableType = this.kind.services.factory.CustomClasses.getOrCreate({
                ...this.details,
                isNullable: false
            });
            this.nullInvertedTypeCache = nonNullableType;
            return nonNullableType;
        }

        override getName(): string {
            return buildCustomClassName(this.details, false);
        }

        override getUserRepresentation(): string {
            return buildCustomClassIdentifier(this.details);
        }

        override analyzeTypeEqualityProblems(otherType: TypirType): TypirProblem[] {
            if (this.kind.services.factory.CustomClasses.isCustomClassType(otherType)) {
                return checkValueForConflict(this.getIdentifier(), otherType.getIdentifier(), "name");
            } else {
                return [
                    <TypeEqualityProblem>{
                        $problem: TypeEqualityProblem,
                        type1: this,
                        type2: otherType,
                        subProblems: [createKindConflict(otherType, this)]
                    }
                ];
            }
        }

        override getLocalMember(memberName: string): Member | undefined {
            return this.details.definition.members.get(memberName);
        }
    }

    return CustomClassTypeImplementation;
};

/**
 * Build a unique identifier for a custom class type.
 *
 * @param details The class type details
 * @returns The unique identifier string
 */
export function buildCustomClassIdentifier(details: CustomClassDetails<any>): string {
    return buildCustomClassName(details, true);
}

/**
 * Build a name for a custom class type.
 *
 * @param details The class type details
 * @param isIdentifier Whether to build a full identifier (with package) or just a name
 * @returns The name string
 */
export function buildCustomClassName(details: CustomClassDetails<any>, isIdentifier: boolean): string {
    const name = isIdentifier ? details.definition.package + "." + details.definition.name : details.definition.name;
    const typeArgsStr = Array.from(details.typeArgs.values())
        .map((typeArg) => (isIdentifier ? typeArg.getIdentifier() : typeArg.getName()))
        .join(",");
    return typeArgsStr.length > 0
        ? `${name}<${typeArgsStr}>${details.isNullable ? "?" : ""}`
        : `${name}${details.isNullable ? "?" : ""}`;
}
