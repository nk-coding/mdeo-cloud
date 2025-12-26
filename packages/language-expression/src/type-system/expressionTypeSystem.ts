import type { LangiumTypeSystemDefinition, TypirLangiumServices, TypirLangiumSpecifics } from "typir-langium";
import type { DefaultTypeConfig } from "./defaultTypeConfig.js";
import type { CustomClassType } from "../typir-extensions/kinds/custom-class/custom-class-type.js";
import type { ClassType } from "../typir-extensions/config/type.js";
import type { TypeDefinitionService } from "../typir-extensions/service/type-definition-service.js";
import type {
    TypeInferenceRuleWithoutInferringChildren,
    ValidationRuleFunctional
} from "typir";
import { inferMemberAccess } from "../typir-extensions/rules/inferMemberAccess.js";
import { inferCall } from "../typir-extensions/rules/inferCall.js";
import type { ExpressionTypes } from "../grammar/expressionTypes.js";
import { validateMemberAccess } from "../typir-extensions/rules/validateMemberAccess.js";
import type { ExpressionTypirServices } from "./services.js";
import type { Interface } from "@mdeo/language-common";
import type { AstNode } from "langium";
import { validateCall } from "../typir-extensions/rules/validateCall.js";

/**
 * Represents a mapping of primitive type names to their corresponding CustomClassType instances.
 * Excludes special type configuration keys like "additionalTypes", "lambdaSuperTypes", and "void".
 */
type PrimitiveTypes = Record<
    Exclude<keyof DefaultTypeConfig, "additionalTypes" | "lambdaSuperTypes" | "void">,
    CustomClassType
>;

/**
 * Defines the modes for type conversions within the type system.
 */
enum ConversionMode {
    /** Only explicit conversions are allowed */
    EXPLICIT = "EXPLICIT",
    /** Both implicit and explicit conversions are allowed */
    IMPLICIT_EXPLICIT = "IMPLICIT_EXPLICIT"
}

/**
 * The main type system implementation for expressions in the language.
 * This class manages type definitions, type inference rules, validation rules,
 * and type conversions for various expression types.
 * 
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 */
export class ExpressionTypeSystem<Specifics extends TypirLangiumSpecifics>
    implements LangiumTypeSystemDefinition<Specifics>
{
    /** Non-nullable primitive types (int, long, float, double, string, boolean, Any) */
    protected primitiveTypes!: PrimitiveTypes;

    /** Nullable versions of primitive types */
    protected nullablePrimitiveTypes!: PrimitiveTypes;

    /** The void type */
    protected voidType!: CustomClassType;

    /**
     * Creates a new ExpressionTypeSystem instance.
     * 
     * @param defaultTypeConfig Configuration for default/primitive types
     * @param expressionTypes Type definitions for various expression AST nodes
     */
    constructor(
        readonly defaultTypeConfig: DefaultTypeConfig,
        readonly expressionTypes: ExpressionTypes
    ) {}

    onInitialize(typir: TypirLangiumServices<Specifics>): void {
        const extendedTypir = typir as ExpressionTypirServices<Specifics>;
        const { TypeDefinitions, Conversion, Inference } = extendedTypir;
        this.registerTypes(TypeDefinitions);
        this.primitiveTypes = this.buildPrimitiveTypes(TypeDefinitions, false);
        this.nullablePrimitiveTypes = this.buildPrimitiveTypes(TypeDefinitions, true);
        this.voidType = this.buildPrimitiveType(TypeDefinitions, this.defaultTypeConfig.void, false);

        for (const isNullable of [false, true]) {
            const primitives = isNullable ? this.nullablePrimitiveTypes : this.primitiveTypes;
            Conversion.markAsConvertible(primitives.int, primitives.long, ConversionMode.IMPLICIT_EXPLICIT);
            Conversion.markAsConvertible(primitives.int, primitives.float, ConversionMode.IMPLICIT_EXPLICIT);
            Conversion.markAsConvertible(primitives.int, primitives.double, ConversionMode.IMPLICIT_EXPLICIT);
            Conversion.markAsConvertible(primitives.long, primitives.float, ConversionMode.IMPLICIT_EXPLICIT);
            Conversion.markAsConvertible(primitives.long, primitives.double, ConversionMode.IMPLICIT_EXPLICIT);
            Conversion.markAsConvertible(primitives.float, primitives.double, ConversionMode.IMPLICIT_EXPLICIT);
        }

        this.registerExpressionRules(extendedTypir);
    }

    onNewAstNode(languageNode: Specifics["LanguageType"], typir: TypirLangiumServices<Specifics>): void {
        throw new Error("Method not implemented.");
    }

    /**
     * Registers all type definitions including primitive types and lambda super types.
     * 
     * @param typeDefinitions The type definition service to register types with
     */
    private registerTypes(typeDefinitions: TypeDefinitionService): void {
        typeDefinitions.registerClassTypes([
            this.defaultTypeConfig.Any,
            this.defaultTypeConfig.int,
            this.defaultTypeConfig.long,
            this.defaultTypeConfig.float,
            this.defaultTypeConfig.double,
            this.defaultTypeConfig.string,
            this.defaultTypeConfig.boolean,
            this.defaultTypeConfig.void,
            ...this.defaultTypeConfig.additionalTypes
        ]);
        typeDefinitions.registerLambdaSuperTypes(this.defaultTypeConfig.lambdaSuperTypes);
    }

    /**
     * Builds all primitive types (nullable or non-nullable).
     * 
     * @param typeDefinitions The type definition service
     * @param isNullable Whether to build nullable or non-nullable versions
     * @returns An object mapping primitive type names to their CustomClassType instances
     */
    private buildPrimitiveTypes(typeDefinitions: TypeDefinitionService, isNullable: boolean): PrimitiveTypes {
        return {
            Any: this.buildPrimitiveType(typeDefinitions, this.defaultTypeConfig.Any, isNullable),
            int: this.buildPrimitiveType(typeDefinitions, this.defaultTypeConfig.int, isNullable),
            long: this.buildPrimitiveType(typeDefinitions, this.defaultTypeConfig.long, isNullable),
            float: this.buildPrimitiveType(typeDefinitions, this.defaultTypeConfig.float, isNullable),
            double: this.buildPrimitiveType(typeDefinitions, this.defaultTypeConfig.double, isNullable),
            string: this.buildPrimitiveType(typeDefinitions, this.defaultTypeConfig.string, isNullable),
            boolean: this.buildPrimitiveType(typeDefinitions, this.defaultTypeConfig.boolean, isNullable)
        };
    }

    /**
     * Builds a single primitive type instance.
     * 
     * @param typeDefinitions The type definition service
     * @param type The class type configuration
     * @param isNullable Whether the type should be nullable
     * @returns The resolved CustomClassType instance
     */
    private buildPrimitiveType(
        typeDefinitions: TypeDefinitionService,
        type: ClassType,
        isNullable: boolean
    ): CustomClassType {
        return typeDefinitions.resolveCustomClassOrLambdaType({
            type: type.name,
            isNullable: isNullable
        }) as CustomClassType;
    }

    /**
     * Registers type inference and validation rules for all expression types.
     * This includes literal expressions, member access, and call expressions.
     * 
     * @param typir The extended Typir services with expression-specific functionality
     */
    private registerExpressionRules(typir: ExpressionTypirServices<Specifics>): void {
        const AstReflection = typir.langium.LangiumServices.AstReflection;

        this.registerInferenceRule(this.expressionTypes.intLiteralExpressionType, () => this.primitiveTypes.int, typir);
        this.registerInferenceRule(
            this.expressionTypes.longLiteralExpressionType,
            () => this.primitiveTypes.long,
            typir
        );
        this.registerInferenceRule(
            this.expressionTypes.floatLiteralExpressionType,
            () => this.primitiveTypes.float,
            typir
        );
        this.registerInferenceRule(
            this.expressionTypes.doubleLiteralExpressionType,
            () => this.primitiveTypes.double,
            typir
        );
        this.registerInferenceRule(
            this.expressionTypes.stringLiteralExpressionType,
            () => this.primitiveTypes.string,
            typir
        );
        this.registerInferenceRule(
            this.expressionTypes.booleanLiteralExpressionType,
            () => this.primitiveTypes.boolean,
            typir
        );

        this.registerInferenceRule(
            this.expressionTypes.memberAccessExpressionType,
            (node) => {
                const inferResult = inferMemberAccess(node, node.expression, node.member, typir);
                if (node.isNullChaining && typir.factory.CustomValues.isCustomValueType(inferResult)) {
                    return inferResult.asNullable;
                }
                return inferResult;
            },
            typir
        );
        this.registerInferenceRule(
            this.expressionTypes.callExpressionType,
            (node) => {
                const inferResult = inferCall(
                    node,
                    node.expression,
                    node.genericArgs.typeArguments,
                    node.arguments,
                    typir
                );
                if (typir.factory.CustomValues.isCustomValueType(inferResult)) {
                    const expression = node.expression;
                    if (AstReflection.isInstance(expression, this.expressionTypes.memberAccessExpressionType)) {
                        if (expression.isNullChaining) {
                            return inferResult.asNullable;
                        }
                    }
                }
                return inferResult;
            },
            typir
        );

        this.registerValidationRule(
            this.expressionTypes.memberAccessExpressionType,
            (node) => validateMemberAccess(node, node.expression, node.member, node.isNullChaining, typir),
            typir
        );

        this.registerValidationRule(
            this.expressionTypes.callExpressionType,
            (node) => validateCall(node, node.expression, node.genericArgs.typeArguments, node.arguments, typir),
            typir
        );
    }

    /**
     * Registers a type inference rule for a specific AST node type.
     * 
     * @template T The AST node type extending AstNode
     * @param type The interface/type definition for the AST node
     * @param rule The inference rule function to apply
     * @param typir The Typir services instance
     */
    private registerInferenceRule<T extends AstNode>(
        type: Interface<T>,
        rule: TypeInferenceRuleWithoutInferringChildren<Specifics, T>,
        typir: ExpressionTypirServices<Specifics>
    ): void {
        const inference = typir.Inference;
        const AstReflection = typir.langium.LangiumServices.AstReflection;
        inference.addInferenceRule((node) => {
            if (!AstReflection.isInstance(node, type)) {
                return typir.context.typir.InferenceRuleNotApplicable;
            }
            return rule(node, typir);
        });
    }

    /**
     * Registers a validation rule for a specific AST node type.
     * 
     * @template T The AST node type extending AstNode
     * @param type The interface/type definition for the AST node
     * @param rule The validation rule function to apply
     * @param typir The Typir services instance
     */
    private registerValidationRule<T extends AstNode>(
        type: Interface<T>,
        rule: ValidationRuleFunctional<Specifics, T>,
        typir: ExpressionTypirServices<Specifics>
    ): void {
        const validation = typir.validation.Collector;
        const AstReflection = typir.langium.LangiumServices.AstReflection;
        validation.addValidationRule(
            (node, services) => {
                if (!AstReflection.isInstance(node, type)) {
                    return [];
                }
                return rule(node, services, typir);
            },
            {
                languageKey: type.name
            }
        );
    }
}
