import type { ValidationProblem, TypirSpecifics } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";

/**
 * Validate a property access expression.
 * Checks that the owner supports property access, handles nullable types,
 * and verifies that the property exists.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the property access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the property being accessed
 * @param allowNullChaining Whether null-safe chaining (?.) is used
 * @param services Extended Typir services for type operations
 * @returns Array of validation problems (empty if validation succeeds)
 */
export function validatePropertyAccess<Specifics extends TypirSpecifics>(
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
            message: `Type '${ownerType.getName()}' does not support property access.`,
            subProblems: []
        });
        return errors;
    }

    if (!allowNullChaining && ownerType.isNullable) {
        errors.push({
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message: `Cannot access property '${name}' on nullable type '${ownerType.getName()}' without null chaining.`,
            subProblems: []
        });
        return errors;
    }

    const property = ownerType.getProperty(name);
    if (property === undefined) {
        errors.push({
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message: `Property '${name}' does not exist on type '${ownerType.getName()}'.`,
            subProblems: []
        });
    }

    return errors;
}
