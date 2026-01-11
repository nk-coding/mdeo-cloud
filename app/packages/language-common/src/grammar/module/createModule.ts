import type { AstNode, PropertyMetaData, TypeMetaData } from "langium";
import type { PluginContext } from "../../plugin/pluginContext.js";
import { GrammarSerializer } from "../serialization/grammarSerializer.js";
import type { AstTypes, Property } from "langium/grammar";
import type { LanguagePlugin } from "../../plugin/languagePlugin.js";
import type { LanguageModule, AstReflection } from "./module.js";
import type { Interface } from "../type/interface/types.js";

/**
 * Creates a complete language module from a parser rule and additional terminal rules.
 *
 * @param plugins Array of language plugins to include in the module
 * @param context The plugin context providing necessary dependencies
 * @returns A complete language module ready for use with Langium services
 */
export function createModule(
    plugins: LanguagePlugin<any>[],
    { "langium/grammar": langiumGrammar, langium }: PluginContext
): LanguageModule {
    const serializedGrammars = new Map<LanguagePlugin<any>, string>();
    for (const plugin of plugins) {
        const serializableGrammar = new GrammarSerializer(plugin.rootRule, plugin.additionalTerminals).grammar;
        const serializedGrammar = JSON.stringify(serializableGrammar);
        serializedGrammars.set(plugin, serializedGrammar);
    }
    const { loadGrammarFromJson, AbstractAstReflection } = langium;

    const astTypes = langiumGrammar.collectAst(
        [...serializedGrammars.values()].map((serializedGrammar) => loadGrammarFromJson(serializedGrammar))
    );
    const astTypesFiltered: AstTypes = {
        interfaces: [...astTypes.interfaces],
        unions: astTypes.unions.filter((e: any) => langiumGrammar.isAstType(e.type))
    };
    const typeHierarchy = langiumGrammar.collectTypeHierarchy(langiumGrammar.mergeTypesAndInterfaces(astTypesFiltered));

    class RuntimeAstReflection extends AbstractAstReflection {
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

        override isInstance(node: unknown, type: string): boolean;
        override isInstance<T extends AstNode>(node: unknown, type: Interface<T>): node is T;
        override isInstance(node: unknown, type: string | Interface<any>): boolean {
            if (typeof type === "string") {
                return super.isInstance(node, type);
            }
            return super.isInstance(node, type.name);
        }
    }

    return {
        grammars: new Map(
            [...serializedGrammars.entries()].map(([plugin, serializedGrammar]) => [
                plugin,
                loadGrammarFromJson(serializedGrammar)
            ])
        ),
        reflection: new RuntimeAstReflection() as unknown as AstReflection
    };
}

/**
 * Builds property metadata for a specific property of an AST node.
 *
 * @param property The property definition to build metadata for
 * @param langiumGrammar The `langium/grammar` helpers for reference type resolution
 * @returns Complete property metadata including reference type information
 */
function buildPropertyMetaData(property: Property, langiumGrammar: PluginContext["langium/grammar"]): PropertyMetaData {
    const refTypes = langiumGrammar.findReferenceTypes(property.type) as string[];
    const refType = refTypes.length > 0 ? refTypes[0] : undefined;
    return {
        name: property.name,
        defaultValue: property.defaultValue,
        referenceType: refType
    };
}

/**
 * Builds type metadata for a specific AST node type.
 *
 * @param name The name of the AST node type
 * @param props Array of properties defined on this type
 * @param superTypes Array of parent type names this type extends from
 * @param langiumGrammar The `langium/grammar` helpers for reference type resolution
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
