import type { DefaultTokenBuilder, Grammar, TokenBuilderOptions } from "langium";
import type { IMultiModeLexerDefinition, TokenType, TokenVocabulary } from "chevrotain";
import { HIDDEN_NEWLINE, ID, NEWLINE } from "@mdeo/language-common";
import { sharedImport } from "../sharedImport.js";

const { DefaultTokenBuilder: DefaultTokenBuilderBase } = sharedImport("langium");

/**
 * Newline aware token builder that can switch lexer modes based on specific tokens.
 *
 * @param pushNewlineAwareModeTokens tokens that will make the lexer enter the newline aware mode
 * @param pushIgnoreNewlineModeTokens tokens that will make the lexer enter the ignore newline mode
 * @param popModeTokens tokens that will make the lexer exit the current mode and return to the previous one
 */
export class NewlineAwareTokenBuilder extends DefaultTokenBuilderBase {
    constructor(
        private readonly pushNewlineAwareModeTokens: Set<string>,
        private readonly pushIgnoreNewlineModeTokens: Set<string>,
        private readonly popModeTokens: Set<string>
    ) {
        super();
    }

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

        if (this.popModeTokens.has(keyword.value)) {
            token.POP_MODE = true;
        } else if (this.pushNewlineAwareModeTokens.has(keyword.value)) {
            token.PUSH_MODE = Modes.DEFAULT;
        } else if (this.pushIgnoreNewlineModeTokens.has(keyword.value)) {
            token.PUSH_MODE = Modes.IGNORE_NEW_LINE;
        }

        return token;
    }

    protected override findLongerAlt(keyword: Keyword, terminalTokens: TokenType[]): TokenType[] {
        const longerAlts = super.findLongerAlt(keyword, terminalTokens);
        if (/^[\p{ID_Start}][\p{ID_Continue}]*$/v.test(keyword.value)) {
            const idToken = terminalTokens.find((t) => t.name === ID.name);
            if (idToken != undefined) {
                longerAlts.push(idToken);
            }
        }
        return longerAlts;
    }
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
