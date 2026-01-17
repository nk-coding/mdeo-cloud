import type { IView, RenderingContext, ViewerOptions } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GGraph } from "@eclipse-glsp/client";

const { injectable, inject } = sharedImport("inversify");
const { svg, TYPES } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class GGraphView implements IView {
    @inject(TYPES.ViewerOptions) protected options!: ViewerOptions;

    get backgroundPatternId(): string {
        return this.options.baseDiv + "-background-pattern";
    }

    render(model: Readonly<GGraph>, context: RenderingContext): VNode | undefined {
        const transform = `scale(${model.zoom}) translate(${-model.scroll.x},${-model.scroll.y})`;
        return svg(
            "svg",
            {
                ns: "http://www.w3.org/2000/svg",
                class: {
                    "sprotty-graph": true
                }
            },
            svg("defs", null, this.renderBackgroundPattern(model)),
            this.renderBackground(),
            svg(
                "g",
                {
                    attrs: {
                        transform
                    }
                },
                ...context.renderChildren(model)
            )
        );
    }

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

    private renderBackgroundPattern(model: Readonly<GGraph>): VNode | undefined {
        const zoomNormalized = model.zoom / Math.pow(3, Math.floor(Math.log(model.zoom) / Math.log(3)));
        const gridSize = 25 * zoomNormalized;
        return svg(
            "pattern",
            {
                attrs: {
                    id: this.backgroundPatternId,
                    width: gridSize,
                    height: gridSize,
                    x: (-model.scroll.x * model.zoom) % gridSize,
                    y: (-model.scroll.y * model.zoom) % gridSize,
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
