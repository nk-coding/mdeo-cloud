import { createInterface, Union, type ASTType } from "@mdeo/language-common";
import type { ExpressionConfig } from "./expressionConfig.js";
import type { TypeTypes } from "./typeTypes.js";

/**
 * Generates expression-related type interfaces based on the provided configuration.
 *
 * @param config config used for generating type names
 * @param typeTypes the generated type types to reference in expressions
 * @returns the generated expression types
 */
export function generateExpressionTypes(config: ExpressionConfig, typeTypes: TypeTypes) {
    const baseTypeType = typeTypes.baseTypeType;
    const classTypeType = typeTypes.classTypeType;
    const lambdaTypeType = typeTypes.lambdaTypeType;

    const baseExpressionType = createInterface(config.baseExpressionTypeName).attrs({});

    const unaryExpressionType = createInterface(config.unaryExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            expression: baseExpressionType,
            operator: Union("-", "!")
        });

    const binaryExpressionType = createInterface(config.binaryExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            operator: Union("+", "-", "*", "/", "%", "&&", "||", "==", "!=", "<", ">", "<=", ">="),
            left: baseExpressionType,
            right: baseExpressionType
        });

    const ternaryExpressionType = createInterface(config.ternaryExpressionTypeName).extends(baseExpressionType).attrs({
        condition: baseExpressionType,
        trueExpression: baseExpressionType,
        falseExpression: baseExpressionType
    });

    const callExpressionGenericArgsType = createInterface(config.callExpressionGenericArgsTypeName).attrs({
        typeArguments: [baseTypeType]
    });

    const callExpressionType = createInterface(config.callExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            expression: baseExpressionType,
            arguments: [baseExpressionType],
            genericArgs: callExpressionGenericArgsType
        });

    const memberAccessExpressionType = createInterface(config.memberAccessExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            expression: baseExpressionType,
            member: String,
            isNullChaining: Boolean
        });

    const identifierExpressionType = createInterface(config.identifierExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            name: String
        });

    const stringLiteralExpressionType = createInterface(config.stringLiteralExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            value: String
        });

    const intLiteralExpressionType = createInterface(config.intLiteralExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            value: String
        });

    const longLiteralExpressionType = createInterface(config.longLiteralExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            value: String
        });

    const floatLiteralExpressionType = createInterface(config.floatLiteralExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            value: String
        });

    const doubleLiteralExpressionType = createInterface(config.doubleLiteralExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            value: String
        });

    const booleanLiteralExpressionType = createInterface(config.booleanLiteralExpressionTypeName)
        .extends(baseExpressionType)
        .attrs({
            value: Boolean
        });

    return {
        baseExpressionType,
        unaryExpressionType,
        binaryExpressionType,
        ternaryExpressionType,
        callExpressionGenericArgsType,
        callExpressionType,
        memberAccessExpressionType,
        identifierExpressionType,
        stringLiteralExpressionType,
        intLiteralExpressionType,
        longLiteralExpressionType,
        floatLiteralExpressionType,
        doubleLiteralExpressionType,
        booleanLiteralExpressionType,
        baseTypeType,
        classTypeType,
        lambdaTypeType
    };
}

/**
 * Type representing all generated expression types.
 */
export type ExpressionTypes = ReturnType<typeof generateExpressionTypes>;

/**
 * Type representing the base expression type.
 */
export type BaseExpressionType = ASTType<ExpressionTypes["baseExpressionType"]>;

/**
 * Type representing the member access expression
 */
export type MemberAccessExpressionType = ASTType<ExpressionTypes["memberAccessExpressionType"]>;

/**
 * Type representing the call expression
 */
export type CallExpressionType = ASTType<ExpressionTypes["callExpressionType"]>;
