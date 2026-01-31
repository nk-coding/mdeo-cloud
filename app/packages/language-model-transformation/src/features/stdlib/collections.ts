import {
    BagType,
    CollectionType,
    ListType,
    OrderedCollectionType,
    OrderedSetType,
    ReadonlyBagType,
    ReadonlyCollectionType,
    ReadonlyListType,
    ReadonlyOrderedCollectionType,
    ReadonlyOrderedSetType,
    ReadonlySetType,
    SetType,
    classTypeFrom,
    type ClassType
} from "@mdeo/language-expression";

/**
 * Immutable Collection type for model transformation.
 * This type removes all mutating methods (add, remove, clear, etc.) and void-returning methods (forEach)
 * from the base Collection type, making it suitable for use in transformations where side effects
 * should be avoided.
 *
 * @remarks
 * Omitted methods:
 * - add: Modifies the collection
 * - addAll: Modifies the collection
 * - clear: Modifies the collection
 * - remove: Modifies the collection
 * - removeAll: Modifies the collection
 * - forEach: Returns void
 */
export const ModelTransformationCollectionType: ClassType = classTypeFrom(CollectionType)
    .omitMembers("add", "addAll", "clear", "remove", "removeAll", "forEach")
    .build();

/**
 * Immutable ReadonlyCollection type for model transformation.
 * This type removes void-returning methods (forEach) from the base ReadonlyCollection type.
 *
 * @remarks
 * Omitted methods:
 * - forEach: Returns void
 */
export const ModelTransformationReadonlyCollectionType: ClassType = classTypeFrom(ReadonlyCollectionType)
    .omitMembers("forEach")
    .build();

/**
 * Immutable OrderedCollection type for model transformation.
 * This type removes all mutating methods and void-returning methods from the base OrderedCollection type.
 *
 * @remarks
 * Omitted methods (in addition to those from Collection):
 * - removeAt: Modifies the collection
 * - sortBy: Modifies the collection
 * - add: Modifies the collection
 * - addAll: Modifies the collection
 * - clear: Modifies the collection
 * - remove: Modifies the collection
 * - removeAll: Modifies the collection
 * - forEach: Returns void
 */
export const ModelTransformationOrderedCollectionType: ClassType = classTypeFrom(OrderedCollectionType)
    .omitMembers("removeAt", "sortBy", "add", "addAll", "clear", "remove", "removeAll", "forEach")
    .build();

/**
 * Immutable ReadonlyOrderedCollection type for model transformation.
 * This type removes void-returning methods from the base ReadonlyOrderedCollection type.
 *
 * @remarks
 * Omitted methods:
 * - forEach: Returns void
 */
export const ModelTransformationReadonlyOrderedCollectionType: ClassType = classTypeFrom(ReadonlyOrderedCollectionType)
    .omitMembers("forEach")
    .build();

/**
 * Immutable List type for model transformation.
 * This type removes all mutating methods and void-returning methods from the base List type,
 * making it suitable for functional transformations.
 *
 * @remarks
 * Lists inherit methods from both ReadonlyList and OrderedCollection.
 * Omitted methods include all mutating operations and void-returning methods.
 */
export const ModelTransformationListType: ClassType = classTypeFrom(ListType)
    .omitMembers("add", "addAll", "clear", "remove", "removeAll", "removeAt", "sortBy", "forEach")
    .build();

/**
 * Immutable ReadonlyList type for model transformation.
 * This type removes void-returning methods from the base ReadonlyList type.
 *
 * @remarks
 * Omitted methods:
 * - forEach: Returns void
 */
export const ModelTransformationReadonlyListType: ClassType = classTypeFrom(ReadonlyListType)
    .omitMembers("forEach")
    .build();

/**
 * Immutable Set type for model transformation.
 * This type removes all mutating methods and void-returning methods from the base Set type.
 *
 * @remarks
 * Omitted methods:
 * - add: Modifies the set
 * - addAll: Modifies the set
 * - clear: Modifies the set
 * - remove: Modifies the set
 * - removeAll: Modifies the set
 * - forEach: Returns void
 */
export const ModelTransformationSetType: ClassType = classTypeFrom(SetType)
    .omitMembers("add", "addAll", "clear", "remove", "removeAll", "forEach")
    .build();

/**
 * Immutable ReadonlySet type for model transformation.
 * This type removes void-returning methods from the base ReadonlySet type.
 *
 * @remarks
 * Omitted methods:
 * - forEach: Returns void
 */
export const ModelTransformationReadonlySetType: ClassType = classTypeFrom(ReadonlySetType)
    .omitMembers("forEach")
    .build();

/**
 * Immutable Bag type for model transformation.
 * This type removes all mutating methods and void-returning methods from the base Bag type.
 *
 * @remarks
 * Bags are collections that allow duplicates but don't maintain order.
 * Omitted methods:
 * - add: Modifies the bag
 * - addAll: Modifies the bag
 * - clear: Modifies the bag
 * - remove: Modifies the bag
 * - removeAll: Modifies the bag
 * - forEach: Returns void
 */
export const ModelTransformationBagType: ClassType = classTypeFrom(BagType)
    .omitMembers("add", "addAll", "clear", "remove", "removeAll", "forEach")
    .build();

/**
 * Immutable ReadonlyBag type for model transformation.
 * This type removes void-returning methods from the base ReadonlyBag type.
 *
 * @remarks
 * Omitted methods:
 * - forEach: Returns void
 */
export const ModelTransformationReadonlyBagType: ClassType = classTypeFrom(ReadonlyBagType)
    .omitMembers("forEach")
    .build();

/**
 * Immutable OrderedSet type for model transformation.
 * This type removes all mutating methods and void-returning methods from the base OrderedSet type.
 *
 * @remarks
 * OrderedSets maintain insertion order and don't allow duplicates.
 * Omitted methods:
 * - add: Modifies the set
 * - addAll: Modifies the set
 * - clear: Modifies the set
 * - remove: Modifies the set
 * - removeAll: Modifies the set
 * - removeAt: Modifies the set
 * - sortBy: Modifies the set
 * - forEach: Returns void
 */
export const ModelTransformationOrderedSetType: ClassType = classTypeFrom(OrderedSetType)
    .omitMembers("add", "addAll", "clear", "remove", "removeAll", "removeAt", "sortBy", "forEach")
    .build();

/**
 * Immutable ReadonlyOrderedSet type for model transformation.
 * This type removes void-returning methods from the base ReadonlyOrderedSet type.
 *
 * @remarks
 * Omitted methods:
 * - forEach: Returns void
 */
export const ModelTransformationReadonlyOrderedSetType: ClassType = classTypeFrom(ReadonlyOrderedSetType)
    .omitMembers("forEach")
    .build();
