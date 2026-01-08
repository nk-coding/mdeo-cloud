import type { IView, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GHorizontalDivider } from "../model/horizontalDivider.js";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class GHorizontalDividerView implements IView {
    render(model: Readonly<GHorizontalDivider>, context: RenderingContext, args?: {} | undefined): VNode | undefined {
        return html("div", {
            class: {
                ...this.getClasses(model)
            }
        });
    }

    /**
     * Returns the CSS classes to be applied to the horizontal divider's main visual element.
     * Subclasses can override this method to customize or extend the default classes.
     *
     * @param _model - The horizontal divider model being rendered
     * @returns An object mapping class names to boolean values
     */
    protected getClasses(_model: Readonly<GHorizontalDivider>): Record<string, boolean> {
        return {
            "w-full": true,
            "border-b-2": true,
            "border-current": true
        };
    }
}
