import type { GIssueMarker } from "../../model/issueMarker.js";

/**
 * Physical size (in CSS pixels) of a rendered issue marker badge at zoom 1.
 * The badge is square; width and height are equal to this value.
 */
export const ISSUE_MARKER_SIZE = 16;

/**
 * Returns display information for a severity that is used by the popup UI.
 *
 * @param severity The severity string ("error" | "warning" | "info")
 * @returns Tailwind text class and capitalised label for this severity
 */
export function getSeverityDisplayConfig(severity: string): { textClass: string; label: string } {
    switch (severity) {
        case "error":
            return { textClass: "text-destructive", label: "Error" };
        case "warning":
            return { textClass: "text-chart-5", label: "Warning" };
        case "info":
        default:
            return { textClass: "text-sidebar-primary", label: "Info" };
    }
}

/**
 * Narrows an unknown child element to {@link GIssueMarker} by checking its GLSP
 * type discriminant (`type === "marker"`).
 *
 * Using this type guard avoids a runtime `instanceof` dependency on the
 * dynamically loaded `GIssueMarker` class.
 *
 * @param child Any model element child
 * @returns `true` when the child is a GIssueMarker
 */
export function isIssueMarker(child: unknown): child is GIssueMarker {
    return child != null && typeof child === "object" && (child as { type?: string }).type === "marker";
}
