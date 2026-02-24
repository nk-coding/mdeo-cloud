import type { RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeViewBase } from "@mdeo/editor-shared";
import type { GMatchNode } from "../model/matchNode.js";
import type { GMatchNodeCompartments } from "../model/matchNodeCompartments.js";

const { injectable } = sharedImport("inversify");
const { svg, html, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class GMatchNodeView extends GNodeViewBase {
    /**
     * Padding between the pattern content (instances, links) and the match node's outline/container area.
     */
    static readonly INNER_PADDING = 20;

    /**
     * Minimum width/height for the pattern content area, to ensure a reasonable size when no pattern elements are present or when they have zero-size bounds.
     */
    static readonly MIN_CONTENT_SIZE = 80;

    override render(model: Readonly<GMatchNode>, context: RenderingContext): VNode | undefined {
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
            ...this.renderControlElements(model),
            ...outlines,
            innerGroup,
            ...bottomVNodes
        );
    }

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
