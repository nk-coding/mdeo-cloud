import { action, createRule, group, ID, optional, or } from "@mdeo/language-common";
import { LeadingTrailing, manySep } from "@mdeo/language-shared";
import type { TypeConfig } from "./typeConfig.js";
import type { TypeTypes } from "./typeTypes.js";

/**
 * Generates type-related parser rules based on the provided configuration.
 *
 * @param config Configuration object containing naming for all type rules and types
 * @param types The generated type type interfaces to use as return types
 * @returns Object containing all generated type parser rules
 */
export function generateTypeRules(config: TypeConfig, types: TypeTypes) {
    const classTypeRule = createRule(config.classTypeRuleName)
        .returns(types.classTypeType)
        .as(({ set, add, flag }) => [
            set("name", ID),
            optional(
                "<",
                ...manySep(
                    add("typeArgs", () => classTypeRule),
                    ",",
                    LeadingTrailing.TRAILING
                ),
                ">"
            ),
            optional(flag("isNullable", "?"))
        ]);

    const voidTypeRule = createRule(config.voidTypeRuleName)
        .returns(types.voidTypeType)
        .as(() => [action(types.voidTypeType, () => ["void"])]);

    const returnTypeRule = createRule(config.returnTypeRuleName)
        .returns(types.returnTypeType)
        .as(() => [or(classTypeRule, voidTypeRule)]);

    const lambdaTypeParametersRule = createRule(config.lambdaTypeParametersRuleName)
        .returns(types.lambdaTypeParametersType)
        .as(({ add }) => [
            "(",
            ...manySep(
                add("parameters", () => classTypeRule),
                ",",
                LeadingTrailing.TRAILING
            ),
            ")"
        ]);

    const lambdaTypeRule = createRule(config.lambdaTypeRuleName)
        .returns(types.lambdaTypeType)
        .as(({ set, flag }) => {
            const inner = [set("parameterList", lambdaTypeParametersRule), "=>", set("returnType", returnTypeRule)];
            return [or(group(...inner), flag("isNullable", group("(", ...inner, ")", "?")))];
        });

    const typeRule = createRule(config.typeRuleName)
        .returns(types.baseTypeType)
        .as(() => [or(classTypeRule, lambdaTypeRule)]);

    return {
        typeRule,
        returnTypeRule
    };
}
