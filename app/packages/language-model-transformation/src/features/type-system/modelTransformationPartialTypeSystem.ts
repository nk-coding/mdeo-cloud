import { PartialTypeSystem, type ExpressionTypirServices } from "@mdeo/language-expression";
import type { TypirLangiumSpecifics } from "typir-langium";

/**
 * Partial type system for Model Transformation-specific AST nodes.
 * This serves as infrastructure for future type checking rules.
 *
 * Currently empty - implementation to be added when specific
 * type inference and validation rules are needed for transformation constructs.
 */
export class ModelTransformationPartialTypeSystem extends PartialTypeSystem<
    TypirLangiumSpecifics,
    Record<string, never>
> {
    /**
     * Creates an instance of ModelTransformationPartialTypeSystem.
     *
     * @param typir The typir services for Model Transformation.
     */
    constructor(typir: ExpressionTypirServices<TypirLangiumSpecifics>) {
        super(typir, {});
    }

    /**
     * Registers type inference and validation rules.
     * Currently empty - to be implemented when specific rules are needed.
     */
    override registerRules(): void {
        // Infrastructure ready for future type rules
        // No implementation for now as per requirements
    }
}
