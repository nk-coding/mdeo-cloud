import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { GRectangularNodeView, sharedImport } from "@mdeo/editor-shared";
import type { GRectangularNode } from "@mdeo/editor-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

const { injectable } = sharedImport("inversify");
const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for the header box of a transformation: a box with rounded corners holding the title and
 * the sections of imports and declarations the server sends. When the transformation declares
 * no inputs or outputs, the box says so below the title.
 */
@injectable()
export class GHeaderNodeView extends GRectangularNodeView {
    protected override getClasses(_model: Readonly<GRectangularNode>): Record<string, boolean> {
        return {
            "border-2": true,
            "border-foreground": true,
            "rounded-md": true,
            "bg-background": true,
            "box-border": true,
            flex: true,
            "flex-col": true,
            "w-fit": true,
            "min-w-40": true,
            "cursor-pointer": true
        };
    }

    protected override renderNodeContent(
        model: Readonly<GRectangularNode>,
        context: RenderingContext,
        children: readonly GModelElement[]
    ): VNode[] {
        const content = children
            .map((child) => context.renderElement(child))
            .filter((vnode): vnode is VNode => vnode !== undefined);
        if (!this.hasDeclarations(model)) {
            content.push(
                html(
                    "div",
                    {
                        class: {
                            "border-t-2": true,
                            "border-foreground": true,
                            "px-2": true,
                            "py-1.5": true,
                            "text-sm": true,
                            italic: true,
                            "text-muted-foreground": true
                        }
                    },
                    "no inputs or outputs"
                )
            );
        }
        return content;
    }

    /**
     * Checks whether the box shows a declaration, including one that is being added.
     *
     * @param element The header box or one of its descendants
     * @returns `true` when there is a declaration row
     */
    private hasDeclarations(element: Readonly<GModelElement>): boolean {
        if (element.type === ModelTransformationElementType.LABEL_HEADER_DECLARATION) {
            return true;
        }
        const children = (element as { children?: readonly GModelElement[] }).children ?? [];
        return children.some((child) => this.hasDeclarations(child));
    }
}
