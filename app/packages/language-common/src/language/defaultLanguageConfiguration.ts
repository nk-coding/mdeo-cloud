import type { languages } from "monaco-editor";

/**
 * Default language configuration for Monaco Editor
 */
export const defaultLanguageConfiguration: languages.LanguageConfiguration = {
    surroundingPairs: [
        { open: "{", close: "}" },
        { open: "(", close: ")" },
        { open: "[", close: "]" },
        { open: '"', close: '"' },
        { open: "`", close: "`" }
    ],
    autoClosingPairs: [
        { open: "{", close: "}" },
        { open: "(", close: ")" },
        { open: "[", close: "]" },
        { open: '"', close: '"' },
        { open: "`", close: "`" }
    ],
    brackets: [
        ["{", "}"],
        ["(", ")"],
        ["[", "]"]
    ],
    colorizedBracketPairs: [
        ["{", "}"],
        ["(", ")"],
        ["[", "]"]
    ],
    comments: {
        lineComment: "//",
        blockComment: ["/*", "*/"]
    }
} as const;
