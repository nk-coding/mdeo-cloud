import { createRule, ID, LeadingTrailing, manySep, optional, or, type ParserRule } from "@mdeo/language-common";
import type { TypeConfig } from "./typeConfig.js";
import type { BaseTypeType, TypeTypes } from "./typeTypes.js";

/**
 * Generates type-related parser rules based on the provided configuration.
 *
 * @param config Configuration object containing naming for all type rules and types
 * @param types The generated type type interfaces to use as return types
 * @returns Object containing all generated type parser rules
 */
export function generateTypeRules(config: TypeConfig, types: TypeTypes): ParserRule<BaseTypeType> {
    const classTypeRule: ParserRule<any> = createRule(config.classTypeRuleName)
        .returns(types.classTypeType)
        .as(({ set, add }) => [
            set("name", ID),
            optional(
                "<",
                ...manySep(
                    add("typeArgs", () => classTypeRule),
                    ",",
                    LeadingTrailing.TRAILING
                ),
                ">"
            )
        ]);

    const lambdaTypeRule = createRule(config.lambdaTypeRuleName)
        .returns(types.lambdaTypeType)
        .as(({ set, add }) => [
            "(",
            ...manySep(
                add("parameters", () => classTypeRule),
                ",",
                LeadingTrailing.TRAILING
            ),
            ")",
            "=>",
            set("returnType", classTypeRule)
        ]);

    const typeRule = createRule(config.typeRuleName)
        .returns(types.baseTypeType)
        .as(() => [or(classTypeRule, lambdaTypeRule)]);

    return typeRule;
}
