import type { TypirProblem, TypirSpecifics, ValidationProblem } from "typir";
import type { CustomFunctionType } from "../kinds/custom-function/custom-function-type.js";
import type { CustomLambdaType } from "../kinds/custom-lambda/custom-lambda-type.js";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import { CallValidationHelper } from "./callValidationHelper.js";
import { sharedImport } from "@mdeo/language-shared";

const { ValidationProblem: ValidationProblemConstant } = sharedImport("typir");

/**
 * Gets the chosen overload name for a function call.
 * Returns undefined if the call is to a lambda or if no overload could be determined.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the entire call expression
 * @param functionType The type of the function (must be a function type or lambda type)
 * @param genericArgumentsNodes AST nodes for explicit generic type arguments
 * @param argumentNodes AST nodes for the call arguments
 * @param services Extended Typir services for type operations
 * @returns The name of the chosen overload, or undefined
 */
export function getCallOverload<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    functionType: CustomFunctionType | CustomLambdaType,
    genericArgumentsNodes: Specifics["LanguageType"][],
    argumentNodes: Specifics["LanguageType"][],
    services: ExtendedTypirServices<Specifics>
): string | undefined {
    const helper = new OverloadCallHelper<Specifics>(
        languageNode,
        functionType,
        genericArgumentsNodes,
        argumentNodes,
        services,
        false
    );
    return helper.chosenOverloadName;
}

/**
 * Internal call helper for extracting overload information.
 * Creates validation problems when validation fails.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
class OverloadCallHelper<Specifics extends TypirSpecifics> extends CallValidationHelper<
    Specifics,
    ValidationProblem<Specifics>
> {
    protected override createError(
        languageNode: Specifics["LanguageType"],
        message: string,
        subProblems?: TypirProblem[]
    ): ValidationProblem<Specifics> {
        return {
            $problem: ValidationProblemConstant,
            languageNode,
            severity: "error",
            message,
            subProblems
        };
    }
}
