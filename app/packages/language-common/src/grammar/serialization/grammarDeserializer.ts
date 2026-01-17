import type { GrammarAST } from "langium";
import type {
    SerializedAstNode,
    SerializableReference,
    SerializableExternalReference,
    SerializedGrammar
} from "./grammarSerializer.js";
import type { SerializableGrammarNode } from "./types.js";
import type { ParserRule } from "../rule/types.js";
import type { TerminalRule } from "../rule/terminal/types.js";
import type { Interface } from "../type/interface/types.js";
import type { Type } from "../type/type/types.js";
import { createExternalParserRule } from "../rule/parser/factory.js";
import { createExternalTerminalRule } from "../rule/terminal/factory.js";
import { createExternalInterface } from "../type/interface/factory.js";

/**
 * Context for resolving external references during grammar deserialization.
 * This context provides access to types, parser rules, and terminal rules
 * that may be referenced from other grammars.
 */
export interface GrammarDeserializationContext {
    /**
     * Lookup map for resolving external type/interface references by name.
     */
    types?: Map<string, Interface<any> | Type<any>>;

    /**
     * Lookup map for resolving external parser rule references by name.
     */
    parserRules?: Map<string, ParserRule<any>>;

    /**
     * Lookup map for resolving external terminal rule references by name.
     */
    terminalRules?: Map<string, TerminalRule<any>>;
}

/**
 * Deserializes a serialized grammar back into ParserRule, TerminalRule, Interface,
 * and Type instances that can be used to build a composed language.
 *
 * The deserializer uses lazy function-based references to avoid two-phase processing.
 * References are resolved on-demand when accessed through function calls.
 */
export class GrammarDeserializer {
    /**
     * The serialized grammar to deserialize.
     */
    private readonly serializedGrammar: SerializedGrammar;

    /**
     * Context for resolving external references.
     */
    private readonly context: GrammarDeserializationContext;

    /**
     * Lazy lookup for internal rule references, indexed by position.
     */
    private readonly ruleLookup = new Map<number, () => ParserRule<any> | TerminalRule<any>>();

    /**
     * Lazy lookup for internal type references, indexed by position.
     */
    private readonly typeLookup = new Map<number, () => Type<any>>();

    /**
     * Lazy lookup for internal interface references, indexed by position.
     */
    private readonly interfaceLookup = new Map<number, () => Interface<any>>();

    /**
     * Creates a new grammar deserializer bound to a specific serialized grammar.
     *
     * @param serializedGrammar The serialized grammar to deserialize
     * @param context Context containing registered types, parser rules, and terminal rules for external references
     */
    constructor(serializedGrammar: SerializedGrammar, context: GrammarDeserializationContext = {}) {
        this.serializedGrammar = serializedGrammar;
        this.context = {
            types: context.types ?? new Map(),
            parserRules: context.parserRules ?? new Map(),
            terminalRules: context.terminalRules ?? new Map()
        };
        this.initializeLookups();
    }

    /**
     * Deserializes the bound grammar into rules, types, and interfaces.
     *
     * @returns An object containing the deserialized rules, types, and interfaces
     */
    deserializeGrammar(): {
        rules: (ParserRule<any> | TerminalRule<any>)[];
        types: Type<any>[];
        interfaces: Interface<any>[];
    } {
        const rules = this.serializedGrammar.rules.map((_, index) => this.ruleLookup.get(index)!());
        const types = this.serializedGrammar.types.map((_, index) => this.typeLookup.get(index)!());
        const interfaces = this.serializedGrammar.interfaces.map((_, index) => this.interfaceLookup.get(index)!());

        return { rules, types, interfaces };
    }

    /**
     * Initializes lazy lookup tables for all rules, types, and interfaces.
     * Each lookup returns a function that, when called, deserializes and returns the actual object.
     */
    private initializeLookups(): void {
        this.serializedGrammar.rules.forEach((serializedRule, index) => {
            this.ruleLookup.set(index, () => this.deserializeRule(serializedRule));
        });

        this.serializedGrammar.types.forEach((serializedType, index) => {
            this.typeLookup.set(index, () => this.deserializeType(serializedType));
        });

        this.serializedGrammar.interfaces.forEach((serializedInterface, index) => {
            this.interfaceLookup.set(index, () => this.deserializeInterface(serializedInterface));
        });
    }

    /**
     * Deserializes a single rule from its serialized form.
     *
     * @param serialized The serialized rule node or external reference to deserialize.
     * @returns The runtime `ParserRule` or `TerminalRule` corresponding to the serialized node.
     */
    private deserializeRule(
        serialized:
            | SerializedAstNode<GrammarAST.ParserRule>
            | SerializedAstNode<GrammarAST.TerminalRule>
            | SerializedAstNode<GrammarAST.InfixRule>
            | SerializableExternalReference
    ): ParserRule<any> | TerminalRule<any> {
        if (this.isExternalReference(serialized)) {
            return this.resolveExternalRule(serialized);
        }

        if (serialized.$type === "TerminalRule") {
            return this.deserializeTerminalRule(serialized);
        } else {
            return this.deserializeParserRule(serialized);
        }
    }

    /**
     * Deserializes a terminal rule.
     *
     * @param serialized The serialized terminal rule node to deserialize.
     * @returns A `TerminalRule` implementation with `name` and `toRule`.
     */
    private deserializeTerminalRule(serialized: SerializedAstNode<GrammarAST.TerminalRule>): TerminalRule<any> {
        const resolved = this.resolveReferences(serialized) as SerializableGrammarNode<GrammarAST.TerminalRule>;
        return {
            name: serialized.name,
            toRule: () => resolved
        };
    }

    /**
     * Deserializes a parser or infix rule.
     *
     * @param serialized The serialized parser or infix rule node to deserialize.
     * @returns A `ParserRule` implementation exposing `toRule`.
     */
    private deserializeParserRule(
        serialized: SerializedAstNode<GrammarAST.ParserRule> | SerializedAstNode<GrammarAST.InfixRule>
    ): ParserRule<any> {
        const resolved = this.resolveReferences(serialized) as
            | SerializableGrammarNode<GrammarAST.ParserRule>
            | SerializableGrammarNode<GrammarAST.InfixRule>;
        return {
            toRule: () => resolved
        };
    }

    /**
     * Deserializes a single type from its serialized form.
     *
     * @param serialized The serialized type node or external reference.
     * @returns A `Type` implementation exposing `name` and `toType`.
     */
    private deserializeType(serialized: SerializedAstNode<GrammarAST.Type> | SerializableExternalReference): Type<any> {
        if (this.isExternalReference(serialized)) {
            return this.resolveExternalType(serialized);
        }

        const resolved = this.resolveReferences(serialized) as SerializableGrammarNode<GrammarAST.Type>;
        return {
            name: serialized.name,
            toType: () => resolved
        };
    }

    /**
     * Deserializes a single interface from its serialized form.
     *
     * @param serialized The serialized interface node or external reference.
     * @returns An `Interface` implementation exposing `name` and `toType`.
     */
    private deserializeInterface(
        serialized: SerializedAstNode<GrammarAST.Interface> | SerializableExternalReference
    ): Interface<any> {
        if (this.isExternalReference(serialized)) {
            return this.resolveExternalInterface(serialized);
        }

        const resolved = this.resolveReferences(serialized);
        return {
            name: serialized.name,
            toType: () => resolved
        };
    }

    /**
     * Recursively resolves all references in a serialized object, converting them to functions.
     *
     * @param value The serialized value to resolve. May be a primitive, array, object, or reference.
     * @returns The value with references replaced by functions that resolve to the referenced objects.
     */
    private resolveReferences<T>(value: T): any {
        if (Array.isArray(value)) {
            return value.map((item) => this.resolveReferences(item));
        }

        if (value == null) {
            return value;
        }

        if (this.isInternalReference(value)) {
            return this.resolveInternalReference(value);
        }

        if (this.isExternalReference(value)) {
            return this.createExternalReferenceFunction(value);
        }

        if (typeof value === "object") {
            const result: Record<string, any> = {};
            for (const [key, val] of Object.entries(value)) {
                result[key] = this.resolveReferences(val);
            }
            return result;
        }

        return value;
    }

    /**
     * Resolves an internal reference (e.g., "#/rules@0") to a function that returns the actual object.
     *
     * @param ref The serializable internal reference to resolve (e.g. "#/rules@0").
     * @returns A zero-argument function that returns the referenced runtime object.
     */
    private resolveInternalReference(ref: SerializableReference): () => any {
        const { collection, index } = this.parseReference(ref.$ref);

        switch (collection) {
            case "rules":
                return this.ruleLookup.get(index)!;
            case "types":
                return this.typeLookup.get(index)!;
            case "interfaces":
                return this.interfaceLookup.get(index)!;
            default:
                throw new Error(`Unknown collection: ${collection}`);
        }
    }

    /**
     * Creates a function that returns an external reference.
     *
     * @param ref The serializable external reference describing kind and name.
     * @returns A zero-argument function that resolves to the external runtime object.
     */
    private createExternalReferenceFunction(ref: SerializableExternalReference): () => any {
        return () => {
            switch (ref.kind) {
                case "ParserRule":
                    return this.context.parserRules?.get(ref.name) ?? createExternalParserRule(ref.name);
                case "TerminalRule":
                    return this.context.terminalRules?.get(ref.name) ?? createExternalTerminalRule(ref.name);
                case "Type":
                    return this.context.types?.get(ref.name) ?? createExternalInterface(ref.name);
                default:
                    throw new Error(`Unknown external reference kind: ${(ref as any).kind}`);
            }
        };
    }

    /**
     * Resolves an external rule reference directly.
     *
     * @param ref The external rule reference to resolve.
     * @returns The resolved `ParserRule` or `TerminalRule` from the provided context or a created external placeholder.
     */
    private resolveExternalRule(ref: SerializableExternalReference): ParserRule<any> | TerminalRule<any> {
        if (ref.kind === "ParserRule") {
            return this.context.parserRules?.get(ref.name) ?? createExternalParserRule(ref.name);
        } else if (ref.kind === "TerminalRule") {
            return this.context.terminalRules?.get(ref.name) ?? createExternalTerminalRule(ref.name);
        }
        throw new Error(`Invalid rule kind: ${ref.kind}`);
    }

    /**
     * Resolves an external type reference directly.
     *
     * @param ref The external type reference to resolve.
     * @returns The resolved `Type` or `Interface` from the provided context or a created external placeholder.
     */
    private resolveExternalType(ref: SerializableExternalReference): Type<any> {
        const fromContext = this.context.types?.get(ref.name);
        if (fromContext && "toType" in fromContext) {
            const result = fromContext.toType();
            if ("$type" in result && result.$type === "Type") {
                return fromContext as Type<any>;
            }
        }
        return createExternalInterface(ref.name) as Type<any>;
    }

    /**
     * Resolves an external interface reference directly.
     *
     * @param ref The external interface reference to resolve.
     * @returns The resolved `Interface` from the context or a created external placeholder.
     */
    private resolveExternalInterface(ref: SerializableExternalReference): Interface<any> {
        return (this.context.types?.get(ref.name) as Interface<any>) ?? createExternalInterface(ref.name);
    }

    /**
     * Parses a reference string into collection and index.
     *
     * @param refString The reference string to parse (e.g. "#/rules@0").
     * @returns An object with `collection` (rules|types|interfaces) and numeric `index`.
     */
    private parseReference(refString: string): { collection: string; index: number } {
        const match = refString.match(/#\/(rules|types|interfaces)@(\d+)/);
        if (!match) {
            throw new Error(`Invalid reference format: ${refString}`);
        }
        return {
            collection: match[1],
            index: parseInt(match[2], 10)
        };
    }

    /**
     * Type guard to check if a value is an internal reference.
     *
     * @param value The value to test.
     * @returns True when the value is a serializable internal reference.
     */
    private isInternalReference(value: any): value is SerializableReference {
        return typeof value === "object" && "$ref" in value;
    }

    /**
     * Type guard to check if a value is an external reference.
     *
     * @param value The value to test.
     * @returns True when the value is a serializable external reference.
     */
    private isExternalReference(value: any): value is SerializableExternalReference {
        return typeof value === "object" && "$externalRef" in value && value.$externalRef === true;
    }
}
