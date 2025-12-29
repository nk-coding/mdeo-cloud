import type { AstNode, LangiumCoreServices } from "langium";
import type { PluginContext } from "../../plugin/pluginContext.js";
import type { AstSerializerAdditionalServices } from "./astSerializer.js";
import { FLOAT, ID, INT, STRING } from "../../language/defaultTokens.js";

/**
 * Registers default serializers for common token types.
 *
 * @param context the plugin context
 * @param services the Langium core services with AST serializer services
 */
export function registerDefaultTokenSerializers(
    { langium }: PluginContext,
    services: LangiumCoreServices & AstSerializerAdditionalServices
): void {
    const { AstSerializer, Grammar } = services;

    const keywords = langium
        .stream(langium.GrammarUtils.getAllReachableRules(Grammar, false))
        .flatMap((rule) =>
            langium.AstUtils.streamAllContents(rule).filter((potentialKeyword) => potentialKeyword.$type === "Keyword")
        )
        .map((keyword) => (keyword as { value: string } & AstNode).value)
        .toSet();

    AstSerializer.registerPrimitiveSerializer(INT, ({ value, cstNode }) => cstNode?.text ?? value.toString());
    AstSerializer.registerPrimitiveSerializer(FLOAT, ({ value, cstNode }) => cstNode?.text ?? value.toString());
    AstSerializer.registerPrimitiveSerializer(STRING, ({ value, cstNode }) => cstNode?.text ?? JSON.stringify(value));
    AstSerializer.registerPrimitiveSerializer(ID, ({ value, cstNode }) => {
        if (cstNode != undefined) {
            return cstNode.text;
        }
        if (!/^[\p{ID_Start}][\p{ID_Continue}]*$/u.test(value) || keywords.has(value)) {
            return `\`${value}\``;
        }
        return value;
    });
}
