import type { BindingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import { GGraphView } from "../views/graphView.js";

const { DefaultTypes, configureModelElement } = sharedImport("@eclipse-glsp/sprotty");
const { GHtmlRoot, HtmlRootView, GViewportRootElement, SvgViewportView, GGraph } = sharedImport("@eclipse-glsp/client")

/**
 * Configures the default model elements and their corresponding views.
 * 
 * @param context The binding context used for configuration
 */
export function configureDefaultModelElements(context: BindingContext) {
    configureModelElement(context, DefaultTypes.HTML, GHtmlRoot, HtmlRootView);
    configureModelElement(context, DefaultTypes.SVG, GViewportRootElement, SvgViewportView);
    configureModelElement(context, DefaultTypes.GRAPH, GGraph, GGraphView);

}