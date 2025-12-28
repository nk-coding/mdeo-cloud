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

    const lambdaTypeType = createInterface(config.lambdaTypeTypeName)
        .extends(baseTypeType)
        .attrs({
            parameters: [baseTypeType],
            returnType: returnTypeType,
            isNullable: Boolean
        });

    return {
        baseTypeType,
        voidTypeType,
        returnTypeType,
        classTypeType,
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
