import type { CstNode, DefaultValueConverter, ValueType } from "langium";
import { sharedImport } from "../sharedImport.js";

const { DefaultValueConverter: DefaultValueConverterBase } = sharedImport("langium");

/**
 * ID value converter that supports backtick-quoted IDs.
 */
export class IdValueConverter extends DefaultValueConverterBase {
    protected override runConverter(
        rule: Parameters<DefaultValueConverter["runConverter"]>[0],
        input: string,
        cstNode: CstNode
    ): ValueType {
        if (rule.name === "ID") {
            return parseIdentifier(input);
        }
        return super.runConverter(rule, input, cstNode);
    }
}

/**
 * Parses an identifier, removing backticks if present.
 * 
 * @param identifier the identifier to parse
 * @returns the parsed identifier without backticks
 */
export function parseIdentifier(identifier: string): string {
    if (identifier.charAt(0) === "`") {
        return identifier.substring(1, identifier.length - 1);
    } else {
        return identifier;
    }
}
