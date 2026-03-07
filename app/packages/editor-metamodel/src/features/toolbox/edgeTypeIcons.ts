import type { IconNode } from "lucide";

/**
 * Custom icon for a unidirectional association arrow.
 */
export const UnidirectionalAssociationIcon: IconNode = [
    ["path", { d: "M1.5 22.5L22.5 1.5" }],
    ["path", { d: "M12.5 4.5L22.5 1.5L19.5 11.5" }]
];

/**
 * Custom icon for a bidirectional association arrow.
 * Diagonal line from bottom-left to top-right with balanced arrowheads at both ends.
 */
export const BidirectionalAssociationIcon: IconNode = [
    ["path", { d: "M1.5 22.5L22.5 1.5" }],
    ["path", { d: "M12.5 4.5L22.5 1.5L19.5 11.5" }],
    ["path", { d: "M11.5 19.5L1.5 22.5L4.5 12.5" }]
];

/**
 * Custom icon for a composition association.
 * Filled diamond at the top-right end (target/end), line to bottom-left.
 */
export const CompositionIcon: IconNode = [
    ["path", { d: "M22.5 1.5L13.75 2.75L12.5 11.5L21.25 10.25Z", fill: "currentColor" }],
    ["path", { d: "M12.5 11.5L1.5 22.5" }]
];

/**
 * Custom icon for a navigable composition association.
 * Filled diamond at the top-right end (target/end), arrowhead at bottom-left (source).
 */
export const NavigableCompositionIcon: IconNode = [
    ["path", { d: "M22.5 1.5L13.75 2.75L12.5 11.5L21.25 10.25Z", fill: "currentColor" }],
    ["path", { d: "M12.5 11.5L1.5 22.5" }],
    ["path", { d: "M11.5 19.5L1.5 22.5L4.5 12.5" }]
];

/**
 * Custom icon for an extends (inheritance) relationship.
 */
export const ExtendsIcon: IconNode = [
    ["path", { d: "M1.5 22.5L14.5 9.5" }],
    ["path", { d: "M22.5 1.5L9 6L18 15Z" }]
];
