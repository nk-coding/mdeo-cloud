import type { RenderingContext, GModelElement, Bounds as BoundsType } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, GNodeViewBase, GCompartment, type GNode } from "@mdeo/editor-shared";
import type { GMatchNode } from "../model/matchNode.js";
import { isPatternInstanceNode } from "../model/patternInstanceNode.js";
import { ModelTransformationElementType } from "../model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { svg, html, ATTR_BBOX_ELEMENT, Bounds } = sharedImport("@eclipse-glsp/sprotty");

const INNER_PADDING = 8;

const MIN_CONTENT_SIZE = 80;

@injectable()
export class GMatchNodeView extends GNodeViewBase {
    override render(model: Readonly<GNode>, context: RenderingContext): VNode | undefined {
        const matchModel = model as GMatchNode;

        // Separate children into pattern instance nodes, pattern link edges and compartments
        const patternChildren: GModelElement[] = [];
        const edgeChildren: GModelElement[] = [];
        const compartmentChildren: GModelElement[] = [];

        for (const child of matchModel.children) {
            if (isPatternInstanceNode(child)) {
                patternChildren.push(child);
            } else if (child.type === ModelTransformationElementType.EDGE_PATTERN_LINK) {
                edgeChildren.push(child);
            } else if (child instanceof GCompartment) {
                compartmentChildren.push(child);
            }
        }

        const patternBbox = this.computePatternBounds(patternChildren, matchModel);

        const patternContentRight = INNER_PADDING + patternBbox.width + INNER_PADDING;
        const patternAreaHeight = INNER_PADDING + patternBbox.height + INNER_PADDING;

        const compartmentHeights = compartmentChildren.map(
            (comp) => (comp as { bounds?: { height: number } }).bounds?.height ?? 60
        );
        const DIVIDER_HEIGHT = 1;
        const compartmentBlockHeight =
            compartmentHeights.length > 0
                ? compartmentHeights.reduce((sum, h) => sum + h, 0) + (compartmentHeights.length - 1) * DIVIDER_HEIGHT
                : 0;

        const hasCompartments = compartmentChildren.length > 0;
        const compartmentSeparatorHeight = hasCompartments ? DIVIDER_HEIGHT : 0;

        const totalWidth = Math.max(patternContentRight, MIN_CONTENT_SIZE + INNER_PADDING * 2);
        const patternAreaBottomY = patternAreaHeight;
        const totalHeight = patternAreaBottomY + compartmentSeparatorHeight + compartmentBlockHeight;

        const outline = this.renderOutline(totalWidth, totalHeight, matchModel.multiple);

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

        const compartmentVNodes = this.renderCompartments(
            compartmentChildren,
            compartmentHeights,
            context,
            totalWidth,
            patternAreaBottomY
        );

        return svg(
            "g",
            { class: { "cursor-pointer": true } },
            ...this.renderControlElements(model),
            outline,
            innerGroup,
            ...compartmentVNodes
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

    private renderOutline(width: number, height: number, isMultiple: boolean): VNode {
        const borderClasses: Record<string, boolean> = {
            "fill-background": true,
            "stroke-foreground": true,
            "stroke-[1.5px]": true
        };
        if (isMultiple) {
            borderClasses["stroke-[3px]"] = true;
        }

        const border = svg("rect", {
            class: borderClasses,
            attrs: {
                x: 0,
                y: 0,
                width,
                height,
                rx: 4,
                ry: 4,
                [ATTR_BBOX_ELEMENT]: true
            }
        });

        return svg("g", null, border);
    }

    private renderCompartments(
        children: GModelElement[],
        heights: number[],
        context: RenderingContext,
        totalWidth: number,
        yOffset: number
    ): VNode[] {
        if (children.length === 0) {
            return [];
        }

        const DIVIDER_HEIGHT = 1;

        const topSeparator = svg("line", {
            class: { "stroke-foreground": true, "stroke-1": true },
            attrs: { x1: 0, y1: yOffset, x2: totalWidth, y2: yOffset }
        });

        const htmlChildren: VNode[] = [];
        const dividerVNodes: VNode[] = [];

        let currentY = 0;
        for (let i = 0; i < children.length; i++) {
            if (i > 0) {
                dividerVNodes.push(
                    svg("line", {
                        class: { "stroke-foreground": true, "stroke-1": true },
                        attrs: {
                            x1: 0,
                            y1: yOffset + currentY,
                            x2: totalWidth,
                            y2: yOffset + currentY
                        }
                    })
                );
                currentY += DIVIDER_HEIGHT;
            }
            const rendered = context.renderElement(children[i]);
            if (rendered) {
                htmlChildren.push(html("div", {}, rendered));
            }
            currentY += heights[i] ?? 60;
        }

        const totalBlockHeight = currentY;

        const foreignObject = svg(
            "foreignObject",
            {
                class: { "pointer-events-none": true, "[&_*]:pointer-events-auto": true },
                attrs: { x: 0, y: yOffset + DIVIDER_HEIGHT, width: totalWidth, height: totalBlockHeight }
            },
            html("div", { style: { width: `${totalWidth}px` } }, ...htmlChildren)
        );

        return [topSeparator, ...dividerVNodes, foreignObject];
    }
}
