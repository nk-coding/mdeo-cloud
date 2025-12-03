import type { AstNode, GrammarAST, Reference } from "langium";
import type { SerializableGrammarNode } from "./types.js";
import type { BaseType } from "../type/types.js";
import type { Type } from "../type/type/types.js";
import type { Interface } from "../type/interface/types.js";
import type { ParserRule } from "../rule/types.js";
import type { TerminalRule } from "../rule/terminal/types.js";

/**
 * Utility type that represents either a single value or an array of values.
 * Used throughout serialization to handle properties that can be either singular or plural.
 * 
 * @template T The base type that can be singular or plural
 */
type ArrayOrT<T> = T | T[];

/**
 * Union type representing all possible rule types that can be serialized.
 * Rules are the fundamental building blocks of a grammar and include terminal rules,
 * parser rules, and infix rules for handling operators.
 */
type Rule =
    | SerializableGrammarNode<GrammarAST.TerminalRule>
    | SerializableGrammarNode<GrammarAST.ParserRule>
    | SerializableGrammarNode<GrammarAST.InfixRule>;

/**
 * Represents a serializable reference to another node in the grammar.
 * References use JSON Pointer-like syntax to identify the target node's location
 * in the serialized grammar structure.
 */
export interface SerializableReference {
    /**
     * JSON Pointer-style reference to another node (e.g., "#/rules@0", "#/types@1").
     * The format is "#/collection@index" where collection is rules/types/interfaces
     * and index is the position in that collection.
     */
    $ref: string;
}

/**
 * Type that transforms any AST node into a serializable form by converting
 * Langium references to serializable references and handling circular dependencies.
 * 
 * This transformation is necessary because AST nodes can contain circular references
 * and Langium-specific Reference types that cannot be directly serialized to JSON.
 * 
 * @template T The AST node type to make serializable
 */
export type SerializedAstNode<T extends AstNode> = {
    [K in keyof T]: T[K] extends AstNode
        ? SerializedAstNode<T[K]>
        : T[K] extends AstNode[]
        ? SerializedAstNode<T[K][number]>[]
        : T[K] extends Reference<any>
        ? SerializableReference
        : T[K] extends Reference<any>[]
        ? SerializableReference[]
        : T[K];
};

/**
 * Union type representing serialized versions of all possible rule types.
 * These are the JSON-serializable equivalents of the Rule type.
 */
type SerializedRule =
    | SerializedAstNode<GrammarAST.TerminalRule>
    | SerializedAstNode<GrammarAST.ParserRule>
    | SerializedAstNode<GrammarAST.InfixRule>;

/**
 * Internal data structure that pairs a grammar node with its serializable reference.
 * Used to track already-serialized nodes and avoid duplicating them in the output.
 * 
 * @template T The type of the grammar node being tracked
 */
interface NodeWithRef<T> {
    /**
     * The actual grammar node instance.
     */
    node: T;
    
    /**
     * The serializable reference that points to this node in the output.
     */
    ref: SerializableReference;
}

/**
 * Recursive type representing all possible values that can appear during
 * the serialization process. This includes primitive values, objects,
 * arrays, and function references to other grammar nodes.
 */
type Entry = ArrayOrT<
    SerializableGrammarNode<any> | number | string | boolean | undefined | (() => SerializableGrammarNode<any>)
>;

/**
 * Serializes grammar rules and types into a JSON-serializable format that can be
 * processed by Langium's grammar loading mechanisms.
 */
export class GrammarSerializer {
    /**
     * Lookup table mapping rule names to their registered nodes and references.
     * Prevents duplicate rules and enables cross-referencing.
     */
    private readonly ruleLookup = new Map<string, NodeWithRef<Rule>>();
    
    /**
     * Lookup table mapping type names to their registered nodes and references.
     * Handles both Type and Interface definitions.
     */
    private readonly typeLookup = new Map<string, NodeWithRef<BaseType<any>>>();

    /**
     * Array of serialized rules in registration order.
     * Undefined slots are placeholders during the registration process.
     */
    private readonly rules: (SerializedRule | undefined)[] = [];
    
    /**
     * Array of serialized types in registration order.
     * Undefined slots are placeholders during the registration process.
     */
    private readonly types: (SerializedAstNode<GrammarAST.Type> | undefined)[] = [];
    
    /**
     * Array of serialized interfaces in registration order.
     * Undefined slots are placeholders during the registration process.
     */
    private readonly interfaces: (SerializedAstNode<GrammarAST.Interface> | undefined)[] = [];

    /**
     * Gets the complete serialized grammar containing all registered rules, types, and interfaces.
     * This property returns the final JSON-serializable grammar structure that can be
     * processed by Langium's loadGrammarFromJson function.
     * 
     * @returns The complete serialized grammar ready for JSON conversion
     */
    get grammar(): SerializedAstNode<GrammarAST.Grammar> {
        return {
            $type: "Grammar",
            isDeclared: true,
            rules: this.rules as SerializedRule[],
            types: this.types as SerializedAstNode<GrammarAST.Type>[],
            interfaces: this.interfaces as SerializedAstNode<GrammarAST.Interface>[],
            imports: []
        };
    }

    /**
     * Creates a new grammar serializer and registers the entry rule and additional terminals.
     * 
     * @param entry The main parser rule that serves as the grammar entry point
     * @param additionalTerminals Array of terminal rules to include in the grammar
     */
    constructor(entry: ParserRule<any>, additionalTerminals: TerminalRule<any>[]) {
        this.registerRule(entry.toRule());
        (this.rules[0] as SerializedAstNode<GrammarAST.ParserRule>).entry = true;
        for (const terminal of additionalTerminals) {
            this.registerRule(terminal);
        }
    }

    /**
     * Recursively visits and serializes grammar entries, handling different value types
     * and converting function references to serializable references.
     * 
     * This method is the core of the serialization process. It handles:
     * - Arrays by recursively visiting each element
     * - Objects by visiting all property values
     * - Functions by evaluating them and registering the returned nodes
     * - Primitive values by returning them unchanged
     * 
     * @param entry The entry to visit and serialize
     * @returns The serialized representation of the entry
     * @throws Error if an unsupported reference type is encountered
     */
    private visit(entry: Entry): ArrayOrT<SerializedAstNode<any> | string | number | boolean | undefined> {
        if (Array.isArray(entry)) {
            return entry.map((n) => this.visit(n)) as Entry;
        }
        if (typeof entry === "object") {
            return Object.fromEntries(
                Object.entries(entry).map(([key, value]) => [key, this.visit(value as SerializableGrammarNode<any>)])
            ) as SerializedAstNode<any>;
        }
        if (typeof entry === "function") {
            const reference = entry();
            if (this.isRule(reference)) {
                return this.registerRule(reference);
            } else if (this.isType(reference)) {
                return this.registerType(reference);
            } else if (this.isInterface(reference)) {
                return this.registerInterface(reference);
            } else {
                throw new Error(`Unsupported reference type: ${reference}`);
            }
        }
        return entry;
    }

    /**
     * Registers a rule in the serialization lookup and converts it to serialized form.
     * 
     * This method handles rule deduplication by checking if a rule with the same name
     * has already been registered. If found, it verifies that it's the same rule instance
     * and returns the existing reference. Otherwise, it creates a new entry.
     * 
     * @param rule The rule to register and serialize
     * @returns A serializable reference to the registered rule
     * @throws Error if a rule with the same name but different instance is found
     */
    private registerRule(rule: Rule): SerializableReference {
        let existing = this.ruleLookup.get(rule.name);
        if (existing) {
            if (existing.node !== rule) {
                throw new Error(`Duplicate rule name: ${rule.name}`);
            }
            return existing.ref;
        }
        const id = this.rules.length;
        this.rules.push(undefined);
        const ref: SerializableReference = { $ref: `#/rules@${id}` };
        this.ruleLookup.set(rule.name, { node: rule, ref });
        const serialized = this.visit(rule) as SerializedRule;
        this.rules[id] = serialized;
        return ref;
    }

    /**
     * Registers a type definition in the serialization lookup and converts it to serialized form.
     * 
     * Similar to registerRule, this method handles type deduplication and ensures that
     * each type is only serialized once. It works with union type definitions.
     * 
     * @param type The type definition to register and serialize
     * @returns A serializable reference to the registered type
     * @throws Error if a type with the same name but different instance is found
     */
    private registerType(type: Type<any>): SerializableReference {
        let existing = this.typeLookup.get(type.name);
        if (existing) {
            if (existing.node !== type) {
                throw new Error(`Duplicate type name: ${type.name}`);
            }
            return existing.ref;
        }
        const id = this.types.length;
        this.types.push(undefined);
        const ref: SerializableReference = { $ref: `#/types@${id}` };
        this.typeLookup.set(type.name, { node: type, ref });
        const serialized = this.visit(type) as SerializedAstNode<GrammarAST.Type>;
        this.types[id] = serialized;
        return ref;
    }

    /**
     * Registers an interface definition in the serialization lookup and converts it to serialized form.
     * 
     * This method handles interface deduplication and ensures that each interface
     * is only serialized once. It works with structured interface definitions.
     * 
     * @param iface The interface definition to register and serialize
     * @returns A serializable reference to the registered interface
     * @throws Error if an interface with the same name but different instance is found
     */
    private registerInterface(iface: Interface<any>): SerializableReference {
        let existing = this.typeLookup.get(iface.name);
        if (existing) {
            if (existing.node !== iface) {
                throw new Error(`Duplicate interface name: ${iface.name}`);
            }
            return existing.ref;
        }
        const id = this.interfaces.length;
        this.interfaces.push(undefined);
        const ref: SerializableReference = { $ref: `#/interfaces@${id}` };
        this.typeLookup.set(iface.name, { node: iface, ref });
        const serialized = this.visit(iface) as SerializedAstNode<GrammarAST.Interface>;
        this.interfaces[id] = serialized;
        return ref;
    }

    /**
     * Type guard to check if a value is a grammar rule (terminal, parser, or infix).
     * 
     * @param value The value to check
     * @returns True if the value is a rule, false otherwise
     */
    private isRule(value: any): value is Rule {
        return (
            value &&
            typeof value === "object" &&
            "$type" in value &&
            (value.$type === "TerminalRule" || value.$type === "ParserRule" || value.$type === "InfixRule")
        );
    }

    /**
     * Type guard to check if a value is a Type definition.
     * 
     * @param value The value to check
     * @returns True if the value is a Type, false otherwise
     */
    private isType(value: any): value is Type<any> {
        return value && typeof value === "object" && "$type" in value && value.$type === "Type";
    }

    /**
     * Type guard to check if a value is an Interface definition.
     * 
     * @param value The value to check
     * @returns True if the value is an Interface, false otherwise
     */
    private isInterface(value: any): value is Interface<any> {
        return value && typeof value === "object" && "$type" in value && value.$type === "Interface";
    }
}