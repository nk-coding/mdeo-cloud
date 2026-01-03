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
            if (input.charAt(0) === "`") {
                return input.substring(1, input.length - 1);
            } else {
                return input;
            }
        }
        return super.runConverter(rule, input, cstNode);
    }
}
