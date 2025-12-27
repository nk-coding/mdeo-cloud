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
    type ExpressionTypirServices
} from "@mdeo/language-expression";
import type { ScriptTypirSpecifics } from "../plugin.js";
import { expressionTypes, statementTypes, typeTypes } from "../grammar/types.js";

/**
 * The type system for the Script language.
 */
export class ScriptTypeSystem extends ExpressionTypeSystem<ScriptTypirSpecifics> {
    constructor() {
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
                lambdaSuperTypes: [{ type: AnyType.name }]
            },
            expressionTypes,
            []
        );
    }

    protected override onInitializeExtended(typir: ExpressionTypirServices<ScriptTypirSpecifics>): void {
        const statementPartialTypeSystem = new StatementPartialTypeSystem<ScriptTypirSpecifics>(
            typir,
            statementTypes,
            this.expressionTypes,
            this.primitiveTypes,
            this.nullablePrimitiveTypes,
            this.voidType,
            this.defaultTypeConfig.Iterable
        );
        statementPartialTypeSystem.registerRules();
        const typePartialTypeSystem = new TypePartialTypeSystem<ScriptTypirSpecifics>(
            typir,
            typeTypes,
            this.nullablePrimitiveTypes.Any
        );
        typePartialTypeSystem.registerRules();
    }
}
