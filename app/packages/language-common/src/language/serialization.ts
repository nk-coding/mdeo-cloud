import type { languages } from "monaco-editor";
import type { DeepSerializeRegex, SerializedRegex } from "@mdeo/plugin";

/**
 * Checks if a value is a SerializedRegex object.
 *
 * @param value The value to check
 * @returns True if the value is a SerializedRegex, false otherwise
 */
function isSerializedRegex(value: unknown): value is SerializedRegex {
    return (
        typeof value === "object" &&
        value !== null &&
        "__regex" in value &&
        value.__regex === true &&
        "source" in value &&
        "flags" in value
    );
}

/**
 * Deeply serializes RegExp objects in a value to SerializedRegex objects.
 *
 * @param value The value to serialize
 * @returns The serialized value
 */
export function serializeMonarchTokensProvider(
    value: languages.IMonarchLanguage
): DeepSerializeRegex<languages.IMonarchLanguage> {
    return deepSerialize(value) as DeepSerializeRegex<languages.IMonarchLanguage>;
}

/**
 * Deeply deserializes SerializedRegex objects in a value back to RegExp objects.
 *
 * @param value The value to deserialize
 * @returns The deserialized value
 */
export function deserializeMonarchTokensProvider(
    value: DeepSerializeRegex<languages.IMonarchLanguage>
): languages.IMonarchLanguage {
    return deepDeserialize(value) as languages.IMonarchLanguage;
}

/**
 * Recursively serializes RegExp objects within a value.
 *
 * @param value The value to serialize
 * @returns The serialized value
 */
function deepSerialize(value: unknown): unknown {
    if (value instanceof RegExp) {
        return {
            __regex: true,
            source: value.source,
            flags: value.flags
        } satisfies SerializedRegex;
    }

    if (Array.isArray(value)) {
        return value.map((item) => deepSerialize(item));
    }

    if (typeof value === "object" && value !== null) {
        const result: Record<string, unknown> = {};
        for (const [key, val] of Object.entries(value)) {
            result[key] = deepSerialize(val);
        }
        return result;
    }

    return value;
}

/**
 * Recursively deserializes SerializedRegex objects within a value.
 *
 * @param value The value to deserialize
 * @returns The deserialized value
 */
function deepDeserialize(value: unknown): unknown {
    if (isSerializedRegex(value)) {
        return new RegExp(value.source, value.flags);
    }

    if (Array.isArray(value)) {
        return value.map((item: unknown) => deepDeserialize(item));
    }

    if (typeof value === "object" && value !== null) {
        const result: Record<string, unknown> = {};
        for (const [key, val] of Object.entries(value)) {
            result[key] = deepDeserialize(val);
        }
        return result;
    }

    return value;
}
