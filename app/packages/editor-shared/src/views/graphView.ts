import type { IView, RenderingContext, ViewerOptions } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GGraph } from "@eclipse-glsp/client";
import { isIssueMarker } from "../features/decoration/issueMarker.js";

const { injectable, inject } = sharedImport("inversify");
const { svg, TYPES } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class GGraphView implements IView {
    @inject(TYPES.ViewerOptions) protected options!: ViewerOptions;

    get backgroundPatternId(): string {
        return this.options.baseDiv + "-background-pattern";
    }

    render(model: Readonly<GGraph>, context: RenderingContext): VNode | undefined {
        const zoom = model.zoom > 0 && context.targetKind !== "hidden" ? model.zoom : 1;
        const renderChildren = model.zoom > 0 || context.targetKind === "hidden";
        const transform = `scale(${zoom}) translate(${-model.scroll.x},${-model.scroll.y})`;
        const children = renderChildren
            ? [
                  svg("defs", null, this.renderBackgroundPattern(model, zoom)),
                  this.renderBackground(),
                  svg(
                      "g",
                      {
                          attrs: {
                              transform
                          }
                      },
                      ...model.children
                          .filter((c) => !isIssueMarker(c))
                          .flatMap((c) => {
                              const v = context.renderElement(c);
                              return v !== undefined ? [v] : [];
                          })
                  )
              ]
            : [];
        return svg(
            "svg",
            {
                ns: "http://www.w3.org/2000/svg",
                class: {
                    "sprotty-graph": true
                }
            },
            ...children
        );
    }

    /**
     * Renders the background of the graph using a pattern defined in the SVG defs. The pattern creates a grid that scales with the zoom level to maintain a consistent appearance.
     *
     * @returns A VNode representing the SVG rectangle that fills the background with the defined pattern
     */
    private renderBackground(): VNode | undefined {
        return svg("rect", {
            attrs: {
                x: 0,
                y: 0,
                width: "100%",
                height: "100%",
                fill: `url(#${this.backgroundPatternId})`
            }
        });
    }

    /**
     * Renders the background pattern for the graph. The pattern is a grid that scales with the zoom level to maintain a consistent appearance.
     *
     * @param model The graph model containing the scroll and zoom information
     * @param zoom The current zoom level of the graph
     * @returns A VNode representing the SVG pattern definition for the background grid
     */
    private renderBackgroundPattern(model: Readonly<GGraph>, zoom: number): VNode | undefined {
        const zoomNormalized = zoom / Math.pow(3, Math.floor(Math.log(zoom) / Math.log(3)));
        const gridSize = 25 * zoomNormalized;
        return svg(
            "pattern",
            {
                attrs: {
                    id: this.backgroundPatternId,
                    width: gridSize,
                    height: gridSize,
                    x: (-model.scroll.x * zoom) % gridSize,
                    y: (-model.scroll.y * zoom) % gridSize,
                    patternUnits: "userSpaceOnUse"
                }
            },
            svg("circle", {
                class: {
                    "fill-grid": true
                },
                attrs: {
                    cx: gridSize / 2,
                    cy: gridSize / 2,
                    r: 1
                }
            })
        );
    }
}
