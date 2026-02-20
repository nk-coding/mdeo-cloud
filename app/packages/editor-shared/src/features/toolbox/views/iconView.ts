import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";
import type { IconNode } from "lucide";

const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Generates a Lucide icon as a VNode.
 * Uses SVG attributes directly without innerHTML for security.
 *
 * @param icon The Lucide IconNode to render
 * @param classes Optional CSS class names to apply
 * @returns The rendered SVG VNode
 */
export function generateIcon(icon: IconNode, classes: string[] = []): VNode {
    const classObj = Object.fromEntries(classes.map((cls) => [cls, true]));
    return html(
        "svg",
        {
            attrs: {
                viewBox: "0 0 24 24",
                fill: "none",
                stroke: "currentColor",
                "stroke-width": "2",
                "stroke-linecap": "round",
                "stroke-linejoin": "round"
            },
            class: {
                "toolbox-icon": true,
                "size-4": true,
                ...classObj
            }
        },
        ...icon.map(([tag, attrs]) =>
            html(tag, {
                attrs: Object.fromEntries(Object.entries(attrs).filter(([_, value]) => value != undefined)) as Record<
                    string,
                    string | number
                >
            })
        )
    );
}
