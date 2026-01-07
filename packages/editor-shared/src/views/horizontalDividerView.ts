import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GHorizontalDivider } from "../model/horizontalDivider.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class HorizontalDividerView implements IView {
    render(model: Readonly<GHorizontalDivider>, context: RenderingContext, args?: {} | undefined): VNode | undefined {
        return svg("rect", {
            class: {
                "fill-foreground": true
            },
            attrs: {
                width: Math.max(0, model.size.width),
                height: 2,
                x: 0,
                y: 0
            }
        });
    }
}
