import type { IconNode } from "lucide";

/**
 * Custom icon for a plain (no decorator) association line.
 * Rendered as a single diagonal line without any arrow head or diamond.
 */
export const PlainLineIcon: IconNode = [["path", { d: "M1.5 22.5L22.5 1.5" }]];

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
 * Filled diamond at the bottom-left end (source/start), line to top-right.
 */
export const CompositionIcon: IconNode = [
    ["path", { d: "M1.5 22.5L10.25 21.25L11.5 12.5L2.75 13.75Z", fill: "currentColor" }],
    ["path", { d: "M11.5 12.5L22.5 1.5" }]
];

/**
 * Custom icon for a navigable composition association.
 * Filled diamond at the bottom-left end (source/start), arrowhead at top-right (target).
 */
export const NavigableCompositionIcon: IconNode = [
    ["path", { d: "M1.5 22.5L10.25 21.25L11.5 12.5L2.75 13.75Z", fill: "currentColor" }],
    ["path", { d: "M11.5 12.5L22.5 1.5" }],
    ["path", { d: "M12.5 4.5L22.5 1.5L19.5 11.5" }]
];

/**
 * Custom icon for an extends (inheritance) relationship.
 */
export const ExtendsIcon: IconNode = [
    ["path", { d: "M1.5 22.5L14.5 9.5" }],
    ["path", { d: "M22.5 1.5L9 6L18 15Z" }]
];
