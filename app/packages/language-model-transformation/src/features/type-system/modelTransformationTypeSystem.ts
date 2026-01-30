import {
    AnyType,
    booleanType,
    doubleType,
    ExpressionTypeSystem,
    floatType,
    intType,
    IterableType,
    longType,
    stringType,
    TypePartialTypeSystem,
    type PrimitiveTypes,
    type ExpressionTypirServices
} from "@mdeo/language-expression";
import type { TypirLangiumSpecifics } from "typir-langium";
import { expressionTypes, typeTypes } from "../../grammar/modelTransformationTypes.js";
import { ModelTransformationPartialTypeSystem } from "./modelTransformationPartialTypeSystem.js";

/**
 * Type system for the Model Transformation language.
 * Provides type inference and validation for transformation expressions.
 */
export class ModelTransformationTypeSystem extends ExpressionTypeSystem<TypirLangiumSpecifics> {
    /**
     * Creates an instance of ModelTransformationTypeSystem.
     */
    constructor() {
        super(
            {
                Any: AnyType,
                int: intType,
                long: longType,
                float: floatType,
                double: doubleType,
                string: stringType,
                boolean: booleanType,
                Iterable: IterableType,
                additionalTypes: [],
                lambdaSuperTypes: [{ type: AnyType.name }]
            },
            expressionTypes,
            []
        );
    }

    /**
     * Returns the primitive types configuration.
     *
     * @returns The primitive types used by the type system.
     */
    getPrimitiveTypes(): PrimitiveTypes {
        return this.primitiveTypes;
    }

    /**
     * Initializes extended type system components.
     * Registers partial type systems for specific language constructs.
     *
     * @param typir The typir services to initialize with.
     */
    protected override onInitializeExtended(typir: ExpressionTypirServices<TypirLangiumSpecifics>): void {
        const typePartialTypeSystem = new TypePartialTypeSystem<TypirLangiumSpecifics>(
            typir,
            typeTypes,
            this.nullablePrimitiveTypes.Any
        );
        typePartialTypeSystem.registerRules();

        const modelTransformationPartialTypeSystem = new ModelTransformationPartialTypeSystem(typir);
        modelTransformationPartialTypeSystem.registerRules();
    }
}
