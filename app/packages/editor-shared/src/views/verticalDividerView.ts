import type { IView } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { VNode } from "snabbdom";
import type { GVerticalDivider } from "../model/verticalDivider.js";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering vertical divider elements.
 */
@injectable()
export class GVerticalDividerView implements IView {
    render(model: Readonly<GVerticalDivider>): VNode | undefined {
        return html("div", {
            class: {
                ...this.getClasses(model)
            }
        });
    }

    /**
     * Returns the CSS classes to be applied to the vertical divider's main visual element.
     * Subclasses can override this method to customize or extend the default classes.
     *
     * @param _model - The vertical divider model being rendered
     * @returns An object mapping class names to boolean values
     */
    protected getClasses(_model: Readonly<GVerticalDivider>): Record<string, boolean> {
        return {
            "h-full": true,
            "border-r-2": true,
            "border-current": true
        };
    }
}
