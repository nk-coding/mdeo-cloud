import type { InferenceProblem, TypirSpecifics, Type as TypirType } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { CustomValueType } from "../kinds/custom-value/custom-value-type.js";

/**
 * Infer the type of a property access expression.
 * Validates that the owner supports property access, handles nullable types,
 * and resolves the property type including inheritance.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the property access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the property being accessed
 * @param services Extended Typir services for type operations
 * @returns The inferred property type, or an inference problem if inference fails
 */
export function inferPropertyAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    services: ExtendedTypirServices<Specifics>
): InferenceProblem<Specifics> | CustomValueType {
    const { InferenceProblem } = services.context.typir;
    const ownerType = services.Inference.inferType(owner);
    if (!services.factory.CustomClasses.isCustomClassType(ownerType)) {
        if (Array.isArray(ownerType)) {
            return <InferenceProblem<Specifics>>{
                $problem: InferenceProblem,
                languageNode,
                location: `Cannot infer type`,
                subProblems: ownerType
            };
        }
        return <InferenceProblem<Specifics>>{
            $problem: InferenceProblem,
            languageNode,
            location: `Type '${ownerType.getName()}' does not support property access.`,
            subProblems: []
        };
    }
    const property = ownerType.getProperty(name);
    if (property == undefined) {
        return <InferenceProblem<Specifics>>{
            $problem: InferenceProblem,
            languageNode,
            location: `Property '${name}' does not exist on type '${ownerType.getName()}'.`,
            subProblems: []
        };
    }
    if (ownerType.isNullable) {
        return property.asNullable;
    }
    return property;
}
