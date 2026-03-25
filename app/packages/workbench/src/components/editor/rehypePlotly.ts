import type { Root, Element, ElementContent } from "hast";
import { visit } from "unist-util-visit";

/**
 * Descriptor for a plotly chart embedded in markdown.
 */
export interface PlotEmbed {
    /**
     * Unique DOM element ID for the placeholder div.
     */
    id: string;
    /**
     * The plotly.js-compatible JSON configuration string.
     */
    json: string;
}

/**
 * Rehype plugin that transforms fenced code blocks with language "plot"
 * into placeholder `<div>` elements for plotly.js chart rendering.
 *
 * Code blocks like:
 * ```plot
 * {"data":[...],"layout":{...}}
 * ```
 *
 * are replaced with `<div id="plot-{n}" class="plotly-placeholder"></div>`.
 *
 * @param onPlot Callback invoked for each discovered plot code block.
 * @param idGenerator A function that generates unique IDs for the placeholder divs.
 */
export function rehypePlotly(onPlot: (plot: PlotEmbed) => void, idGenerator: () => string) {
    return (tree: Root) => {
        visit(tree, "element", (node: Element, index, parent) => {
            if (node.tagName !== "pre" || index == null || parent == null) {
                return;
            }

            const codeElement = node.children.find(
                (child): child is Element => child.type === "element" && child.tagName === "code"
            );
            if (!codeElement) {
                return;
            }

            const className = codeElement.properties?.className;
            const classes = Array.isArray(className) ? className : [className];
            if (!classes.includes("language-plot")) {
                return;
            }

            const textNode = codeElement.children.find((child) => child.type === "text");
            if (!textNode || textNode.type !== "text") {
                return;
            }

            const json = textNode.value.trim();
            const plotId = idGenerator();

            const placeholder: Element = {
                type: "element",
                tagName: "div",
                properties: {
                    id: plotId,
                    className: "plotly-placeholder"
                },
                children: []
            };

            parent.children[index] = placeholder as ElementContent;
            onPlot({ id: plotId, json });
        });
    };
}
