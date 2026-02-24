import type { InferenceProblem, TypirSpecifics } from "typir";
import { sharedImport } from "@mdeo/language-shared";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { CustomValueType } from "../kinds/custom-value/custom-value-type.js";
import { isCustomValueType } from "../kinds/custom-value/custom-value-type.js";
import type { CustomFunctionType } from "../kinds/custom-function/custom-function-type.js";
import type { ValueType, FunctionType } from "../config/type.js";

const { InferenceProblem: InferenceProblemConstant } = sharedImport("typir");

/**
 * Resolves the owner to a CustomValueType, or returns inference problems if it cannot.
 */
function resolveOwnerValueType<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    services: ExtendedTypirServices<Specifics>
): InferenceProblem<Specifics>[] | CustomValueType {
    const ownerType = services.Inference.inferType(owner);
    if (isCustomValueType(ownerType)) {
        return ownerType;
    }
    if (Array.isArray(ownerType)) {
        return [
            <InferenceProblem<Specifics>>{
                $problem: InferenceProblemConstant,
                languageNode,
                location: `Cannot infer type`,
                subProblems: ownerType
            }
        ];
    }
    return [
        <InferenceProblem<Specifics>>{
            $problem: InferenceProblemConstant,
            languageNode,
            location: `Type '${ownerType.getName()}' does not support member access.`,
            subProblems: []
        }
    ];
}

/**
 * Infer the type of a property access expression.
 * Validates that the owner supports member access and resolves the property type including inheritance.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the member access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the property being accessed
 * @param services Extended Typir services for type operations
 * @returns The inferred property type, or inference problems if inference fails
 */
export function inferPropertyAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    services: ExtendedTypirServices<Specifics>
): InferenceProblem<Specifics>[] | CustomValueType {
    const ownerType = resolveOwnerValueType(languageNode, owner, services);
    if (Array.isArray(ownerType)) {
        return ownerType;
    }

    const prop = ownerType.getProperty(name);
    if (prop == undefined) {
        return [
            <InferenceProblem<Specifics>>{
                $problem: InferenceProblemConstant,
                languageNode,
                location: `Property '${name}' does not exist on type '${ownerType.getName()}'.`,
                subProblems: []
            }
        ];
    }
    return services.TypeDefinitions.resolveCustomClassOrLambdaType(
        prop.type as ValueType,
        ownerType.details.typeArgs as Map<string, CustomValueType>
    );
}

/**
 * Infer the type of a method access expression.
 * Validates that the owner supports member access and resolves the method type including inheritance.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the member access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the method being accessed
 * @param services Extended Typir services for type operations
 * @returns The inferred method type, or inference problems if inference fails
 */
export function inferMethodAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    services: ExtendedTypirServices<Specifics>
): InferenceProblem<Specifics>[] | CustomValueType | CustomFunctionType {
    const ownerType = resolveOwnerValueType(languageNode, owner, services);
    if (Array.isArray(ownerType)) {
        return ownerType;
    }

    const method = ownerType.getMethod(name);
    if (method == undefined) {
        return [
            <InferenceProblem<Specifics>>{
                $problem: InferenceProblemConstant,
                languageNode,
                location: `Method '${name}' does not exist on type '${ownerType.getName()}'.`,
                subProblems: []
            }
        ];
    }
    return services.TypeDefinitions.resolveCustomFunctionType(
        method.type as FunctionType,
        `${ownerType.getIdentifier()}.${name}`,
        ownerType.details.typeArgs as Map<string, CustomValueType>
    );
}
