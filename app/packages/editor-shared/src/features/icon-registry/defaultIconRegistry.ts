import type { VNode } from "snabbdom";
import type { IconNode } from "lucide";
import { sharedImport } from "../../sharedImport.js";
import type { IconRegistry } from "./iconRegistry.js";

const { injectable } = sharedImport("inversify");
const { icons } = sharedImport("lucide");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Converts kebab-case icon names to PascalCase Lucide export names.
 *
 * @param iconName Icon name in kebab-case format
 * @returns Icon name in PascalCase format
 */
function toLucideExportName(iconName: string): string {
    return iconName
        .split("-")
        .filter((part) => part.length > 0)
        .map((part) => `${part[0].toUpperCase()}${part.slice(1).toLowerCase()}`)
        .join("");
}

/**
 * Default implementation of IconRegistry using lucide icons.
 *
 * Provides a mapping of icon names (kebab-case) to lucide IconNode objects,
 * which can be converted to Snabbdom VNodes for rendering in the editor.
 *
 * Each icon can be customized with size and CSS classes during conversion.
 * This class is injectable and can be used as a singleton service throughout
 * the editor application, or overridden per diagram for custom icon implementations.
 *
 * @example
 * ```typescript
 * @injectable()
 * export class MyDiagramModule extends GLSPModule {
 *   configureServices(services: GLSPServices): void {
 *     services.set(IconRegistry, new DefaultIconRegistry());
 *   }
 * }
 * ```
 */
@injectable()
export class DefaultIconRegistry implements IconRegistry {
    /**
     * Get an icon by name and convert it to a Snabbdom VNode.
     *
     * The icon name is case-insensitive and should use kebab-case format
     * (e.g., "trash", "settings", "help-circle"). If the icon is not found,
     * returns undefined.
     *
     * @param iconName The name of the icon to retrieve (e.g., "trash", "settings")
     * @param size Optional size in pixels for the icon (default: 24)
     * @param cssClass Optional CSS class names to apply
     * @returns VNode representing the icon, or undefined if not found
     */
    getIcon(iconName: string, size: number = 24, cssClass?: string): VNode | undefined {
        const normalizedName = iconName.toLowerCase().trim();
        const lookupName = toLucideExportName(normalizedName);
        const iconNode = (icons as Record<string, IconNode | undefined>)[lookupName];

        if (!iconNode) {
            return undefined;
        }

        return this.iconNodeToVNode(iconNode, size, cssClass);
    }

    /**
     * Convert a raw Lucide IconNode to a Snabbdom VNode.
     *
     * Lucide IconNode is an array of [tag, attributes] tuples that define
     * the SVG structure. This method converts them to a Snabbdom HTML VNode.
     *
     * The resulting VNode will have:
     * - SVG root element with viewBox="0 0 24 24"
     * - Appropriate stroke styling for outline icons
     * - Optional width/height attributes based on size parameter
     * - Optional CSS classes applied to the SVG element
     *
     * @param iconNode The raw IconNode from lucide
     * @param size Optional size in pixels (applied as width/height attributes)
     * @param cssClass Optional CSS class names (space-separated string)
     * @returns VNode representing the icon as an SVG element
     */
    iconNodeToVNode(iconNode: IconNode, size: number = 24, cssClass?: string): VNode {
        const classObj = cssClass ? Object.fromEntries(cssClass.split(/\s+/).map((cls) => [cls, true])) : {};

        const baseAttrs: Record<string, string | number> = {
            viewBox: "0 0 24 24",
            fill: "none",
            stroke: "currentColor",
            "stroke-width": "2",
            "stroke-linecap": "round",
            "stroke-linejoin": "round"
        };

        if (size) {
            baseAttrs.width = size;
            baseAttrs.height = size;
        }

        return html(
            "svg",
            {
                attrs: baseAttrs,
                class: {
                    icon: true,
                    "icon-svg": true,
                    ...classObj
                }
            },
            ...iconNode.map(([tag, attrs]) =>
                html(tag, {
                    attrs: Object.fromEntries(
                        Object.entries(attrs).filter(([_, value]) => value != undefined)
                    ) as Record<string, string | number>
                })
            )
        );
    }
}
