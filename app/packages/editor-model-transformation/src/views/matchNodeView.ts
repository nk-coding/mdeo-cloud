import type { IView, RenderingContext, GModelElement } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport, findViewportZoom } from "@mdeo/editor-shared";
import type { GMatchNode } from "../model/matchNode.js";
import { isPatternInstanceNode } from "../model/patternInstanceNode.js";
import { ModelTransformationElementType } from "../model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Height of the header section containing the match label
 */
const HEADER_HEIGHT = 24;

/**
 * Padding inside the match node for pattern elements
 */
const INNER_PADDING = 8;

/**
 * Minimum width/height for the match node
 */
const MIN_SIZE = 100;

/**
 * Selection outline offset
 */
const SELECTION_OFFSET = 6;

/**
 * View for rendering match nodes in the model transformation diagram.
 * Match nodes are containers that hold pattern elements (instances, links)
 * and constraint compartments (variables, where clauses).
 *
 * The rendering structure:
 * - SVG rect as the border/container
 * - Header area with the match label (e.g., "match", "for match")
 * - Children rendered with their SVG positions
 * - Constraints compartment at the bottom (if present) in a foreignObject
 */
@injectable()
export class GMatchNodeView implements IView {
    /**
     * Renders the match node with its children.
     *
     * @param model The match node model
     * @param context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GMatchNode>, context: RenderingContext): VNode | undefined {
        const { width, height } = model.bounds;
        const actualWidth = Math.max(width, MIN_SIZE);
        const actualHeight = Math.max(height, MIN_SIZE);

        // Separate children into pattern elements and constraints
        const patternChildren: GModelElement[] = [];
        const constraintChildren: GModelElement[] = [];
        const edgeChildren: GModelElement[] = [];

        for (const child of model.children) {
            if (this.isPatternElement(child)) {
                patternChildren.push(child);
            } else if (this.isEdgeElement(child)) {
                edgeChildren.push(child);
            } else if (this.isConstraintElement(child)) {
                constraintChildren.push(child);
            }
        }

        // Calculate constraints height
        const hasConstraints = constraintChildren.length > 0;

        // Render components
        const border = this.renderBorder(actualWidth, actualHeight, model.multiple);
        const header = this.renderHeader(actualWidth, model.label);
        const divider = this.renderHeaderDivider(actualWidth);

        // Render pattern elements group (positioned with transforms)
        const patternElements = this.renderPatternElements(patternChildren, context);
        const patternGroup = svg(
            "g",
            {
                attrs: {
                    transform: `translate(${INNER_PADDING}, ${HEADER_HEIGHT + INNER_PADDING})`
                }
            },
            ...patternElements
        );

        // Render edges
        const edgeElements = edgeChildren
            .map((child) => context.renderElement(child))
            .filter((vnode): vnode is VNode => vnode !== undefined);

        // Render constraints in foreignObject at the bottom (if present)
        const constraintSection = hasConstraints
            ? this.renderConstraints(constraintChildren, actualWidth, actualHeight, context)
            : undefined;

        // Build root group with all elements
        const rootClasses: Record<string, boolean> = {
            "cursor-pointer": true
        };

        // Add selection styling
        const selectionElements: VNode[] = [];
        if (model.selected) {
            const zoom = findViewportZoom(model);
            selectionElements.push(...this.renderSelectionRect(actualWidth, actualHeight, zoom));
            rootClasses["[&>rect:first-child]:stroke-sky-500"] = true;
        }

        const children: VNode[] = [border, header, divider, patternGroup, ...edgeElements, ...selectionElements];

        if (constraintSection) {
            children.push(constraintSection);
        }

        return svg("g", { class: rootClasses }, ...children);
    }

    /**
     * Renders the border rectangle for the match node.
     *
     * @param width The width of the node
     * @param height The height of the node
     * @param isMultiple Whether this is a "for match" (multiple matches)
     * @returns The border VNode
     */
    private renderBorder(width: number, height: number, isMultiple: boolean): VNode {
        const classes: Record<string, boolean> = {
            "fill-background": true,
            "stroke-foreground": true,
            "stroke-[1.5px]": true
        };

        // Use double border for "for match" to indicate iteration
        if (isMultiple) {
            classes["stroke-[3px]"] = true;
        }

        return svg("rect", {
            class: classes,
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
    }

    /**
     * Renders the header text for the match node.
     *
     * @param width The width of the node
     * @param label The label text (e.g., "match", "for match")
     * @returns The header VNode
     */
    private renderHeader(width: number, label: string): VNode {
        return svg(
            "text",
            {
                class: {
                    "fill-foreground": true,
                    "text-sm": true,
                    "font-semibold": true,
                    "pointer-events-none": true
                },
                attrs: {
                    x: width / 2,
                    y: HEADER_HEIGHT / 2 + 4,
                    "text-anchor": "middle",
                    "dominant-baseline": "middle"
                }
            },
            label
        );
    }

    /**
     * Renders a horizontal divider below the header.
     *
     * @param width The width of the node
     * @returns The divider VNode
     */
    private renderHeaderDivider(width: number): VNode {
        return svg("line", {
            class: {
                "stroke-foreground": true,
                "stroke-1": true
            },
            attrs: {
                x1: 0,
                y1: HEADER_HEIGHT,
                x2: width,
                y2: HEADER_HEIGHT
            }
        });
    }

    /**
     * Renders pattern elements (instances) with their positions.
     *
     * @param children The pattern element children
     * @param context The rendering context
     * @returns Array of rendered VNodes
     */
    private renderPatternElements(children: GModelElement[], context: RenderingContext): VNode[] {
        const result: VNode[] = [];

        for (const child of children) {
            // Get the position from the child's meta
            const position = (child as { meta?: { position?: { x: number; y: number } } }).meta?.position ?? {
                x: 0,
                y: 0
            };

            // Render the child
            const renderedChild = context.renderElement(child);

            if (renderedChild) {
                // Wrap in a group with transform for positioning
                const wrapper = svg(
                    "g",
                    {
                        attrs: {
                            transform: `translate(${position.x}, ${position.y})`
                        }
                    },
                    renderedChild
                );
                result.push(wrapper);
            }
        }

        return result;
    }

    /**
     * Renders the constraints compartment (variables and where clauses) in a foreignObject.
     *
     * @param children The constraint element children
     * @param width The width of the node
     * @param height The height of the node
     * @param context The rendering context
     * @returns The foreignObject VNode
     */
    private renderConstraints(
        children: GModelElement[],
        width: number,
        height: number,
        context: RenderingContext
    ): VNode {
        // Render child elements
        const renderedChildren = children.map((child) => context.renderElement(child)).filter(Boolean) as VNode[];

        return svg(
            "foreignObject",
            {
                class: {
                    "pointer-events-none": true,
                    "[&_*]:pointer-events-auto": true
                },
                attrs: {
                    x: 0,
                    y: height - 80, // Position constraints at bottom
                    width,
                    height: 80
                }
            },
            ...renderedChildren
        );
    }

    /**
     * Renders the selection rectangle when the node is selected.
     *
     * @param width The width of the node
     * @param height The height of the node
     * @param zoom The current zoom level
     * @returns Array of selection VNodes
     */
    private renderSelectionRect(width: number, height: number, zoom: number): VNode[] {
        const scaledOffset = SELECTION_OFFSET / zoom;

        return [
            svg("rect", {
                class: {
                    "stroke-primary": true,
                    "stroke-[1.5px]": true,
                    "fill-none": true,
                    "non-scaling-stroke": true,
                    "pointer-events-none": true
                },
                attrs: {
                    x: -scaledOffset,
                    y: -scaledOffset,
                    width: width + scaledOffset * 2,
                    height: height + scaledOffset * 2,
                    rx: 4,
                    ry: 4
                }
            })
        ];
    }

    /**
     * Checks if a child element is a pattern element (instance node).
     *
     * @param child The child element to check
     * @returns True if the element is a pattern instance
     */
    private isPatternElement(child: GModelElement): boolean {
        return isPatternInstanceNode(child);
    }

    /**
     * Checks if a child element is an edge element (pattern link).
     *
     * @param child The child element to check
     * @returns True if the element is an edge
     */
    private isEdgeElement(child: GModelElement): boolean {
        return child.type === ModelTransformationElementType.EDGE_PATTERN_LINK;
    }

    /**
     * Checks if a child element is a constraint element (compartment with variables/where clauses).
     *
     * @param child The child element to check
     * @returns True if the element is a constraint element
     */
    private isConstraintElement(child: GModelElement): boolean {
        return child.type === ModelTransformationElementType.COMPARTMENT;
    }
}
