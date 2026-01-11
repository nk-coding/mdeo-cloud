import type { InterfaceDeclarationValue, Interface } from "./types.js";
import { isOptional, isResolve, isReference, isUnion } from "./helpers.js";
import type { BaseType } from "../types.js";
import { primitiveTypeLookup } from "../primitiveTypeLookup.js";

/**
 * Maps interface declaration values to their corresponding Langium AST type representations.
 * This function is the core transformation logic that converts TypeScript-style type declarations
 * into Langium's internal AST format.
 *
 * The mapping process handles several different type patterns:
 *
 * **Wrapper Types:**
 * - `Optional(T)` → Maps T (optionality is handled at the declaration level)
 * - `Resolve(T)` → Maps T (resolve is handled at the declaration level)
 *
 * **Reference Types:**
 * - `Ref(T)` → `ReferenceType` with target type T
 *
 * **Union Types:**
 * - `Union("A", "B", "C")` → `UnionType` with string literal alternatives
 *
 * **Array Types:**
 * - `[T]` → `ArrayType` with element type T
 *
 * **Primitive Types:**
 * - `String`, `Number`, etc. → `SimpleType` with primitive type
 *
 * **Custom Types:**
 * - Interface/Type references → `SimpleType` with type reference
 *
 * @param type The interface declaration value to map
 * @returns The corresponding Langium grammar type representation
 */
export function mapType(type: InterfaceDeclarationValue): Interface<any>["attributes"][number]["type"] {
    if (isOptional(type)) {
        return mapType(type.optional);
    }
    if (isResolve(type)) {
        return mapType(type.resolve);
    }
    if (isReference(type)) {
        const refType = type.ref;
        return {
            $type: "ReferenceType",
            isMulti: false,
            referenceType: {
                $type: "SimpleType",
                typeRef: () => {
                    if (typeof refType === "function") {
                        return refType();
                    } else {
                        return refType;
                    }
                }
            }
        };
    }
    if (isUnion(type)) {
        return {
            $type: "UnionType",
            types: type.union.map((entry) => ({
                $type: "SimpleType",
                stringType: entry as string
            }))
        };
    }
    if (Array.isArray(type)) {
        const elementType = mapType(type[0]);
        return {
            $type: "ArrayType",
            elementType
        };
    }
    const primitiveType = primitiveTypeLookup.get(type as any);
    if (primitiveType != undefined) {
        return {
            $type: "SimpleType",
            primitiveType
        };
    }
    return {
        $type: "SimpleType",
        typeRef: () => {
            if (typeof type === "function") {
                return (type as () => BaseType<any>)();
            } else {
                return type as BaseType<any>;
            }
        }
    };
}
