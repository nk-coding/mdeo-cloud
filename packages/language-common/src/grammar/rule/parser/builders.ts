import type { AstNode, GrammarAST } from "langium";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import type { BaseType } from "../../type/types.js";
import type { RuleEntry, RuleContext } from "./types.js";
import { groupIfNeeded, defaultRuleContext } from "./helpers.js";
import type { ParserRule } from "../types.js";

/**
 * Initial builder for creating parser rules. This is the entry point for
 * building grammar rules that parse specific AST node types.
 */
export class RuleBuilder {
    /**
     * Creates a new rule builder.
     * 
     * @param name The unique name for this parser rule in the grammar
     */
    constructor(readonly name: string) {}

    /**
     * Specifies the return type for this parser rule. The return type defines
     * the AST node interface that this rule will produce when parsed.
     * 
     * @template T The AST node type this rule will produce
     * @param type The interface or type definition for the AST node
     * @returns A builder for defining the parsing logic
     * 
     * @example
     * ```typescript
     * const PersonRule = createRule("Person")
     *     .returns(PersonInterface)  // Specify the AST node type
     *     .as(({ set, add, flag }) => [...]);
     * ```
     */
    returns<T extends AstNode>(type: BaseType<T>): RuleBuilderWithType<T> {
        return new RuleBuilderWithType(this.name, type);
    }
}

/**
 * Builder for parser rules that have a return type specified. This builder
 * allows defining the actual parsing logic through a rule definition function.
 * 
 * @template T The AST node type this rule will produce
 */
export class RuleBuilderWithType<T extends AstNode> {
    /**
     * Creates a typed rule builder.
     * 
     * @param name The unique name for this parser rule
     * @param type The AST node type this rule will produce
     */
    constructor(private readonly name: string, private readonly type: BaseType<T>) {}

    /**
     * Defines the parsing logic for this rule using a function that returns
     * rule entries. The function receives a context object with methods for
     * creating assignments to populate the AST node properties.
     * 
     * @param rule Function that defines the parsing structure using the rule context
     * @returns A complete parser rule that can be used in grammar generation
     * 
     * @example
     * ```typescript
     * .as(({ set, add, flag }) => [
     *     set("name", STRING_TERMINAL),        // Assign name property
     *     optional(set("age", NUMBER_TERMINAL)), // Optional age property
     *     many(add("tags", TAG_TERMINAL)),     // Multiple tags
     *     flag("isActive", "active")           // Boolean flag
     * ])
     * ```
     */
    as(rule: (context: RuleContext<T>) => RuleEntry[]): ParserRule<T> {
        let initialized = false;
        const createdRule: SerializableGrammarNode<GrammarAST.ParserRule> = {
            $type: "ParserRule",
            name: this.name,
            returnType: () => this.type,
            fragment: false,
            entry: false,
            definition: {
                $type: "EndOfFile"
            },
            parameters: []
        };
        return {
            toRule: () => {
                if (!initialized) {
                    initialized = true;
                    createdRule.definition = groupIfNeeded(rule(defaultRuleContext));
                }
                return createdRule;
            }
        };
    }
}
