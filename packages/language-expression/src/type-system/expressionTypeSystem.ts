import type { LangiumTypeSystemDefinition, TypirLangiumServices, TypirLangiumSpecifics } from "typir-langium";
import type { DefaultTypeConfig } from "./defaultTypeConfig.js";
import type { CustomClassType } from "../typir-extensions/kinds/custom-class/custom-class-type.js";
import type { ClassType } from "../typir-extensions/config/type.js";
import type { TypeDefinitionService } from "../typir-extensions/service/type-definition-service.js";
import type { ExpressionTypes } from "../grammar/expressionTypes.js";
import type { ExpressionTypirServices } from "./services.js";
import { ExpressionPartialTypeSystem } from "./expressionPartialTypeSystem.js";
import type { PrimitiveTypes } from "./partialTypeSystem.js";

/**
 * Defines the modes for type conversions within the type system.
 */
enum ConversionMode {
    /**
     * Only explicit conversions are allowed.
     */
    EXPLICIT = "EXPLICIT",
    /**
     * Both implicit and explicit conversions are allowed.
     */
    IMPLICIT_EXPLICIT = "IMPLICIT_EXPLICIT"
}

/**
 * The main type system implementation for expressions in the language.
 * This class manages type definitions, type inference rules, validation rules,
 * and type conversions for various expression types.
 *
 * Uses composition to delegate expression-specific type system logic to
 * ExpressionPartialTypeSystem.
 *
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 */
export class ExpressionTypeSystem<Specifics extends TypirLangiumSpecifics>
    implements LangiumTypeSystemDefinition<Specifics>
{
    /**
     * Non-nullable primitive types (int, long, float, double, string, boolean, Any).
     */
    protected primitiveTypes!: PrimitiveTypes;

    /**
     * Nullable versions of primitive types.
     */
    protected nullablePrimitiveTypes!: PrimitiveTypes;

    /**
     * The void type.
     */
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
        const { TypeDefinitions, Conversion } = extendedTypir;
        this.registerTypes(TypeDefinitions);
        this.primitiveTypes = this.buildPrimitiveTypes(TypeDefinitions, false);
        this.nullablePrimitiveTypes = this.buildPrimitiveTypes(TypeDefinitions, true);

        this.registerTypeConversions(Conversion);

        const expressionPartialTypeSystem = new ExpressionPartialTypeSystem(
            extendedTypir,
            this.expressionTypes,
            this.primitiveTypes,
            this.nullablePrimitiveTypes
        );
        expressionPartialTypeSystem.registerRules();
        this.onInitializeExtended(extendedTypir);
    }

    /**
     * Variant of onInitialize called with the extended Typir services.
     * 
     * @param _typir The extended Typir services instance
     * @see LangiumTypeSystemDefinition.onInitialize
     */
    protected onInitializeExtended(_typir: ExpressionTypirServices<Specifics>): void {}

    /**
     * Called when a new AST node is encountered.
     * Currently not implemented.
     *
     * @param languageNode The AST node
     * @param typir The Typir services instance
     */
    onNewAstNode(languageNode: Specifics["LanguageType"], typir: TypirLangiumServices<Specifics>): void {
        const extendedTypir = typir as ExpressionTypirServices<Specifics>;
        this.onNewAstNodeExtended(languageNode, extendedTypir);
    }

    /**
     * Variant of onNewAstNode called with the extended Typir services.
     *
     * @param _languageNode The AST node
     * @param _typir The extended Typir services instance
     * @see LangiumTypeSystemDefinition.onNewAstNode
     */
    protected onNewAstNodeExtended(
        _languageNode: Specifics["LanguageType"],
        _typir: ExpressionTypirServices<Specifics>
    ): void {}

    /**
     * Registers type conversion rules for primitive types.
     * Sets up implicit/explicit conversions between numeric types.
     *
     * @param conversion The conversion service
     */
    private registerTypeConversions(conversion: ExpressionTypirServices<Specifics>["Conversion"]): void {
        for (const isNullable of [false, true]) {
            const primitives = isNullable ? this.nullablePrimitiveTypes : this.primitiveTypes;
            conversion.markAsConvertible(primitives.int, primitives.long, ConversionMode.IMPLICIT_EXPLICIT);
            conversion.markAsConvertible(primitives.int, primitives.float, ConversionMode.IMPLICIT_EXPLICIT);
            conversion.markAsConvertible(primitives.int, primitives.double, ConversionMode.IMPLICIT_EXPLICIT);
            conversion.markAsConvertible(primitives.long, primitives.float, ConversionMode.IMPLICIT_EXPLICIT);
            conversion.markAsConvertible(primitives.long, primitives.double, ConversionMode.IMPLICIT_EXPLICIT);
            conversion.markAsConvertible(primitives.float, primitives.double, ConversionMode.IMPLICIT_EXPLICIT);
        }
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
            this.defaultTypeConfig.Iterable,
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
}
