import type { RenderingContext, GModelElement, Bounds as BoundsType } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeViewBase, type GNode } from "@mdeo/editor-shared";
import type { GMatchNode } from "../model/matchNode.js";
import { isPatternInstanceNode } from "../model/patternInstanceNode.js";
import { ModelTransformationElementType } from "../model/elementTypes.js";
import { GMatchNodeCompartments } from "../model/matchNodeCompartments.js";

const { injectable } = sharedImport("inversify");
const { svg, html, ATTR_BBOX_ELEMENT, Bounds } = sharedImport("@eclipse-glsp/sprotty");

const INNER_PADDING = 20;

const MIN_CONTENT_SIZE = 80;

@injectable()
export class GMatchNodeView extends GNodeViewBase {
    override render(model: Readonly<GNode>, context: RenderingContext): VNode | undefined {
        const matchModel = model as GMatchNode;

        const patternChildren: GModelElement[] = [];
        const edgeChildren: GModelElement[] = [];
        let containerNode: GModelElement | undefined;

        for (const child of matchModel.children) {
            if (isPatternInstanceNode(child)) {
                patternChildren.push(child);
            } else if (child.type === ModelTransformationElementType.EDGE_PATTERN_LINK) {
                edgeChildren.push(child);
            } else if (child instanceof GMatchNodeCompartments) {
                containerNode = child;
            }
        }

        const patternBbox = this.computePatternBounds(patternChildren, matchModel);

        const patternContentRight = INNER_PADDING + patternBbox.width + INNER_PADDING;
        const patternAreaHeight = INNER_PADDING + patternBbox.height + INNER_PADDING;

        const containerBounds = (containerNode as GMatchNodeCompartments | undefined)?.bounds;
        const containerBoundsValid =
            containerBounds !== undefined && containerBounds.width >= 0 && containerBounds.height >= 0;
        const containerHeight = containerBoundsValid ? containerBounds.height : 0;
        const containerWidth = containerBoundsValid ? containerBounds.width : 0;

        const totalWidth = Math.max(patternContentRight, MIN_CONTENT_SIZE + INNER_PADDING * 2, containerWidth);
        const patternAreaBottomY = patternAreaHeight;
        const totalHeight = patternAreaBottomY + containerHeight;

        const outlines = this.renderOutlines(totalWidth, totalHeight, matchModel.multiple);

        const translateX = INNER_PADDING - patternBbox.x;
        const translateY = INNER_PADDING - patternBbox.y;
        const innerGroup = svg(
            "g",
            { attrs: { transform: `translate(${translateX}, ${translateY})` } },
            ...patternChildren
                .map((child) => context.renderElement(child))
                .filter((vnode): vnode is VNode => vnode !== undefined),
            ...edgeChildren
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

    private computePatternBounds(patternChildren: GModelElement[], _matchModel: GMatchNode): BoundsType {
        if (patternChildren.length === 0) {
            return { x: 0, y: 0, width: MIN_CONTENT_SIZE, height: MIN_CONTENT_SIZE };
        }

        let combined: BoundsType | undefined;
        for (const child of patternChildren) {
            const b = (child as { bounds?: BoundsType }).bounds;
            if (!b) continue;
            combined = combined === undefined ? b : Bounds.combine(combined, b);
        }

        return combined ?? { x: 0, y: 0, width: MIN_CONTENT_SIZE, height: MIN_CONTENT_SIZE };
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
        const SHADOW_OFFSET = 4;

        const divChildren: VNode[] = [];

        if (isMultiple) {
            divChildren.push(
                html("div", {
                    style: {
                        position: "absolute",
                        top: `${SHADOW_OFFSET}px`,
                        left: `${SHADOW_OFFSET}px`
                    }
                }, this.renderOutlineDiv(width, height))
            );
        }

        divChildren.push(
            html("div", {
                style: {
                    position: "absolute",
                    top: "0",
                    left: "0"
                }
            }, this.renderOutlineDiv(width, height))
        );

        return [
            svg(
                "foreignObject",
                {
                    attrs: {
                        x: 0,
                        y: 0,
                        width,
                        height,
                        [ATTR_BBOX_ELEMENT]: true
                    }
                },
                html("div", {
                    style: {
                        position: "relative",
                        width: `${width}px`,
                        height: `${height}px`,
                        overflow: "visible"
                    }
                }, ...divChildren)
            )
        ];
    }

    private renderContainerArea(
        containerNode: GModelElement | undefined,
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

        const containerBounds = (containerNode as GMatchNodeCompartments).bounds;
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
