import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GLabel } from "../model/label.js";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class GLabelView implements IView {
    render(model: Readonly<GLabel>, context: RenderingContext, args?: {}): VNode | undefined {
        return html(
            "span",
            {
                class: {
                    ...this.getClasses(model)
                }
            },
            model.text
        );
    }

    /**
     * Returns the CSS classes to be applied to the label's main visual element.
     * Subclasses can override this method to customize or extend the default classes.
     * By default, no classes are applied.
     *
     * @param _model - The HTML label model being rendered
     * @returns An object mapping class names to boolean values
     */
    protected getClasses(_model: Readonly<GLabel>): Record<string, boolean> {
        return {};
    }
}
