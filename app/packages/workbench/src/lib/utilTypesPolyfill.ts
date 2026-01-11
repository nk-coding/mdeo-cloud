/**
 * Polyfill for Node.js util/types module.
 * Required by typir package for browser compatibility.
 * See: https://github.com/TypeFox/typir/issues/96
 */

export function isSet(value: unknown): value is Set<unknown> {
    return value instanceof Set;
}

export function isMap(value: unknown): value is Map<unknown, unknown> {
    return value instanceof Map;
}
