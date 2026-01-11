import { createInterface, createType, type ASTType } from "@mdeo/language-common";
import type { TypeConfig } from "./typeConfig.js";

/**
 * Generates type-related type interfaces based on the provided configuration.
 *
 * @param config config used for generating type names
 * @returns the generated type types
 */
export function generateTypeTypes(config: TypeConfig) {
    const baseTypeType = createInterface(config.baseTypeName).attrs({});

    const voidTypeType = createInterface(config.voidTypeTypeName).attrs({});

    const returnTypeType = createType(config.returnTypeTypeName).types(baseTypeType, voidTypeType);

    const classTypeType = createInterface(config.classTypeTypeName)
        .extends(baseTypeType)
        .attrs({
            name: String,
            typeArgs: [baseTypeType],
            isNullable: Boolean
        });

    const lambdaTypeParametersType = createInterface(config.lambdaTypeParametersTypeName).attrs({
        parameters: [baseTypeType]
    });

    const lambdaTypeType = createInterface(config.lambdaTypeTypeName).extends(baseTypeType).attrs({
        parameterList: lambdaTypeParametersType,
        returnType: returnTypeType,
        isNullable: Boolean
    });

    return {
        baseTypeType,
        voidTypeType,
        returnTypeType,
        classTypeType,
        lambdaTypeParametersType,
        lambdaTypeType
    };
}

/**
 * Type representing all generated type types.
 */
export type TypeTypes = ReturnType<typeof generateTypeTypes>;

/**
 * Type representing the base type type.
 */
export type BaseTypeType = ASTType<TypeTypes["baseTypeType"]>;

/**
 * Type representing the void type.
 */
export type VoidTypeType = ASTType<TypeTypes["voidTypeType"]>;

/**
 * Type representing the class type.
 */
export type ClassTypeType = ASTType<TypeTypes["classTypeType"]>;

/**
 * Type representing the lambda type parameters.
 */
export type LambdaTypeParametersType = ASTType<TypeTypes["lambdaTypeParametersType"]>;

/**
 * Type representing the lambda type.
 */
export type LambdaTypeType = ASTType<TypeTypes["lambdaTypeType"]>;
