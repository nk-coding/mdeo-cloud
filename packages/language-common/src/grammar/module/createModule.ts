import {
    AbstractAstReflection,
    loadGrammarFromJson,
    type AstReflection,
    type Grammar,
    type PropertyMetaData,
    type TypeMetaData
} from "langium";
import type { TerminalRule } from "../rule/terminal/types.js";
import type { ParserRule } from "../rule/types.js";
import { GrammarSerializer } from "../serialization/grammarSerializer.js";
import {
    collectAst,
    collectTypeHierarchy,
    findReferenceTypes,
    isAstType,
    mergeTypesAndInterfaces,
    type AstTypes,
    type Property
} from "langium/grammar";

/**
 * Provides both the grammar and AstReflection for a Langium language, which would
 * typically be handled via code generation.
 */
export interface LanguageModule {
    /**
     * The compiled Langium grammar that defines the language syntax and parsing rules.
     * This grammar can be used to create parsers and other language services.
     */
    grammar: Grammar;

    /**
     * AST reflection metadata that provides runtime type information about
     * the AST node types defined in the grammar. This enables features like
     * type checking, validation, and code completion.
     */
    reflection: AstReflection;
}

/**
 * Creates a complete language module from a parser rule and additional terminal rules.
 *
 * @param entry The root parser rule that serves as the entry point for parsing
 * @param additionalTerminals Array of terminal rules that should be included in the grammar
 * @returns A complete language module ready for use with Langium services
 */
export function createModule(entry: ParserRule<any>, additionalTerminals: TerminalRule<any>[]): LanguageModule {
    const serializableGrammar = new GrammarSerializer(entry, additionalTerminals).grammar;
    const serializedGrammar = JSON.stringify(serializableGrammar);
    const astTypes = collectAst(loadGrammarFromJson(serializedGrammar));
    const astTypesFiltered: AstTypes = {
        interfaces: [...astTypes.interfaces],
        unions: astTypes.unions.filter((e) => isAstType(e.type))
    };
    const typeHierarchy = collectTypeHierarchy(mergeTypesAndInterfaces(astTypesFiltered));

    class AstReflection extends AbstractAstReflection {
        override readonly types = Object.fromEntries([
            ...astTypesFiltered.interfaces.map((iface) => [
                iface.name,
                buildTypeMetaData(iface.name, iface.properties, typeHierarchy.superTypes.get(iface.name))
            ]),
            ...astTypesFiltered.unions.map((union) => [
                union.name,
                buildTypeMetaData(union.name, [], typeHierarchy.superTypes.get(union.name))
            ])
        ]);
    }

    return {
        grammar: loadGrammarFromJson(serializedGrammar),
        reflection: new AstReflection()
    };
}

/**
 * Builds type metadata for a specific AST node type.
 *
 * @param name The name of the AST node type
 * @param props Array of properties defined on this type
 * @param superTypes Array of parent type names this type extends from
 * @returns Complete type metadata for the AST node type
 */
function buildTypeMetaData(name: string, props: Property[], superTypes: readonly string[] | undefined): TypeMetaData {
    return {
        name,
        properties: Object.fromEntries(props.map((property) => [property.name, buildPropertyMetaData(property)])),
        superTypes: [...(superTypes ?? [])]
    };
}

/**
 * Builds property metadata for a specific property of an AST node.
 *
 * @param property The property definition to build metadata for
 * @returns Complete property metadata including reference type information
 */
function buildPropertyMetaData(property: Property): PropertyMetaData {
    const refTypes = findReferenceTypes(property.type);
    const refType = refTypes.length > 0 ? refTypes[0] : undefined;
    return {
        name: property.name,
        defaultValue: property.defaultValue,
        referenceType: refType
    };
}
