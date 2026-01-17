import type { AstNode, GrammarAST } from "langium";
import type { BaseType } from "../../type/types.js";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import { Associativity } from "./types.js";
import type { ParserRule } from "../types.js";

/**
 * Initial builder for creating infix rules. This is the entry point for building
 * infix operator rules in the grammar.
 */
export class InfixRuleBuilder {
    /**
     * Creates a new infix rule builder.
     *
     * @param name The unique name for this infix rule in the grammar
     */
    constructor(private readonly name: string) {}

    /**
     * Specifies the base rule that this infix rule operates on. The base rule
     * defines the operands that the infix operators will work with.
     *
     * @template T The AST node type produced by the base rule
     * @param rule The parser rule that defines the operand type
     * @returns A builder for configuring the return type
     *
     * @example
     * ```typescript
     * const ExpressionRule = createInfixRule("ExpressionRule")
     *     .on(NumberRule)  // Numbers are the operands
     *     .returns(BinaryExpression)
     *     .operators("+", "-");
     * ```
     */
    on<T extends AstNode>(rule: ParserRule<T>) {
        return new InfixRuleBuilderWithCall(this.name, rule);
    }
}

/**
 * Builder for infix rules that have a base rule specified but no return type yet.
 *
 * @template T The AST node type produced by the base rule
 */
export class InfixRuleBuilderWithCall<T extends AstNode> {
    /**
     * Creates a builder with a specified base rule.
     *
     * @param name The unique name for this infix rule
     * @param call The base parser rule that defines operand types
     */
    constructor(
        private readonly name: string,
        private readonly call: ParserRule<T>
    ) {}

    /**
     * Specifies the return type for this infix rule. The return type must be
     * a binary expression type with left, right, and operator fields.
     *
     * @template R The binary expression AST node type
     * @param type The interface or type definition for the binary expression
     * @returns A builder for configuring operators and precedence
     *
     * @example
     * ```typescript
     * .returns(BinaryExpression)  // Where BinaryExpression has left, right, operator
     * ```
     */
    returns<R extends AstNode & { left: AstNode; right: AstNode; operator: string }>(
        type: BaseType<R>
    ): InfixRuleBuilderWithCallAndReturn<T, never, R> {
        return new InfixRuleBuilderWithCallAndReturn(this.name, this.call, type);
    }
}

/**
 * Full builder for infix rules with base rule and return type configured.
 * This builder allows adding operators with different precedence levels.
 *
 * @template T The AST node type produced by the base rule
 * @template P Union type of already configured operator strings
 * @template R The binary expression AST node type
 */
export class InfixRuleBuilderWithCallAndReturn<
    T extends AstNode,
    P extends string,
    R extends AstNode & { operator: string }
> {
    /**
     * List of operator precedence groups. Each group contains operators
     * with the same precedence level and associativity.
     */
    private readonly _operators: SerializableGrammarNode<GrammarAST.InfixRuleOperatorList>[] = [];

    /**
     * Creates a fully configured infix rule builder.
     *
     * @param name The unique name for this infix rule
     * @param call The base parser rule for operands
     * @param returnType The binary expression type definition
     */
    constructor(
        private readonly name: string,
        private readonly call: ParserRule<T>,
        private readonly returnType: BaseType<R>
    ) {}

    /**
     * Adds a group of operators with the same precedence level.
     * Multiple calls to this method create different precedence levels,
     * with later calls having lower precedence.
     *
     * @template P2 The new operator strings being added
     * @param associativity The associativity for these operators
     * @param operators The operator strings to add
     * @returns Updated builder with the new operators
     *
     * @example
     * ```typescript
     * .operators(Associativity.LEFT, "*", "/")     // Higher precedence
     * .operators(Associativity.LEFT, "+", "-")     // Lower precedence
     * ```
     */
    operators<P2 extends Exclude<R["operator"], P>>(
        associativity: Associativity,
        ...operators: P2[]
    ): InfixRuleBuilderWithCallAndReturn<T, P | P2, R>;

    /**
     * Adds a group of operators with default (left) associativity.
     *
     * @template P2 The new operator strings being added
     * @param operators The operator strings to add
     * @returns Updated builder with the new operators
     *
     * @example
     * ```typescript
     * .operators("*", "/")     // Higher precedence
     * .operators("+", "-")     // Lower precedence
     * ```
     */
    operators<P2 extends Exclude<R["operator"], P>>(
        ...operators: P2[]
    ): InfixRuleBuilderWithCallAndReturn<T, P | P2, R>;

    operators<P2 extends Exclude<R["operator"], P>>(
        ...operatorsOrAssociativity: [Associativity, ...P2[]] | P2[]
    ): InfixRuleBuilderWithCallAndReturn<T, P | P2, R> {
        let associativity: Associativity | undefined;
        let operators: string[];
        if (typeof operatorsOrAssociativity[0] === "number") {
            associativity = operatorsOrAssociativity[0] as Associativity;
            operators = operatorsOrAssociativity.slice(1) as string[];
        } else {
            operators = operatorsOrAssociativity as string[];
        }
        this._operators.push({
            $type: "InfixRuleOperatorList",
            associativity:
                associativity != undefined ? (associativity === Associativity.LEFT ? "left" : "right") : undefined,
            operators: operators.map((op) => ({ $type: "Keyword", value: op }))
        });
        return this as InfixRuleBuilderWithCallAndReturn<T, P | P2, R>;
    }

    /**
     * Builds the final infix rule with all configured operators and precedence.
     *
     * @returns A parser rule that can handle infix expressions with the configured operators
     *
     * @example
     * ```typescript
     * const rule = createInfixRule("ExpressionRule")
     *     .on(NumberRule)
     *     .returns(BinaryExpression)
     *     .operators("*", "/")
     *     .operators("+", "-")
     *     .build();
     * ```
     */
    build(): ParserRule<R> {
        const createdRule: SerializableGrammarNode<GrammarAST.InfixRule> = {
            $type: "InfixRule",
            name: this.name,
            returnType: () => this.returnType.toType(),
            call: {
                $type: "RuleCall",
                rule: () => this.call.toRule(),
                arguments: []
            },
            operators: {
                $type: "InfixRuleOperators",
                precedences: this._operators
            },
            parameters: []
        };
        return {
            toRule: () => {
                return createdRule;
            }
        };
    }
}
