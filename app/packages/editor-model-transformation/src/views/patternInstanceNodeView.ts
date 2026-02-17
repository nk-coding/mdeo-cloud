import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";
import type { GPatternInstanceNode } from "../model/patternInstanceNode.js";
import { PatternModifierKind } from "../model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { svg, ATTR_BBOX_ELEMENT } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Padding inside the pattern instance node
 */
const PADDING = 8;

/**
 * Height for the name/type label
 */
const LABEL_HEIGHT = 20;

/**
 * View for rendering pattern instance node elements in model transformation diagrams.
 * Implements IView directly to render pure SVG elements.
 * The node's border style changes based on the modifier (create/delete/forbid).
 */
@injectable()
export class GPatternInstanceNodeView implements IView {
    /**
     * Renders the pattern instance node as an SVG rect with name/type text.
     *
     * @param model The pattern instance node model being rendered
     * @param context The rendering context
     * @returns The rendered VNode
     */
    render(model: Readonly<GPatternInstanceNode>, context: RenderingContext): VNode {
        const { width, height } = model.bounds;
        const actualWidth = Math.max(width, 80);
        const actualHeight = Math.max(height, 40);

        // Get modifier-specific styling
        const { strokeClass, strokeStyle, strokeWidth } = this.getModifierStyles(model.modifier);

        // Build the border rect
        const borderClasses: Record<string, boolean> = {
            "fill-background": true,
            [strokeClass]: true
        };

        const rectAttrs: Record<string, string | number | boolean> = {
            x: 0,
            y: 0,
            width: actualWidth,
            height: actualHeight,
            rx: 2,
            ry: 2,
            [ATTR_BBOX_ELEMENT]: true
        };

        if (strokeWidth) {
            rectAttrs["stroke-width"] = strokeWidth;
        }

        if (strokeStyle) {
            rectAttrs["stroke-dasharray"] = strokeStyle;
        }

        const borderRect = svg("rect", {
            class: borderClasses,
            attrs: rectAttrs
        });

        // Format the display text: "name : type" or just "name"
        let displayText = model.name ?? "?";
        if (model.typeName) {
            displayText = `${displayText} : ${model.typeName}`;
        }

        // Render name/type label with underline (UML instance convention)
        const nameLabel = svg(
            "text",
            {
                class: {
                    "fill-foreground": true,
                    "text-sm": true,
                    "pointer-events-none": true
                },
                attrs: {
                    x: actualWidth / 2,
                    y: PADDING + LABEL_HEIGHT / 2,
                    "text-anchor": "middle",
                    "dominant-baseline": "middle",
                    "text-decoration": "underline"
                }
            },
            displayText
        );

        // Render children (labels for properties/attributes)
        const childElements = context.renderChildren(model);

        // Position children below the name label
        const childGroup = svg(
            "g",
            {
                attrs: {
                    transform: `translate(${PADDING}, ${PADDING + LABEL_HEIGHT + 4})`
                }
            },
            ...childElements
        );

        // Build root group with selection styling
        const rootClasses: Record<string, boolean> = {
            "cursor-pointer": true
        };

        if (model.selected) {
            rootClasses["[&>rect]:stroke-sky-500"] = true;
            rootClasses["[&>rect]:stroke-2"] = true;
        }

        return svg("g", { class: rootClasses }, borderRect, nameLabel, childGroup);
    }

    /**
     * Gets modifier-specific stroke styling.
     *
     * @param modifier The pattern modifier kind
     * @returns Stroke class, dash style, and width
     */
    private getModifierStyles(modifier: PatternModifierKind): {
        strokeClass: string;
        strokeStyle: string | undefined;
        strokeWidth: number | undefined;
    } {
        switch (modifier) {
            case PatternModifierKind.CREATE:
                // Green double-line style (thicker stroke simulates double)
                return { strokeClass: "stroke-green-600", strokeStyle: undefined, strokeWidth: 3 };
            case PatternModifierKind.DELETE:
                // Red dashed border
                return { strokeClass: "stroke-red-600", strokeStyle: "5,3", strokeWidth: 1.5 };
            case PatternModifierKind.FORBID:
                // Amber dotted border
                return { strokeClass: "stroke-amber-600", strokeStyle: "2,2", strokeWidth: 1.5 };
            default:
                // Standard border
                return { strokeClass: "stroke-foreground", strokeStyle: undefined, strokeWidth: 1.5 };
        }
    }
}
