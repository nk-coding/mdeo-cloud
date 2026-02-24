import type { ValidationProblem, TypirSpecifics } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import { isCustomClassType } from "../kinds/custom-class/custom-class-type.js";
import { sharedImport } from "@mdeo/language-shared";

const { ValidationProblem: ValidationProblemConstant } = sharedImport("typir");

/**
 * Validate a property access expression.
 * Checks that the owner supports member access, handles nullable types,
 * and verifies that the property exists.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the member access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the member being accessed
 * @param isNullChaining Whether null-safe chaining (?.) is used
 * @param services Extended Typir services for type operations
 * @returns Array of validation problems (empty if validation succeeds)
 */
export function validatePropertyAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    isNullChaining: boolean,
    services: ExtendedTypirServices<Specifics>
): ValidationProblem<Specifics>[] {
    return validateMemberAccess(languageNode, owner, name, isNullChaining, services, true);
}

/**
 * Validate a method access expression.
 * Checks that the owner supports member access, handles nullable types,
 * and verifies that the method exists.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the member access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the member being accessed
 * @param isNullChaining Whether null-safe chaining (?.) is used
 * @param services Extended Typir services for type operations
 * @returns Array of validation problems (empty if validation succeeds)
 */
export function validateMethodAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    isNullChaining: boolean,
    services: ExtendedTypirServices<Specifics>
): ValidationProblem<Specifics>[] {
    return validateMemberAccess(languageNode, owner, name, isNullChaining, services, false);
}

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
 * @param isProperty Whether to look up a property (true) or method (false)
 * @returns Array of validation problems (empty if validation succeeds)
 */
function validateMemberAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    isNullChaining: boolean,
    services: ExtendedTypirServices<Specifics>,
    isProperty: boolean
): ValidationProblem<Specifics>[] {
    const errors: ValidationProblem<Specifics>[] = [];

    const ownerType = services.Inference.inferType(owner);
    if (Array.isArray(ownerType)) {
        return [];
    }

    if (!isCustomClassType(ownerType)) {
        errors.push({
            $problem: ValidationProblemConstant,
            languageNode,
            severity: "error",
            message: `Type '${ownerType.getName()}' does not support member access.`,
            subProblems: []
        });
        return errors;
    }

    if (!isNullChaining && ownerType.isNullable) {
        errors.push({
            $problem: ValidationProblemConstant,
            languageNode,
            severity: "error",
            message: `Cannot access member '${name}' on nullable type '${ownerType.getName()}' without null chaining.`,
            subProblems: []
        });
        return errors;
    }

    const memberExists = isProperty ? ownerType.getProperty(name) != undefined : ownerType.getMethod(name) != undefined;

    if (!memberExists) {
        errors.push({
            $problem: ValidationProblemConstant,
            languageNode,
            severity: "error",
            message: `${isProperty ? "Property" : "Method"} '${name}' does not exist on type '${ownerType.getName()}'.`,
            subProblems: []
        });
    }

    return errors;
}
