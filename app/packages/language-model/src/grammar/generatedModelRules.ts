import { createRule, createTerminal } from "@mdeo/language-common";
import { GeneratedModel } from "./generatedModelTypes.js";

/**
 * Terminal that matches any content.
 * Captures the entire file content as a single string.
 */
export const JSON_CONTENT = createTerminal("JSON_CONTENT")
    .returns(String)
    .as(/{[\s\S]+}/);

/**
 * Generated model root rule.
 * The AST consists of a single root rule that captures the entire file content
 * via a custom terminal that matches anything.
 */
export const GeneratedModelRule = createRule("GeneratedModelRule")
    .returns(GeneratedModel)
    .as(({ set }) => [set("content", JSON_CONTENT)]);
