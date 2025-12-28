import type { TypirSpecifics, TypirProblem, Type as TypirType, TypeEqualityProblem } from "typir";
import type { CustomFunctionDetails, CustomFunctionKind } from "./custom-function-kind.js";
import type { FunctionSignature, ReturnType } from "../../config/type.js";
import { ClassTypeRef, GenericTypeRef, LambdaType, VoidType } from "../../config/type.js";
import type { CustomValueType } from "../custom-value/custom-value-type.js";
import type { Provider } from "../../service/extendedTypirServices.js";
import { assertUnreachable } from "@mdeo/language-common";

/**
 * Constructor interface for custom function types.
 * Represents the class itself (static side), not instances.
 */
export interface CustomFunctionTypeConstructor {
    new (kind: CustomFunctionKind<TypirSpecifics>, details: CustomFunctionDetails<TypirSpecifics>): CustomFunctionType;
}

/**
 * Interface for custom function types.
 * Represents a named function with one or more signatures (overloads).
 */
export interface CustomFunctionType extends TypirType {
    /**
     * The kind that created this type
     */
    readonly kind: CustomFunctionKind<TypirSpecifics>;

    /**
     * The function type details
     */
    readonly details: CustomFunctionDetails<TypirSpecifics>;
}

export const CustomFunctionTypeProvider: Provider<CustomFunctionTypeConstructor> = (services) => {
    const { Type } = services.context.typir;
    const { checkValueForConflict, createKindConflict, TypeEqualityProblem } = services.context.typir;

    /**
     * Custom function type implementation.
     * Represents a named function with one or more signatures (overloads).
     */
    class CustomFunctionTypeImplementation extends Type implements CustomFunctionType {
        declare readonly kind: CustomFunctionKind<TypirSpecifics>;

        /**
         * Creates a new custom function type.
         * Automatically initializes the type.
         *
         * @param kind The kind that created this type
         * @param details The function type details
         */
        constructor(
            kind: CustomFunctionKind<TypirSpecifics>,
            readonly details: CustomFunctionDetails<TypirSpecifics>
        ) {
            super(buildCustomFunctionIdentifier(details), details);
            this.kind = kind;
            this.defineTheInitializationProcessOfThisType({});
        }

        override getName(): string {
            return buildCustomFunctionName(this.details);
        }

        override getUserRepresentation(): string {
            return buildCustomFunctionName(this.details);
        }

        override analyzeTypeEqualityProblems(otherType: TypirType): TypirProblem[] {
            if (this.kind.services.factory.CustomFunctions.isCustomFunctionType(otherType)) {
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
    }

    return CustomFunctionTypeImplementation;
};

/**
 * Counter for generating unique function type identifiers.
 */
let functionNameCounter = 0;

/**
 * Build a unique identifier for a custom function type.
 *
 * @param details The function type details
 * @returns The unique identifier string
 */
export function buildCustomFunctionIdentifier(details: CustomFunctionDetails<any>): string {
    return `CustomFunction#${functionNameCounter++}`;
}

/**
 * Build a name for a custom function type.
 * Includes all signature overloads separated by newlines.
 *
 * @param details The function type details
 * @returns The name string
 */
export function buildCustomFunctionName(details: CustomFunctionDetails<any>): string {
    return details.definition.signatures
        .map((signature) => buildFunctionSignatureName(details, signature))
        .join("\n");
}

/**
 * Build a name for a single function signature.
 *
 * @param details The function type details
 * @param signature The specific signature to build a name for
 * @returns The signature name string in the format "name(param1,param2): returnType"
 */
function buildFunctionSignatureName(
    details: CustomFunctionDetails<any>,
    signature: FunctionSignature,
): string {
    const paramTypes = signature.parameters.map((param) => {
        const resolvedType = resolveTypeFromDefinition(param.type, details.typeArgs);
        return resolvedType;
    });

    if (signature.isVarArgs && paramTypes.length > 0) {
        paramTypes[paramTypes.length - 1] = `...${paramTypes[paramTypes.length - 1]}`;
    }

    const returnTypeStr = resolveTypeFromDefinition(signature.returnType, details.typeArgs);

    return `${details.name}(${paramTypes.join(",")}): ${returnTypeStr}`;
}

/**
 * Resolve a value type definition to a string representation.
 * Handles generic types, class types, lambda types, and void types.
 *
 * @param type The value type definition to resolve
 * @param typeArgs Map of generic type arguments
 * @returns The string representation of the type
 */
function resolveTypeFromDefinition(
    type: ReturnType,
    typeArgs: Map<string, CustomValueType>,
): string {
    if (VoidType.is(type)) {
        return "void";
    } else if (GenericTypeRef.is(type)) {
        const resolvedGeneric = typeArgs.get(type.generic);
        if (resolvedGeneric) {
            return  resolvedGeneric.getName();
        }
        return type.generic;
    } else if (ClassTypeRef.is(type)) {
        const baseType = type.type;

        if (type.typeArgs && type.typeArgs.size > 0) {
            const typeArgsStr = Array.from(type.typeArgs.values())
                .map((arg: any) => resolveTypeFromDefinition(arg, typeArgs))
                .join(",");
            return `${baseType}<${typeArgsStr}>${type.isNullable ? "?" : ""}`;
        }

        return `${baseType}${type.isNullable ? "?" : ""}`;
    } else if (LambdaType.is(type)) {
        const params = type.parameters.map((param: any) =>
            resolveTypeFromDefinition(param.type, typeArgs)
        );
        const returnType = resolveTypeFromDefinition(type.returnType, typeArgs);
        const lambdaStr = `(${params.join(",")}) => ${returnType}`;
        return type.isNullable ? `(${lambdaStr})?` : lambdaStr;
    } else {
        assertUnreachable(type);
    }
}
