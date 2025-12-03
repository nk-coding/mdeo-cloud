import type { AstNode } from "langium";
import type { BaseType } from "../../type/types.js";
import type { CrossRef, AbstractElement, RuleEntry, RuleContext } from "./types.js";
import type { TerminalRule } from "../terminal/types.js";
import { RuleBuilder } from "./builders.js";
import { createRuleCall, defaultRuleContext, groupIfNeeded, simplifyEntries } from "./helpers.js";

/**
 * Creates a new parser rule builder for defining grammar rules.
 *
 * Parser rules define how to parse specific AST node types from input text.
 * They specify the syntax pattern and how to construct the resulting AST node.
 *
 * @param name The unique name for this parser rule in the grammar
 * @returns A new RuleBuilder instance for configuring the parser rule
 *
 * @example
 * ```typescript
 * // Create a rule for parsing person declarations
 * const PersonRule = createRule("Person")
 *     .returns(PersonInterface)
 *     .as(({ set, add, flag }) => [
 *         set("name", STRING_TERMINAL),
 *         set("age", NUMBER_TERMINAL),
 *         flag("isActive", "active")
 *     ]);
 * ```
 */
export function createRule(name: string): RuleBuilder {
    return new RuleBuilder(name);
}

/**
 * Creates a cross-reference to another AST node type. Cross-references allow
 * one part of the AST to reference another part by name or identifier.
 *
 * @template T The AST node type being referenced
 * @param type The type definition of the node being referenced
 * @param terminal Optional terminal rule for the reference syntax (defaults to ID)
 * @returns A cross-reference that can be used in rule definitions
 *
 * @example
 * ```typescript
 * // Reference a variable by its identifier
 * set("variable", ref(VariableDeclaration))
 *
 * // Reference with custom terminal syntax
 * set("target", ref(FunctionDeclaration, QUALIFIED_NAME))
 * ```
 */
export function ref<T extends AstNode>(type: BaseType<T>, terminal?: TerminalRule<any>): CrossRef<T> {
    return {
        $type: "CrossReference",
        type: () => type,
        terminal: terminal != undefined ? createRuleCall(terminal) : undefined,
        isMulti: false,
        deprecatedSyntax: false
    };
}

/**
 * Creates a repetition element that matches zero or more occurrences of the given elements.
 * Equivalent to the * quantifier in regular expressions.
 *
 * @param elements The rule entries to repeat
 * @returns An abstract element representing the repetition
 *
 * @example
 * ```typescript
 * // Match zero or more statements
 * many(StatementRule)
 *
 * // Match zero or more comma-separated items
 * many(ItemRule, ",")
 * ```
 */
export function many(...elements: RuleEntry[]): AbstractElement {
    const grouped = groupIfNeeded(elements);
    grouped.cardinality = "*";
    return grouped;
}

/**
 * Creates a repetition element that matches one or more occurrences of the given elements.
 * Equivalent to the + quantifier in regular expressions.
 *
 * @param elements The rule entries to repeat
 * @returns An abstract element representing the repetition
 *
 * @example
 * ```typescript
 * // Match one or more digits
 * atLeastOne(DIGIT_TERMINAL)
 *
 * // Match one or more statements
 * atLeastOne(StatementRule)
 * ```
 */
export function atLeastOne(...elements: RuleEntry[]): AbstractElement {
    const grouped = groupIfNeeded(elements);
    const cardinality = grouped.cardinality;
    if (cardinality === "?") {
        grouped.cardinality = "*";
    } else if (cardinality == undefined) {
        grouped.cardinality = "+";
    }
    return grouped;
}

/**
 * Creates an optional element that matches zero or one occurrence of the given elements.
 * Equivalent to the ? quantifier in regular expressions.
 *
 * @param elements The rule entries to make optional
 * @returns An abstract element representing the optional match
 *
 * @example
 * ```typescript
 * // Optional semicolon
 * optional(";")
 *
 * // Optional type annotation
 * optional(":", TypeRule)
 * ```
 */
export function optional(...elements: RuleEntry[]): AbstractElement {
    const grouped = groupIfNeeded(elements);
    const cardinality = grouped.cardinality;
    if (cardinality === "*" || cardinality === "+") {
        grouped.cardinality = "*";
    } else if (cardinality == undefined) {
        grouped.cardinality = "?";
    }
    return grouped;
}

/**
 * Creates an alternatives element that matches any one of the given elements.
 * Similar to the | operator in regular expressions or BNF grammars.
 *
 * @param elements The rule entries to choose from
 * @returns An abstract element representing the alternatives
 *
 * @example
 * ```typescript
 * // Match either a number or string literal
 * or(NUMBER_TERMINAL, STRING_TERMINAL)
 *
 * // Match different statement types
 * or(IfStatement, WhileStatement, ForStatement)
 * ```
 */
export function or(...elements: RuleEntry[]): AbstractElement {
    return {
        $type: "Alternatives",
        elements: simplifyEntries(elements)
    };
}

/**
 * Creates a group element that sequences the given elements.
 * All elements in the group must be matched in order.
 *
 * @param elements The rule entries to group in sequence
 * @returns An abstract element representing the sequence
 *
 * @example
 * ```typescript
 * // Group function declaration parts
 * group("function", ID_TERMINAL, "(", ParameterList, ")")
 *
 * // Group with optional elements
 * group("if", "(", Expression, ")", Statement)
 * ```
 */
export function group(...elements: RuleEntry[]): AbstractElement {
    return {
        $type: "Group",
        elements: simplifyEntries(elements)
    };
}

/**
 * Creates an unordered group where all elements must be present but can appear
 * in any order. Useful for parsing configuration blocks or attribute lists.
 *
 * @param elements The rule entries that can appear in any order
 * @returns An abstract element representing the unordered group
 *
 * @example
 * ```typescript
 * // Parse class modifiers in any order
 * unorderedGroup(
 *     optional("public"),
 *     optional("static"),
 *     optional("final")
 * )
 * ```
 */
export function unorderedGroup(...elements: RuleEntry[]): AbstractElement {
    return {
        $type: "UnorderedGroup",
        elements: simplifyEntries(elements)
    };
}

/**
 * Creates an action to return a different AST node during parsing.
 *
 * @template T The AST node type to create
 * @param type The interface or type definition for the AST node to instantiate
 * @param handler Function that defines what should be parsed after creating the node
 * @returns An abstract element representing the action
 *
 * @see {@link https://langium.org/docs/grammar-language/#actions | Langium Actions Documentation}
 *
 * @example
 * ```typescript
 * // Create a binary expression for infix operators
 * action(BinaryExpressionInterface, ({ set }) => [
 *     set("operator", "+", "-"),
 *     set("right", ExpressionRule)
 * ])
 * ```
 */
export function action<T extends AstNode>(
    type: BaseType<T>,
    handler: (context: RuleContext<T>) => RuleEntry[]
): AbstractElement {
    return group(
        {
            $type: "Action",
            type: () => type
        },
        ...handler(defaultRuleContext)
    );
}

/**
 * Creates a tree rewrite action that modifies the current AST node during parsing.
 * Unlike regular actions, tree rewrite actions modify existing nodes on the parser stack.
 *
 * @template T The AST node type to modify
 * @param type The interface or type definition that the current node must match
 * @param feature The property name on the node to modify
 * @param operator The assignment operator: "=" (set/replace) or "+=" (add to array)
 * @param handler Function that defines how to parse the value for the property
 * @returns An abstract element representing the tree rewrite action
 *
 * @see {@link https://langium.org/docs/grammar-language/#tree-rewriting-actions | Langium Tree Rewriting Documentation}
 *
 * @example
 * ```typescript
 * // Transform expression into function call when encountering parentheses
 * treeRewriteAction(FunctionCallInterface, "callee", "=", ({ add }) => [
 *     "(",
 *     optional(add("arguments", ExpressionRule)),
 *     ")"
 * ])
 * ```
 */
export function treeRewriteAction<T extends AstNode>(
    type: BaseType<T>,
    feature: keyof Omit<T, keyof AstNode> & string,
    operator: "=" | "+=",
    handler: (context: RuleContext<T>) => RuleEntry[]
): AbstractElement {
    return group(
        {
            $type: "Action",
            type: () => type,
            feature,
            operator
        },
        ...handler(defaultRuleContext)
    );
}
