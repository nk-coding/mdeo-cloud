import {
    AnyType,
    BagType,
    booleanType,
    CollectionType,
    doubleType,
    ExpressionTypeSystem,
    floatType,
    intType,
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
    stringType,
    voidType
} from "@mdeo/language-expression";
import type { ScriptTypirSpecifics } from "../plugin.js";
import { expressionTypes } from "../grammar/types.js";

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
                void: voidType,
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
            expressionTypes
        );
    }
}
