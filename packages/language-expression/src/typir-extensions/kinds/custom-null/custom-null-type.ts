import type { TypirProblem, TypirSpecifics, Type as TypirType, TypeEqualityProblem } from "typir";
import type { CustomValueType, CustomValueTypeDetail } from "../custom-value/custom-value-type.js";
import type { CustomNullKind } from "./custom-null-kind.js";
import type { Provider } from "../../service/extendedTypirServices.js";
import type { ClassTypeRef, Member } from "../../config/type.js";

/**
 * Type details for the null type.
 * Null is a singleton type with no additional details.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface CustomNullDetails<Specifics extends TypirSpecifics> extends CustomValueTypeDetail<Specifics> {
    typeArgs: Map<string, CustomValueType>;
    superTypes: [];
}

/**
 * Constructor interface for custom null type.
 */
export interface CustomNullTypeConstructor {
    new (kind: CustomNullKind<TypirSpecifics>, details: CustomNullDetails<TypirSpecifics>): CustomNullType;
}

/**
 * Interface for the null type.
 * Represents the null value type.
 */
export interface CustomNullType extends CustomValueType<CustomNullDetails<TypirSpecifics>> {
    /**
     * The kind that created this type
     */
    readonly kind: CustomNullKind<TypirSpecifics>;
}

export const CustomNullTypeProvider: Provider<CustomNullTypeConstructor> = (services) => {
    const CustomValueTypeClass = services.factory.CustomValues.CustomValueType;
    const { TypeEqualityProblem: TEP } = services.context.typir;
    const { createKindConflict } = services.context.typir;

    /**
     * Custom null type implementation.
     * Represents the null type as a singleton.
     */
    class CustomNullTypeImplementation
        extends CustomValueTypeClass<CustomNullDetails<TypirSpecifics>>
        implements CustomNullType
    {
        declare readonly kind: CustomNullKind<TypirSpecifics>;

        /**
         * Creates the null type.
         * This should only be called once to create the singleton instance.
         *
         * @param kind The kind that created this type
         * @param details The null type details
         */
        constructor(kind: CustomNullKind<TypirSpecifics>, details: CustomNullDetails<TypirSpecifics>) {
            super("null", details, kind.services, true, []);
            this.kind = kind;
            this.defineTheInitializationProcessOfThisType({});
        }

        override get isNullable(): boolean {
            return true;
        }

        override get asNullable(): CustomNullType {
            return this;
        }

        override get asNonNullable(): CustomNullType {
            return this;
        }

        override get definition(): ClassTypeRef {
            throw new Error("Definition not supported for null type.");
        }

        override getName(): string {
            return "null";
        }

        override getUserRepresentation(): string {
            return "null";
        }

        override analyzeTypeEqualityProblems(otherType: TypirType): TypirProblem[] {
            if (this.kind.services.factory.CustomNull.isCustomNullType(otherType)) {
                return [];
            } else {
                return [
                    <TypeEqualityProblem>{
                        $problem: TEP,
                        type1: this,
                        type2: otherType,
                        subProblems: [createKindConflict(otherType, this)]
                    }
                ];
            }
        }

        override getLocalMember(): Member | undefined {
            return undefined;
        }
    }

    return CustomNullTypeImplementation;
};
