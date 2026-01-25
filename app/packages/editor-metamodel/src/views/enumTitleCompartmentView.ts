import { GCompartmentView, sharedImport } from "@mdeo/editor-shared";
import type { GEnumTitleCompartment } from "../model/enumTitleCompartment.js";
import type { VNode } from "snabbdom";
import type { RenderingContext } from "@eclipse-glsp/sprotty";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class GEnumTitleCompartmentView extends GCompartmentView {
    override render(model: Readonly<GEnumTitleCompartment>, context: RenderingContext): VNode | undefined {
        return html(
            "div",
            {
                class: this.getClasses(model)
            },
            html(
                "span",
                {
                    class: {
                        "text-center": true
                    }
                },
                "«enumeration»"
            ),
            ...context.renderChildren(model)
        );
    }
}
