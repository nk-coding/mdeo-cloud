import type { GrammarAST } from "langium";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import type { TerminalRule } from "../terminal/types.js";
import { isTerminalRule } from "../terminal/types.js";
import type { AbstractElement, RuleEntry, CrossRef, RuleContext } from "./types.js";
import type { ParserRule } from "../types.js";

/**
 * Type guard to check if a value is a parser rule.
 *
 * @param node The value to check
 * @returns True if the node is a parser rule, false otherwise
 */
export function isParserRule(node: unknown): node is ParserRule<any> {
    return typeof node === "object" && node !== null && "toRule" in node && typeof node.toRule === "function";
}

/**
 * Groups multiple rule entries into a single abstract element.
 * If only one element is provided, returns it directly.
 * Otherwise, wraps multiple elements in a Group.
 *
 * @param elements The rule entries to group
 * @returns A single abstract element representing the group
 */
export function groupIfNeeded(elements: RuleEntry[]): AbstractElement {
    const replaced = simplifyEntries(elements);
    if (replaced.length === 1) {
        return replaced[0]!;
    }
    return {
        $type: "Group",
        elements: replaced
    };
}

/**
 * Creates a rule call element that references either a terminal rule or parser rule.
 * Rule calls are used to invoke other rules within a grammar definition.
 *
 * @param element The terminal or parser rule to call
 * @returns An abstract element representing the rule call
 */
export function createRuleCall(
    element: TerminalRule<any> | ParserRule<any> | (() => ParserRule<any>)
): AbstractElement {
    return {
        $type: "RuleCall",
        rule: () => {
            if (isTerminalRule(element)) {
                return element;
            } else if (typeof element === "function") {
                return element().toRule();
            } else {
                return element.toRule();
            }
        },
        arguments: []
    };
}

/**
 * Converts a single rule entry into an abstract element.
 * Handles different types of entries (strings, terminals, parsers, cross-refs).
 *
 * @param entry The rule entry to convert
 * @returns The corresponding abstract element
 */
export function simplifyEntry(entry: RuleEntry): AbstractElement {
    if (typeof entry === "string") {
        return {
            $type: "Keyword",
            value: entry
        };
    } else if (isTerminalRule(entry) || isParserRule(entry) || typeof entry === "function") {
        return createRuleCall(entry);
    }
    return entry;
}

/**
 * Converts an array of rule entries into an array of abstract elements.
 *
 * @param elements The rule entries to convert
 * @returns Array of corresponding abstract elements
 */
export function simplifyEntries(elements: RuleEntry[]): AbstractElement[] {
    return elements.map(simplifyEntry);
}

/**
 * Creates an assignment element that assigns values to AST node properties.
 * Assignments define how parsed content is stored in the resulting AST.
 *
 * @param key The property name to assign to
 * @param operator The assignment operator ("=", "+=", or "?=")
 * @param terminal The terminals or literal values to assign
 * @returns A serializable assignment element
 */
export function createAssignment(
    key: string,
    operator: GrammarAST.Assignment["operator"],
    terminal: [TerminalRule<any> | ParserRule<any> | (() => ParserRule<any>) | CrossRef<any>] | string[]
): SerializableGrammarNode<GrammarAST.Assignment> {
    let terminalEntry: AbstractElement;
    if (terminal.length !== 1) {
        terminalEntry = {
            $type: "Alternatives",
            elements: terminal.map((t) => ({
                $type: "Keyword",
                value: t
            }))
        };
    } else {
        const [first] = terminal;
        terminalEntry = simplifyEntry(first);
    }

    return {
        $type: "Assignment",
        operator,
        feature: key,
        terminal: terminalEntry
    };
}

/**
 * Default rule context implementation that provides standard assignment methods.
 * As the types are only for compile-time checking, this implementation is used for all types.
 */
export const defaultRuleContext: RuleContext<any> = {
    set: (key, ...value) => createAssignment(key, "=", value),
    add: (key, ...value) => createAssignment(key, "+=", value),
    flag: (key, ...value) => ({
        $type: "Assignment",
        operator: "?=",
        feature: key,
        terminal: groupIfNeeded(value)
    })
};
