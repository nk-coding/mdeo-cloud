import type { InferenceProblem, TypirSpecifics } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { CustomFunctionType } from "../kinds/custom-function/custom-function-type.js";

/**
 * Infer the type of a method access expression.
 * Validates that the owner supports method access, handles nullable types,
 * and resolves the method type including inheritance.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the method access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the method being accessed
 * @param services Extended Typir services for type operations
 * @returns The inferred method function type, or an inference problem if inference fails
 */
export function inferMethodAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    services: ExtendedTypirServices<Specifics>
): InferenceProblem<Specifics> | CustomFunctionType {
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
            location: `Type '${ownerType.getName()}' does not support method access.`,
            subProblems: []
        };
    }
    const method = ownerType.getMethod(name);
    if (method == undefined) {
        return <InferenceProblem<Specifics>>{
            $problem: InferenceProblem,
            languageNode,
            location: `Method '${name}' does not exist on type '${ownerType.getName()}'.`,
            subProblems: []
        };
    }
    return method;
}
