import type { Type as TypirType, TypeDetails, TypirProblem, TypirSpecifics, TypeEqualityProblem } from "typir";
import type { CustomVoidKind } from "./custom-void-kind.js";
import type { Provider } from "../../service/extendedTypirServices.js";
import type { VoidType } from "../../config/type.js";

/**
 * Type details for the void type.
 * Void is a singleton type with no additional details.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export type CustomVoidDetails<Specifics extends TypirSpecifics> = TypeDetails<Specifics>;

/**
 * Constructor interface for custom void type.
 */
export interface CustomVoidTypeConstructor {
    new (kind: CustomVoidKind<TypirSpecifics>, details: CustomVoidDetails<TypirSpecifics>): CustomVoidType;
}

/**
 * Interface for the void type.
 * Represents the absence of a return value.
 */
export interface CustomVoidType extends TypirType {
    /**
     * The kind that created this type
     */
    readonly kind: CustomVoidKind<TypirSpecifics>;

    /**
     * The void type details
     */
    readonly details: CustomVoidDetails<TypirSpecifics>;

    /**
     * The type definition associated with this void type
     */
    readonly definition: VoidType;
}

export const CustomVoidTypeProvider: Provider<CustomVoidTypeConstructor> = (services) => {
    const { Type, TypeEqualityProblem: TEP } = services.context.typir;
    const { createKindConflict } = services.context.typir;

    /**
     * Custom void type implementation.
     * Represents the void type as a singleton.
     */
    class CustomVoidTypeImplementation extends Type implements CustomVoidType {
        declare readonly kind: CustomVoidKind<TypirSpecifics>;

        /**
         * Creates the void type.
         * This should only be called once to create the singleton instance.
         *
         * @param kind The kind that created this type
         * @param details The void type details
         */
        constructor(
            kind: CustomVoidKind<TypirSpecifics>,
            readonly details: CustomVoidDetails<TypirSpecifics>
        ) {
            super("void", details);
            this.kind = kind;
            this.defineTheInitializationProcessOfThisType({});
        }

        get definition(): VoidType {
            return {
                kind: "void"
            };
        }

        override getName(): string {
            return "void";
        }

        override getUserRepresentation(): string {
            return "void";
        }

        override analyzeTypeEqualityProblems(otherType: TypirType): TypirProblem[] {
            if (this.kind.services.factory.CustomVoid.isCustomVoidType(otherType)) {
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
    }

    return CustomVoidTypeImplementation;
};
