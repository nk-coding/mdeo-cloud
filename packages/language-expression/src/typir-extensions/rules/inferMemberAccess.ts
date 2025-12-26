import type { InferenceProblem, TypirSpecifics } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { CustomValueType } from "../kinds/custom-value/custom-value-type.js";
import type { CustomFunctionType } from "../kinds/custom-function/custom-function-type.js";
import type { ValueType, FunctionType } from "../config/type.js";

/**
 * Infer the type of a member access expression.
 * Validates that the owner supports member access, handles nullable types,
 * and resolves the member type (property or method) including inheritance.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @param languageNode The AST node representing the member access expression
 * @param owner The AST node representing the object being accessed
 * @param name The name of the member being accessed
 * @param services Extended Typir services for type operations
 * @returns The inferred member type, or an inference problem if inference fails
 */
export function inferMemberAccess<Specifics extends TypirSpecifics>(
    languageNode: Specifics["LanguageType"],
    owner: Specifics["LanguageType"],
    name: string,
    services: ExtendedTypirServices<Specifics>
): InferenceProblem<Specifics> | CustomValueType | CustomFunctionType {
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
            location: `Type '${ownerType.getName()}' does not support member access.`,
            subProblems: []
        };
    }

    const member = ownerType.getMember(name);
    if (member == undefined) {
        return <InferenceProblem<Specifics>>{
            $problem: InferenceProblem,
            languageNode,
            location: `Member '${name}' does not exist on type '${ownerType.getName()}'.`,
            subProblems: []
        };
    }

    if (member.isProperty) {
        return services.TypeDefinitions.resolveCustomClassOrLambdaType(
            member.type as ValueType,
            ownerType.details.typeArgs as Map<string, CustomValueType>
        );
    } else {
        return services.TypeDefinitions.resolveCustomFunctionType(
            member.type as FunctionType,
            `${ownerType.getIdentifier()}.${name}`,
            ownerType.details.typeArgs as Map<string, CustomValueType>
        );
    }
}
