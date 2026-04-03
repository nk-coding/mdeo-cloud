import type { CustomValueType } from "../typir-extensions/kinds/custom-value/custom-value-type.js";
import {
    ClassTypeRef,
    GenericTypeRef,
    VoidType,
    type Method,
    type ValueType
} from "../typir-extensions/config/type.js";

/**
 * Converts a {@link ValueType} to its display string, resolving generic type arguments.
 *
 * Handles generic references (substituted from `typeArgs`), class types (with optional
 * type parameters and nullability), and lambda types.
 *
 * @param type The value type to render.
 * @param typeArgs Map of generic parameter names to their resolved concrete types.
 * @returns A human-readable string representation of the type.
 */
export function memberValueTypeToString(type: ValueType, typeArgs: Map<string, CustomValueType>): string {
    if (GenericTypeRef.is(type)) {
        const resolved = typeArgs.get(type.generic);
        return resolved != null ? resolved.getName() : type.generic;
    } else if (ClassTypeRef.is(type)) {
        const base = type.type;
        if (type.typeArgs != null && Object.keys(type.typeArgs).length > 0) {
            const argsStr = Object.values(type.typeArgs)
                .map((arg) => memberValueTypeToString(arg, typeArgs))
                .join(", ");
            return `${base}<${argsStr}>${type.isNullable ? "?" : ""}`;
        }
        return `${base}${type.isNullable ? "?" : ""}`;
    } else {
        const params = type.parameters.map((p) => memberValueTypeToString(p.type, typeArgs)).join(", ");
        const ret = VoidType.is(type.returnType)
            ? "void"
            : memberValueTypeToString(type.returnType as ValueType, typeArgs);
        const lambda = `(${params}) => ${ret}`;
        return type.isNullable ? `(${lambda})?` : lambda;
    }
}

/**
 * Renders the detail string for a method, in the form `(param1: Type1, param2: Type2): ReturnType`.
 *
 * Uses the first available signature for overloaded methods. Generic type arguments are
 * resolved via `typeArgs`.
 *
 * @param method The method whose signature to render.
 * @param typeArgs Map of generic parameter names to their resolved concrete types.
 * @returns A string such as `(x: Int, y: String): Boolean`.
 */
export function memberMethodDetailString(method: Method, typeArgs: Map<string, CustomValueType>): string {
    const signatures = Object.values(method.type.signatures);
    if (signatures.length === 0) {
        return "()";
    }
    const sig = signatures[0];
    const params = sig.parameters.map((p) => `${p.name}: ${memberValueTypeToString(p.type, typeArgs)}`).join(", ");
    const ret = VoidType.is(sig.returnType) ? "void" : memberValueTypeToString(sig.returnType as ValueType, typeArgs);
    return `(${params}): ${ret}`;
}
