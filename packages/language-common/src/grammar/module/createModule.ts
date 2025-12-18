import type { AstReflection, Grammar, PropertyMetaData, TypeMetaData } from "langium";
import { GrammarSerializer } from "../serialization/grammarSerializer.js";
import type { AstTypes, Property } from "langium/grammar";
import type { LanguagePlugin } from "../../plugin/languagePlugin.js";
import type { PluginContext } from "../../plugin/pluginContext.js";

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
 * @param context The plugin context providing access to Langium modules
 * @param plugins Array of language plugins to include in the module
 * @returns A complete language module ready for use with Langium services
 */
export function createModule(
    { "langium/grammar": langiumGrammar, langium }: PluginContext,
    plugins: LanguagePlugin<any>[]
): LanguageModule {
    const serializedGrammars = new Map<LanguagePlugin<any>, string>();
    for (const plugin of plugins) {
        const serializableGrammar = new GrammarSerializer(plugin.rootRule, plugin.additionalTerminals).grammar;
        const serializedGrammar = JSON.stringify(serializableGrammar);
        serializedGrammars.set(plugin, serializedGrammar);
    }
    const astTypes = langiumGrammar.collectAst(
        [...serializedGrammars.values()].map((serializedGrammar) => langium.loadGrammarFromJson(serializedGrammar))
    );
    const astTypesFiltered: AstTypes = {
        interfaces: [...astTypes.interfaces],
        unions: astTypes.unions.filter((e) => langiumGrammar.isAstType(e.type))
    };
    const typeHierarchy = langiumGrammar.collectTypeHierarchy(langiumGrammar.mergeTypesAndInterfaces(astTypesFiltered));

    class AstReflection extends langium.AbstractAstReflection {
        override readonly types = Object.fromEntries([
            ...astTypesFiltered.interfaces.map((iface) => [
                iface.name,
                buildTypeMetaData(
                    iface.name,
                    iface.properties,
                    typeHierarchy.superTypes.get(iface.name),
                    langiumGrammar
                )
            ]),
            ...astTypesFiltered.unions.map((union) => [
                union.name,
                buildTypeMetaData(union.name, [], typeHierarchy.superTypes.get(union.name), langiumGrammar)
            ])
        ]);
    }

    return {
        grammars: new Map(
            [...serializedGrammars.entries()].map(([plugin, serializedGrammar]) => [
                plugin,
                langium.loadGrammarFromJson(serializedGrammar)
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
function buildTypeMetaData(
    name: string,
    props: Property[],
    superTypes: readonly string[] | undefined,
    langiumGrammar: PluginContext["langium/grammar"]
): TypeMetaData {
    return {
        name,
        properties: Object.fromEntries(
            props.map((property) => [property.name, buildPropertyMetaData(property, langiumGrammar)])
        ),
        superTypes: [...(superTypes ?? [])]
    };
}

/**
 * Builds property metadata for a specific property of an AST node.
 *
 * @param property The property definition to build metadata for
 * @param langiumGrammar The langium grammar module for type resolution
 * @returns Complete property metadata including reference type information
 */
function buildPropertyMetaData(property: Property, langiumGrammar: PluginContext["langium/grammar"]): PropertyMetaData {
    const refTypes = langiumGrammar.findReferenceTypes(property.type);
    const refType = refTypes.length > 0 ? refTypes[0] : undefined;
    return {
        name: property.name,
        defaultValue: property.defaultValue,
        referenceType: refType
    };
}
