import type { ValidationProblem, TypirSpecifics } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";

/**
 * Validate a method access expression.
 * Checks that the owner supports method access, handles nullable types,
 * and verifies that the method exists.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the method access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the method being accessed
 * @param allowNullChaining Whether null-safe chaining (?.) is used
 * @param services Extended Typir services for type operations
 * @returns Array of validation problems (empty if validation succeeds)
 */
export function validateMethodAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    allowNullChaining: boolean,
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
            message: `Type '${ownerType.getName()}' does not support method access.`,
            subProblems: []
        });
        return errors;
    }

    if (!allowNullChaining && ownerType.isNullable) {
        errors.push({
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message: `Cannot access method '${name}' on nullable type '${ownerType.getName()}' without null chaining.`,
            subProblems: []
        });
        return errors;
    }

    const method = ownerType.getMethod(name);
    if (method === undefined) {
        errors.push({
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message: `Method '${name}' does not exist on type '${ownerType.getName()}'.`,
            subProblems: []
        });
    }

    return errors;
}
