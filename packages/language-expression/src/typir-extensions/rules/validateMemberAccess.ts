import type { ValidationProblem, TypirSpecifics } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";

/**
 * Validate a member access expression.
 * Checks that the owner supports member access, handles nullable types,
 * and verifies that the member (property or method) exists.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the member access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the member being accessed
 * @param isNullChaining Whether null-safe chaining (?.) is used
 * @param services Extended Typir services for type operations
 * @returns Array of validation problems (empty if validation succeeds)
 */
export function validateMemberAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    isNullChaining: boolean,
    services: ExtendedTypirServices<Specifics>
): ValidationProblem<Specifics>[] {
    const errors: ValidationProblem<Specifics>[] = [];
    const { ValidationProblem } = services.context.typir;

    const ownerType = services.Inference.inferType(owner);
    if (Array.isArray(ownerType)) {
        return [];
    }

    if (!services.factory.CustomClasses.isCustomClassType(ownerType)) {
        errors.push({
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message: `Type '${ownerType.getName()}' does not support member access.`,
            subProblems: []
        });
        return errors;
    }

    if (!isNullChaining && ownerType.isNullable) {
        errors.push({
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message: `Cannot access member '${name}' on nullable type '${ownerType.getName()}' without null chaining.`,
            subProblems: []
        });
        return errors;
    }

    if (ownerType.getProperty(name) == undefined && ownerType.getMethod(name) == undefined) {
        errors.push({
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message: `Member '${name}' does not exist on type '${ownerType.getName()}'.`,
            subProblems: []
        });
    }

    return errors;
}
