import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeViewBase } from "@mdeo/editor-shared";
import type { GMatchNode } from "../model/matchNode.js";
import type { GMatchNodeCompartments } from "../model/matchNodeCompartments.js";
import { MATCH_NODE_INNER_PADDING } from "@mdeo/protocol-model-transformation";

const { injectable } = sharedImport("inversify");
const { svg, html, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class GMatchNodeView extends GNodeViewBase {
    /**
     * Padding between the pattern content (instances, links) and the match node's outline/container area.
     */
    static readonly INNER_PADDING = MATCH_NODE_INNER_PADDING;

    /**
     * Minimum width/height for the pattern content area, to ensure a reasonable size when no pattern elements are present or when they have zero-size bounds.
     */
    static readonly MIN_CONTENT_SIZE = 80;

    /**
     * Renders the match node as an SVG group containing:
     * - Background control elements (selection rect, resize handles)
     * - Double-border outlines (single or double, for `multiple` patterns)
     * - Pattern instance children translated into the inner area
     * - The container compartment area below the pattern area
     * - Foreground control elements and issue-marker badges
     *
     * @param model   The match-node model element to render.
     * @param context The current rendering context.
     * @returns An SVG `<g>` VNode for the complete match-node visual, or `undefined`
     *   if the element should not be rendered.
     */
    override render(model: Readonly<GMatchNode>, context: RenderingContext): VNode | undefined {
        const { markers } = this.splitChildren(model);
        const { innerChildren, containerNode, innerChildrenBounds, innerChildrenTranslation } = model.getRenderInfo();

        const patternContentRight = innerChildrenBounds.width + GMatchNodeView.INNER_PADDING * 2;
        const patternAreaHeight = innerChildrenBounds.height + GMatchNodeView.INNER_PADDING * 2;

        const containerBounds = containerNode?.bounds;
        const containerBoundsValid =
            containerBounds != undefined && containerBounds.width >= 0 && containerBounds.height >= 0;
        const containerHeight = containerBoundsValid ? containerBounds.height : 0;
        const containerWidth = containerBoundsValid ? containerBounds.width : 0;

        const totalWidth = Math.max(
            patternContentRight,
            GMatchNodeView.MIN_CONTENT_SIZE + GMatchNodeView.INNER_PADDING * 2,
            containerWidth
        );
        const patternAreaBottomY = patternAreaHeight;
        const totalHeight = patternAreaBottomY + containerHeight;

        const outlines = this.renderOutlines(totalWidth, totalHeight, model.multiple);

        const { x: translateX, y: translateY } = innerChildrenTranslation;
        const innerGroup = svg(
            "g",
            { attrs: { transform: `translate(${translateX}, ${translateY})` } },
            ...innerChildren
                .map((child) => context.renderElement(child))
                .filter((vnode): vnode is VNode => vnode !== undefined)
        );

        const bottomVNodes = this.renderContainerArea(containerNode, context, totalWidth, patternAreaBottomY);

        return svg(
            "g",
            { class: { "cursor-pointer": true } },
            ...this.renderBackgroundControlElements(model),
            ...outlines,
            innerGroup,
            ...bottomVNodes,
            ...this.renderForegroundControlElements(model),
            ...this.renderIssueMarkers(markers, model, context)
        );
    }

    /**
     * Renders a single rounded-border outline rectangle as an HTML `<div>` inside a
     * foreignObject, used as the visual frame of the match node (or one of its
     * shadow copies for `multiple` patterns).
     *
     * @param width  Width of the outline in CSS pixels.
     * @param height Height of the outline in CSS pixels.
     * @returns A VNode representing the styled outline `<div>`.
     */
    private renderOutlineDiv(width: number, height: number): VNode {
        return html("div", {
            class: {
                "bg-background": true,
                "border-foreground": true,
                "border-2": true,
                rounded: true,
                "box-border": true
            },
            style: {
                width: `${width}px`,
                height: `${height}px`
            }
        });
    }

    /**
     * Builds the SVG `<foreignObject>` nodes that form the visible border(s) of the
     * match node.  When `isMultiple` is `true`, a second shadow outline is rendered
     * offset by {@link SHADOW_OFFSET} pixels to indicate a multi-match pattern.
     *
     * @param width      Total width of the node content area in CSS pixels.
     * @param height     Total height of the node content area in CSS pixels.
     * @param isMultiple Whether to render a shadow duplicate outline.
     * @returns An array of SVG foreignObject VNodes (one or two).
     */
    private renderOutlines(width: number, height: number, isMultiple: boolean): VNode[] {
        const SHADOW_OFFSET = 8;

        const divChildren: VNode[] = [];

        if (isMultiple) {
            divChildren.push(
                html(
                    "div",
                    {
                        style: {
                            gridArea: "1 / 1",
                            "margin-left": `${SHADOW_OFFSET}px`,
                            "margin-top": `${SHADOW_OFFSET}px`
                        }
                    },
                    this.renderOutlineDiv(width, height)
                )
            );
        }

        divChildren.push(
            html(
                "div",
                {
                    style: {
                        gridArea: "1 / 1"
                    }
                },
                this.renderOutlineDiv(width, height)
            )
        );

        return [
            svg(
                "foreignObject",
                {
                    attrs: {
                        x: 0,
                        y: 0,
                        width: width + SHADOW_OFFSET,
                        height: height + SHADOW_OFFSET,
                        [ATTR_BBOX_ELEMENT]: true
                    }
                },
                html(
                    "div",
                    {
                        class: {
                            grid: true,
                            "w-fit": true
                        }
                    },
                    ...divChildren
                )
            )
        ];
    }

    /**
     * Renders the container compartment area below the pattern content area.
     *
     * The compartment is wrapped in an absolutely positioned `<foreignObject>` so
     * that its HTML content can overflow outside the node's 0-baseline if its bounds
     * are not yet known.  Returns an empty array when `containerNode` is `undefined`.
     *
     * @param containerNode The compartment model element, or `undefined` when absent.
     * @param context       The current rendering context.
     * @param totalWidth    Full pixel width of the node (for the foreignObject).
     * @param yOffset       Y-coordinate at which the container area begins (= end of pattern area).
     * @returns An array of SVG `<foreignObject>` VNodes (zero or one element).
     */
    private renderContainerArea(
        containerNode: GMatchNodeCompartments | undefined,
        context: RenderingContext,
        totalWidth: number,
        yOffset: number
    ): VNode[] {
        if (!containerNode) {
            return [];
        }

        const rendered = context.renderElement(containerNode);
        if (!rendered) {
            return [];
        }

        const containerBounds = containerNode.bounds;
        const boundsIsValid = containerBounds.width >= 0 && containerBounds.height >= 0;
        const foWidth = boundsIsValid ? totalWidth : 99999;
        const foHeight = boundsIsValid ? containerBounds.height : 99999;

        const foreignObject = svg(
            "foreignObject",
            {
                class: {
                    "pointer-events-none": true,
                    "[&_*]:pointer-events-auto": true
                },
                attrs: {
                    x: 0,
                    y: yOffset,
                    width: foWidth,
                    height: foHeight
                }
            },
            html(
                "div",
                {
                    style: boundsIsValid ? { width: `${totalWidth}px` } : {},
                    class: {
                        "w-fit": !boundsIsValid
                    }
                },
                rendered
            )
        );

        return [foreignObject];
    }
}
