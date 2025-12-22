import type { TypirProblem, TypirSpecifics, ValidationProblem } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import { CallValidationHelper } from "./callValidationHelper.js";

/**
 * Validate a function or lambda call.
 * Checks that arguments match parameters, and all type constraints are satisfied.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the entire call expression
 * @param functionReferenceNode The AST node representing the function/lambda being called
 * @param genericArgumentsNodes AST nodes for explicit generic type arguments
 * @param argumentNodes AST nodes for the call arguments
 * @param services Extended Typir services for type operations
 * @returns Array of validation problems (empty if validation succeeds)
 */
export function validateCall<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    functionReferenceNode: Specifics["LanguageType"],
    genericArgumentsNodes: Specifics["LanguageType"][],
    argumentNodes: Specifics["LanguageType"][],
    services: ExtendedTypirServices<Specifics>
): ValidationProblem<Specifics>[] {
    const validator = new InferenceCallValidator<Specifics>(
        languageNode,
        functionReferenceNode,
        genericArgumentsNodes,
        argumentNodes,
        services,
        false
    );
    return validator.errors;
}

/**
 * Internal call validator for validation mode.
 * Creates validation problems when validation fails.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
class InferenceCallValidator<Specifics extends TypirSpecifics> extends CallValidationHelper<
    Specifics,
    ValidationProblem<Specifics>
> {
    protected override createError(
        languageNode: Specifics["LanguageType"],
        message: string,
        subProblems?: TypirProblem[]
    ): ValidationProblem<Specifics> {
        const { ValidationProblem } = this.services.context.typir;
        return {
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message,
            subProblems
        };
    }
}
