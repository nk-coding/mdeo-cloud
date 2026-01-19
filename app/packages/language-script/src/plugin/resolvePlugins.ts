import {
    GrammarDeserializer,
    isTerminalRule,
    type GrammarDeserializationContext,
    type Interface,
    type ParserRule
} from "@mdeo/language-common";
import type {
    ResolvedContributedExpression,
    ResolvedScriptContributionPlugins,
    ScriptContributionPlugin,
    ResolvedContributedFunction
} from "./scriptContributionPlugin.js";
import { FunctionSignature } from "@mdeo/language-expression";

/**
 *  Resolves the contribution plugins into a unified structure.
 *
 * @param plugins The contribution plugins
 * @param deserializationContext The deserialization context
 * @returns The resolved plugins
 * @throws Error if a plugin with expression contributions does not define a grammar
 */
export function resolvePlugins(
    plugins: ScriptContributionPlugin[],
    deserializationContext: GrammarDeserializationContext
): ResolvedScriptContributionPlugins {
    const extensionRules: ParserRule<any>[] = [];
    const expressions: ResolvedContributedExpression[] = [];
    for (const plugin of plugins) {
        if (Object.keys(plugin.expressions).length == 0) {
            continue;
        }
        if (plugin.grammar == undefined) {
            throw new Error("Plugin with expression contributions must define a grammar.");
        }
        const deserializer = new GrammarDeserializer(plugin.grammar, deserializationContext);
        const grammar = deserializer.deserializeGrammar();
        const rulesLookup = new Map<string, ParserRule<any>>();
        for (const rule of grammar.rules) {
            if (!isTerminalRule(rule)) {
                rulesLookup.set(rule.name, rule);
            }
        }
        const interfacesLookup = new Map<string, Interface<any>>();
        for (const type of grammar.interfaces) {
            interfacesLookup.set(type.name, type);
        }
        for (const [expressionName, expression] of Object.entries(plugin.expressions)) {
            const rule = rulesLookup.get(expression.ruleName);
            const type = interfacesLookup.get(expression.interfaceName);
            if (rule == undefined) {
                throw new Error(`Expression rule '${expression.ruleName}' not found in plugin grammar.`);
            }
            if (type == undefined) {
                throw new Error(`Expression interface '${expression.interfaceName}' not found in plugin grammar.`);
            }
            extensionRules.push(rule);
            expressions.push({
                signature: expression.function.signature,
                interface: type,
                name: expressionName
            });
        }
    }

    return {
        functions: resolveFunctions(plugins),
        expressions: expressions,
        rules: extensionRules
    };
}

/**
 * Extracts functions contributed by plugins into a map of function members by name.
 *
 * @param plugins The contribution plugins
 * @returns The resolved contributed functions
 * @throws Error if there are duplicate function or expression names
 */
function resolveFunctions(plugins: ScriptContributionPlugin[]): Map<string, ResolvedContributedFunction> {
    const functions = new Map<string, ResolvedContributedFunction>();
    for (const plugin of plugins) {
        for (const [functionName, contributedFunction] of Object.entries(plugin.functions)) {
            const func = {
                signatures: Object.fromEntries(
                    Object.entries(contributedFunction.signatures).map(([signatureName, signature]) => [
                        signatureName,
                        signature.signature
                    ])
                )
            };
            if (functions.has(functionName)) {
                throw new Error(`Duplicate function or expression name '${functionName}' contributed by plugins.`);
            }
            functions.set(functionName, { function: func, contributedFunction, types: plugin.types });
        }
        for (const [expressionName, contributedExpression] of Object.entries(plugin.expressions)) {
            const func = {
                signatures: {
                    [FunctionSignature.DEFAULT_SIGNATURE]: contributedExpression.function.signature
                }
            };
            if (functions.has(expressionName)) {
                throw new Error(`Duplicate function or expression name '${expressionName}' contributed by plugins.`);
            }
            functions.set(expressionName, {
                function: func,
                contributedFunction: {
                    signatures: { [FunctionSignature.DEFAULT_SIGNATURE]: contributedExpression.function }
                },
                types: plugin.types
            });
        }
    }
    return functions;
}
