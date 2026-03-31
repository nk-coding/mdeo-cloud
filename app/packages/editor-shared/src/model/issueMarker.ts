import type { GIssue, GIssueSeverity, GParentElement } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";

const { SIssueMarkerImpl, GDecoration } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Custom issue marker that extends {@link SIssueMarkerImpl} directly rather
 * than GLSP's `GIssueMarker`.
 *
 * Bypassing `GIssueMarker` means the `target instanceof GIssueMarker` check
 * inside `GlspHoverMouseListener.startMouseOverTimer` returns `false`, which
 * prevents GLSP from attempting to open the unsupported `pre-rendered` popup
 * (id `popup-title`) that causes console warnings on scroll/hover.
 */
export class GIssueMarker extends SIssueMarkerImpl {
    override issues: GIssue[] = [];
    override type = "marker";

    constructor() {
        super();
        this.features = new Set<symbol>(GDecoration.DEFAULT_FEATURES);
    }
}

/**
 * Returns the first {@link GIssueMarker} child of `modelElement`, or
 * `undefined` if none exists.
 *
 * @param modelElement The parent element whose children are searched.
 * @returns The first {@link GIssueMarker} child, or `undefined`.
 */
export function getIssueMarker(modelElement: GParentElement): GIssueMarker | undefined {
    for (const child of modelElement.children) {
        if (child instanceof GIssueMarker) {
            return child;
        }
    }
    return undefined;
}

/**
 * Returns the existing {@link GIssueMarker} child of `modelElement`, creating
 * and appending a new one if none exists.
 *
 * @param modelElement The parent element to search or add a marker to.
 * @returns The existing or newly created {@link GIssueMarker}.
 */
export function getOrCreateIssueMarker(modelElement: GParentElement): GIssueMarker {
    const existing = getIssueMarker(modelElement);
    if (existing !== undefined) {
        return existing;
    }
    const marker = new GIssueMarker();
    marker.id = modelElement.id + "_marker";
    modelElement.add(marker);
    return marker;
}

/**
 * Returns the highest-priority {@link GIssueSeverity} present in `marker`.
 *
 * Severity priority (highest to lowest): `error` > `warning` > `info`.
 *
 * @param marker The issue marker whose issues are inspected.
 * @returns The maximum severity found, defaulting to `"info"`.
 */
function getMaxSeverity(marker: GIssueMarker): GIssueSeverity {
    let current: GIssueSeverity = "info";
    for (const { severity } of marker.issues) {
        if (severity === "error") {
            return "error";
        }
        if (severity === "warning") {
            current = "warning";
        }
    }
    return current;
}

/**
 * Adds or replaces the severity CSS class (`"info"`, `"warning"`, or
 * `"error"`) on `modelElement` based on the highest-priority issue in
 * `marker`.
 *
 * Any existing severity class is removed before the new one is added.
 *
 * @param modelElement The element whose `cssClasses` array is updated.
 * @param marker       - The issue marker that determines the severity class.
 */
export function addSeverityCssClass(modelElement: GParentElement, marker: GIssueMarker): void {
    const cls = getMaxSeverity(marker);
    if (!modelElement.cssClasses) {
        modelElement.cssClasses = [cls];
    } else {
        modelElement.cssClasses = modelElement.cssClasses.filter((v) => !/^(info|warning|error)$/.test(v));
        modelElement.cssClasses.push(cls);
    }
}
