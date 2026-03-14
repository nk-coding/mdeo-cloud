import type { VNode } from "snabbdom";
import type { IconNode } from "lucide";

/**
 * Service for converting icon names and icon nodes to Snabbdom VNode objects.
 *
 * This injectable service can be overridden per diagram to support custom icons.
 * The default implementation uses lucide icons, but custom implementations can
 * provide SVG-based or other icon formats as needed.
 */
export interface IconRegistry {
    /**
     * Get an icon by name and optionally create a VNode with custom styling.
     *
     * @param iconName The name of the icon to retrieve (e.g., "trash", "settings", "plus")
     * @param size Optional size in pixels for the icon
     * @param cssClass Optional CSS class names to apply to the icon
     * @returns VNode or VNode array representing the icon, or undefined if not found
     */
    getIcon(iconName: string, size?: number, cssClass?: string): VNode | VNode[] | undefined;

    /**
     * Convert a raw Lucide IconNode to a Snabbdom VNode.
     *
     * This allows rendering icons from other sources that are already
     * in the lucide IconNode format (array of [tag, attrs] tuples).
     *
     * @param iconNode The raw IconNode from lucide
     * @param size Optional size in pixels for the icon
     * @param cssClass Optional CSS class names to apply
     * @returns VNode representing the icon
     */
    iconNodeToVNode(iconNode: IconNode, size?: number, cssClass?: string): VNode;
}

/**
 * DI token for resolving the active {@link IconRegistry} implementation.
 */
export const IconRegistryKey = Symbol("IconRegistry");
