import {
    AbstractAstReflection,
    loadGrammarFromJson,
    type AstReflection,
    type Grammar,
    type PropertyMetaData,
    type TypeMetaData
} from "langium";
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
import type { LanguagePlugin } from "../../plugin/languagePlugin.js";

/**
 * Provides both the grammar and AstReflection for several langium language, which would
 * typically be handled via code generation.
 */
export interface LanguageModule {
    /**
     * The compiled Langium grammar that defines the language syntax and parsing rules.
     * This grammar can be used to create parsers and other language services.
     */
    grammars: Map<LanguagePlugin<any>, Grammar>;

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
 * @param plugins Array of language plugins to include in the module
 * @returns A complete language module ready for use with Langium services
 */
export function createModule(plugins: LanguagePlugin<any>[]): LanguageModule {
    const serializedGrammars = new Map<LanguagePlugin<any>, string>();
    for (const plugin of plugins) {
        const serializableGrammar = new GrammarSerializer(plugin.rootRule, plugin.additionalTerminals).grammar;
        const serializedGrammar = JSON.stringify(serializableGrammar);
        serializedGrammars.set(plugin, serializedGrammar);
    }
    const astTypes = collectAst(
        [...serializedGrammars.values()].map((serializedGrammar) => loadGrammarFromJson(serializedGrammar))
    );
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
        grammars: new Map(
            [...serializedGrammars.entries()].map(([plugin, serializedGrammar]) => [
                plugin,
                loadGrammarFromJson(serializedGrammar)
            ])
        ),
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
