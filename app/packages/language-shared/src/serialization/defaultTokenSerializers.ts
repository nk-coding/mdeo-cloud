import type { AstNode, LangiumCoreServices } from "langium";
import { FLOAT, ID, INT, STRING, type AstSerializerAdditionalServices } from "@mdeo/language-common";
import { sharedImport } from "../sharedImport.js";

const { stream, GrammarUtils, AstUtils } = sharedImport("langium");

/**
 * Registers default serializers for common token types.
 *
 * @param services the Langium core services with AST serializer services
 */
export function registerDefaultTokenSerializers(services: LangiumCoreServices & AstSerializerAdditionalServices): void {
    const { AstSerializer, Grammar } = services;

    const keywords = stream(GrammarUtils.getAllReachableRules(Grammar, false))
        .flatMap((rule) =>
            AstUtils.streamAllContents(rule).filter((potentialKeyword) => potentialKeyword.$type === "Keyword")
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
