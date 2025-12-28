import type { CstNode, DefaultValueConverter, LangiumCoreServices, ValueConverter, ValueType } from "langium";
import type { PluginContext } from "../../plugin/pluginContext.js";

/**
 * Generates an ID value converter that supports backtick-quoted IDs.
 *
 * @param context the plugin context
 * @returns an object containing the ValueConverter provider
 */
export function generateIdValueConverter(context: PluginContext): {
    ValueConverter: (services: LangiumCoreServices) => ValueConverter;
} {
    class IdValueConverter extends context.langium.DefaultValueConverter {
        protected override runConverter(
            rule: Parameters<DefaultValueConverter["runConverter"]>[0],
            input: string,
            cstNode: CstNode
        ): ValueType {
            if (rule.name === "ID") {
                if (input.charAt(0) === "`") {
                    return input.substring(1, input.length - 1);
                } else {
                    return input;
                }
            }
            return super.runConverter(rule, input, cstNode);
        }
    }

    return {
        ValueConverter: () => new IdValueConverter()
    };
}
