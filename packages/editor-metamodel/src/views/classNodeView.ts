import { type RenderingContext } from "@eclipse-glsp/sprotty";
import { RectangularNodeView, sharedImport } from "@mdeo/editor-shared";
import type { VNode } from "snabbdom";
import type { GClassNode } from "../model/classNode.js";

const { injectable } = sharedImport("inversify");
const { svg } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class ClassNodeView extends RectangularNodeView {
    override render(model: Readonly<GClassNode>, context: RenderingContext): VNode | undefined {
        if (!this.isVisible(model, context)) {
            return undefined;
        }
        return svg(
            "g",
            null,
            this.renderRect(model),
            ...this.renderSelectedRect(model),
            ...context.renderChildren(model)
        );
    }
}
