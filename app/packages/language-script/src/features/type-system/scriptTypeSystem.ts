import {
    AnyType,
    BagType,
    booleanType,
    CollectionType,
    doubleType,
    ExpressionTypeSystem,
    floatType,
    intType,
    IterableType,
    ListType,
    longType,
    OrderedCollectionType,
    OrderedSetType,
    ReadonlyBagType,
    ReadonlyCollectionType,
    ReadonlyListType,
    ReadonlyOrderedCollectionType,
    ReadonlyOrderedSetType,
    ReadonlySetType,
    SetType,
    StatementPartialTypeSystem,
    stringType,
    TypePartialTypeSystem,
    typeRef
} from "@mdeo/language-expression";
import type { ScriptTypirServices, ScriptTypirSpecifics } from "../../plugin.js";
import { expressionTypes, statementTypes, typeTypes } from "../../grammar/scriptTypes.js";
import { ScriptPartialTypeSystem } from "./scriptPartialTypeSystem.js";
import type { ResolvedScriptContributionPlugins } from "../../plugin/scriptContributionPlugin.js";
import { stdlibGlobalFunctions } from "../stdlib/globalFunctions.js";

/**
 * The type system for the Script language.
 */
export class ScriptTypeSystem extends ExpressionTypeSystem<ScriptTypirSpecifics> {
    /**
     * Creates an instance of ScriptTypeSystem.
     *
     * @param plugins The contribution plugins for the Script language.
     */
    constructor(private readonly plugins: ResolvedScriptContributionPlugins) {
        super(
            {
                Any: AnyType,
                int: intType,
                long: longType,
                float: floatType,
                double: doubleType,
                string: stringType,
                boolean: booleanType,
                Iterable: IterableType,
                additionalTypes: [
                    CollectionType,
                    ReadonlyCollectionType,
                    OrderedCollectionType,
                    ReadonlyOrderedCollectionType,
                    ListType,
                    ReadonlyListType,
                    SetType,
                    ReadonlySetType,
                    BagType,
                    ReadonlyBagType,
                    OrderedSetType,
                    ReadonlyOrderedSetType
                ],
                lambdaSuperTypes: [{ type: AnyType.name }],
                createListType: (elementType) => typeRef("List").withTypeArgs({ T: elementType }).build()
            },
            expressionTypes,
            [
                ...stdlibGlobalFunctions,
                ...[...plugins.functions.entries()].map(([name, func]) => ({
                    name,
                    isProperty: false,
                    type: func.function
                }))
            ]
        );
    }

    protected override onInitializeExtended(typir: ScriptTypirServices): void {
        const statementPartialTypeSystem = new StatementPartialTypeSystem<ScriptTypirSpecifics>(
            typir,
            statementTypes,
            this.expressionTypes,
            this.primitiveTypes,
            this.nullablePrimitiveTypes,
            this.defaultTypeConfig.Iterable
        );
        statementPartialTypeSystem.registerRules();
        const typePartialTypeSystem = new TypePartialTypeSystem<ScriptTypirSpecifics>(
            typir,
            typeTypes,
            this.nullablePrimitiveTypes.Any
        );
        typePartialTypeSystem.registerRules();

        const scriptPartialTypeSystem = new ScriptPartialTypeSystem(typir, this.primitiveTypes, this.plugins);
        scriptPartialTypeSystem.registerRules();
    }
}
