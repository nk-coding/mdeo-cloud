import type { DefaultTokenBuilder, Grammar, LangiumCoreServices, TokenBuilder, TokenBuilderOptions } from "langium";
import type { PluginContext } from "../../plugin/pluginContext.js";
import type { IMultiModeLexerDefinition, TokenType, TokenVocabulary } from "chevrotain";
import { HIDDEN_NEWLINE, NEWLINE } from "../../language/defaultTokens.js";

/**
 * Provides a newline aware token builder that can switch lexer modes based on specific tokens.
 *
 * @param context the plugin context
 * @param pushNewlineAwareModeTokens tokens that will make the lexer enter the newline aware mode
 * @param pushIgnoreNewlineModeTokens tokens that will make the lexer enter the ignore newline mode
 * @param popModeTokens tokens that will make the lexer exit the current mode and return to the previous one
 * @returns a service provider for the custom token builder
 */
export function generateNewlineAwareTokenBuilder(
    context: PluginContext,
    pushNewlineAwareModeTokens: Set<string>,
    pushIgnoreNewlineModeTokens: Set<string>,
    popModeTokens: Set<string>
): {
    TokenBuilder: (services: LangiumCoreServices) => TokenBuilder;
} {
    class NewlineAwareTokenBuilder extends context.langium.DefaultTokenBuilder {
        override buildTokens(grammar: Grammar, options?: TokenBuilderOptions): TokenVocabulary {
            const tokens = super.buildTokens(grammar, options);
            if (!Array.isArray(tokens)) {
                throw new Error("Expected tokens to be an array");
            }

            const defaultModeTokens = tokens.filter((token) => token.name !== HIDDEN_NEWLINE.name);
            const ignoreNewlineModeTokens = tokens.filter((token) => token.name !== NEWLINE.name);

            const lexerDefinition: IMultiModeLexerDefinition = {
                defaultMode: Modes.DEFAULT,
                modes: {
                    [Modes.DEFAULT]: defaultModeTokens,
                    [Modes.IGNORE_NEW_LINE]: ignoreNewlineModeTokens
                }
            };

            return lexerDefinition;
        }

        protected override buildKeywordToken(keyword: Keyword, terminalTokens: TokenType[], caseInsensitive: boolean) {
            const token = super.buildKeywordToken(keyword, terminalTokens, caseInsensitive);

            if (popModeTokens.has(keyword.value)) {
                token.POP_MODE = true;
            } else if (pushNewlineAwareModeTokens.has(keyword.value)) {
                token.PUSH_MODE = Modes.DEFAULT;
            } else if (pushIgnoreNewlineModeTokens.has(keyword.value)) {
                token.PUSH_MODE = Modes.IGNORE_NEW_LINE;
            }

            return token;
        }
    }

    return {
        TokenBuilder: () => new NewlineAwareTokenBuilder()
    };
}

/**
 * Non-exported langium keyword type
 */
type Keyword = Parameters<DefaultTokenBuilder["buildKeywordToken"]>[0];

/**
 * Supported lexer modes
 */
enum Modes {
    /**
     * Default mode, keep newlines in cst
     */
    DEFAULT = "DEFAULT",
    /**
     * Skip newlines in cst
     */
    IGNORE_NEW_LINE = "IGNORE_NEW_LINES"
}
