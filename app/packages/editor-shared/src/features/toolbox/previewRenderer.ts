import type { GhostElement, IModelFactory, ModelRenderer, ModelRendererFactory } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import type { VNode } from "snabbdom";
import { HiddenBoundsUpdater } from "../bounds/hiddenBoundsUpdater.js";

const { inject, injectable } = sharedImport("inversify");
const { TYPES } = sharedImport("@eclipse-glsp/client");
const { html, isBoundsAware, GModelRoot } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Interface for rendering previews of ghost elements in the toolbox.
 */
export interface PreviewRenderer {
    /**
     * Renders a preview VNode for the given ghost element.
     *
     * @param element The ghost element to render
     */
    renderPreview(element: GhostElement): VNode | undefined;
}

/**
 * Symbol for the PreviewRenderer interface for dependency injection.
 */
export const PreviewRenderer = Symbol("PreviewRenderer");

/**
 * Default implementation of the PreviewRenderer interface.
 * Makes assumptions that
 * - the preview can be rendered without the full model (root) context
 * - the rendered result can be directly displayed inside an svg element
 * - the dimensions of the preview can be directly determined fro´m the rendered element bounds
 * - neither client nor server layout is required for rendering
 * For more complex scenarios, a custom PreviewRenderer should be implemented.
 */
@injectable()
export class DefaultPreviewRenderer implements PreviewRenderer {
    /**
     * The model factory, used to render previews.
     */
    @inject(TYPES.IModelFactory) private readonly modelFactory!: IModelFactory;

    /**
     * The bounds updater, used to compute element bounds.
     */
    @inject(HiddenBoundsUpdater) private readonly boundsUpdater!: HiddenBoundsUpdater;

    /**
     * The model renderer for rendering previews.
     */
    protected readonly renderer: ModelRenderer;

    /**
     * Creates a new DefaultPreviewRenderer instance.
     *
     * @param modelRendererFactory The model renderer factory
     */
    constructor(@inject(TYPES.ModelRendererFactory) modelRendererFactory: ModelRendererFactory) {
        this.renderer = modelRendererFactory("popup", []);
    }

    renderPreview(element: GhostElement): VNode | undefined {
        const template = element.template;
        if (typeof template === "string") {
            return undefined;
        }
        const modelElement = this.modelFactory.createElement(template);
        // @ts-expect-error every element needs a root, so we just set a virtual one here
        modelElement.parent = new GModelRoot();
        const vnode = this.renderer.renderElement(modelElement);
        if (vnode == undefined || !isBoundsAware(modelElement)) {
            return undefined;
        }

        const setBounds = (node: VNode) => {
            const element = node.elm?.firstChild;
            if (element == undefined) {
                return;
            }
            (node.elm as SVGSVGElement).removeAttribute("viewBox");
            const bounds = this.boundsUpdater.getBounds(element, modelElement);
            (node.elm as SVGSVGElement).setAttribute(
                "viewBox",
                `${bounds.x} ${bounds.y} ${bounds.width} ${bounds.height}`
            );
        };

        return html(
            "svg",
            {
                hook: {
                    insert: (node) => setBounds(node),
                    postpatch: (_old, node) => setBounds(node)
                }
            },
            vnode
        );
    }
}
