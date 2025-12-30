import type { AstPath, Doc, ParserOptions } from "prettier";
import type { Builders, Comment, PrintContext } from "./types.js";
import type { AstNode } from "langium";
import { ML_COMMENT } from "../../language/defaultTokens.js";

/**
 * Prints all dangling comments
 *
 * @param path the current path
 * @param options the formatting options
 * @returns the formatted comments
 */
export function printDanglingComments({ ctx, path, options }: PrintContext<any>, builders: Builders): Doc[] {
    const comments = ctx.comments;
    if (Array.isArray(comments) && comments.length > 0) {
        const parts: Doc = [];
        (path as AstPath<{ comments: Comment[] } & AstNode>).each((comment) => {
            const commentToken = comment.node;
            if (commentToken.trailing || commentToken.leading) {
                return;
            }
            const commentDoc = printComment(commentToken, options, builders);
            if (commentToken.node.tokenType.name === ML_COMMENT.name) {
                parts.push(commentDoc);
            } else {
                parts.push(builders.lineSuffix(commentDoc));
            }
        }, "comments");
        return [builders.join(builders.hardline, parts)];
    } else {
        return [];
    }
}
/**
 * Prints a comment
 *
 * @param comment the comment to print
 * @param options the formatting options
 * @param builders Prettier doc builders
 * @returns the formatted comment
 */

export function printComment(comment: Comment, options: ParserOptions<AstNode>, builders: Builders): Doc {
    comment.printed = true;
    if (comment.node.tokenType.name === ML_COMMENT.name) {
        const image = comment.node.text;
        const leadingWhitespaceLength = calculateWhitespaceLength(
            getLeadingWhitespace(options.originalText, options.locStart(comment as any)),
            options.tabWidth
        );
        const lines = image
            .split("\n")
            .map((line) => decreaseIdentation(line, leadingWhitespaceLength, options.tabWidth));
        return builders.join(builders.hardline, lines);
    } else {
        return comment.node.text;
    }
}
/**
 * Gets the leading whitespace of a given offset until the last non-whitespace character or newline
 *
 * @param text the text to get the leading whitespace from
 * @param offset the offset to start from
 * @returns the leading whitespace
 */
function getLeadingWhitespace(text: string, offset: number): string {
    let start = offset;
    while (start > 0 && text[start - 1].match(/[^\S\n]/)) {
        start--;
    }
    return text.substring(start, offset);
}
/**
 * Calculates the whitespace length of a given text
 *
 * @param text the purely whitespace text
 * @param tabWidth the width of a tab
 * @returns the length of the text
 */
function calculateWhitespaceLength(text: string, tabWidth: number): number {
    let length = 0;
    for (let i = 0; i < text.length; i++) {
        if (text[i] === "\t") {
            length += tabWidth;
        } else {
            length++;
        }
    }
    return length;
}
/**
 * Decreases the indentation of a given text by a given length
 * If a tab causes a larger dedentation than desired, spaces are used to replace it
 *
 * @param text the text to dedent
 * @param length the length to dedent
 * @param tabWidth the width of a tab
 * @returns the dedented text
 */
function decreaseIdentation(text: string, length: number, tabWidth: number): string {
    let i = 0;
    while (length > 0 && i < text.length && text[i].match(/[^\S\n]/)) {
        if (text[i] === "\t") {
            length -= tabWidth;
        } else {
            length--;
        }
        i++;
    }
    const leadingSpaces = " ".repeat(Math.max(-length, 0));
    return leadingSpaces + text.substring(i);
}
