import type { TypirSpecifics, TypirProblem, Type as TypirType, TypeEqualityProblem } from "typir";
import type { CustomClassDetails, CustomClassKind } from "./custom-class-kind.js";
import { CustomValueTypeImplementation, type CustomValueType } from "../custom-value/custom-value-type.js";
import type { ClassTypeRef, Member, ValueType } from "../../config/type.js";
import { sharedImport } from "@mdeo/language-shared";

const {
    checkValueForConflict,
    TypeEqualityProblem: TypeEqualityProblemConstant,
    createKindConflict
} = sharedImport("typir");

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

/**
 * Custom class type implementation.
 * Represents a concrete class type with properties, methods, and type arguments.
 */
export class CustomClassTypeImplementation
    extends CustomValueTypeImplementation<CustomClassDetails<TypirSpecifics>>
    implements CustomClassType
{
    declare readonly kind: CustomClassKind<TypirSpecifics>;

    /**
     * Cache for the nullable/non-nullable variant of this type
     */
    private nullInvertedTypeCache: CustomClassType | undefined = undefined;

    /**
     * Cached definition of this class type
     */
    private _definition: ClassTypeRef | undefined = undefined;

    /**
     * Creates a new custom class type.
     * Automatically registers subtype relationships and initializes the type.
     *
     * @param kind The kind that created this type
     * @param details The class type details
     */
    constructor(kind: CustomClassKind<TypirSpecifics>, details: CustomClassDetails<TypirSpecifics>) {
        super(
            buildCustomClassIdentifier(details),
            details,
            kind.services,
            details.isNullable,
            details.definition.superTypes ?? []
        );
        this.kind = kind;
        this.registerSubtypesAndConversion();
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

    override get definition(): ClassTypeRef {
        if (this._definition == undefined) {
            const definition = this.details.definition;
            const typeArgs: Record<string, ValueType> = {};
            for (const [argName, argType] of this.details.typeArgs) {
                typeArgs[argName] = argType.definition;
            }
            this._definition = {
                type: `${definition.package}.${definition.name}`,
                isNullable: this.isNullable,
                typeArgs: typeArgs
            };
        }
        return this._definition;
    }

    override getName(): string {
        return buildCustomClassName(this.details, false);
    }

    override getUserRepresentation(): string {
        return buildCustomClassIdentifier(this.details);
    }

    override analyzeTypeEqualityProblems(otherType: TypirType): TypirProblem[] {
        if (isCustomClassType(otherType)) {
            return checkValueForConflict(this.getIdentifier(), otherType.getIdentifier(), "name");
        } else {
            return [
                <TypeEqualityProblem>{
                    $problem: TypeEqualityProblemConstant,
                    type1: this,
                    type2: otherType,
                    subProblems: [createKindConflict(otherType, this)]
                }
            ];
        }
    }

    override getLocalMember(memberName: string): Member | undefined {
        return this.details.definition.members[memberName];
    }
}

/**
 * Type guard to check if a value is a CustomClassType.
 *
 * @param type The value to check
 * @returns true if the value is a CustomClassType
 */
export function isCustomClassType(type: unknown): type is CustomClassType {
    return type instanceof CustomClassTypeImplementation;
}

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
