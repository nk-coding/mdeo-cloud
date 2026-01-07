import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GVerticalDivider } from "../model/verticalDivider.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class VerticalDividerView implements IView {
    render(model: Readonly<GVerticalDivider>, context: RenderingContext, args?: {} | undefined): VNode | undefined {
        return svg("rect", {
            class: {
                "fill-foreground": true
            },
            attrs: {
                width: 2,
                height: Math.max(0, model.size.height),
                x: 0,
                y: 0
            }
        });
    }
}
