import type { GModelElement, RenderingContext } from "@eclipse-glsp/sprotty";
import type { VNode } from "snabbdom";
import { sharedImport } from "@mdeo/editor-shared";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Checks whether a start or end node lists declarations (its inputs or the outputs), i.e. has
 * the compartment of declaration rows, including one that is being added.
 *
 * @param children The node's children
 * @returns `true` when the node is drawn as a box with rows
 */
export function hasDeclarationRows(children: readonly GModelElement[]): boolean {
    return children.some(
        (child) =>
            child.type === ModelTransformationElementType.COMPARTMENT &&
            (child as { children?: readonly GModelElement[] }).children?.some(
                (row) => row.type === ModelTransformationElementType.LABEL_DECLARATION
            ) === true
    );
}

/**
 * Renders a start or end node that lists declarations: a box with rounded corners holding the
 * node's symbol at the top and, below a rule, one row per declaration.
 *
 * @param symbol The node's symbol (the start dot, the end bullseye)
 * @param context The rendering context
 * @param children The node's children (the compartment of rows)
 * @returns The box
 */
export function renderDeclarationBox(
    symbol: VNode,
    context: RenderingContext,
    children: readonly GModelElement[]
): VNode {
    const content = children
        .map((child) => context.renderElement(child))
        .filter((vnode): vnode is VNode => vnode !== undefined);
    return html(
        "div",
        {
            class: {
                "border-2": true,
                "border-foreground": true,
                "bg-background": true,
                "box-border": true,
                flex: true,
                "flex-col": true,
                "items-center": true,
                "cursor-pointer": true
            },
            style: { borderRadius: "6px", minWidth: "80px" }
        },
        html("div", { style: { padding: "6px 0 4px 0", display: "flex", justifyContent: "center" } }, symbol),
        html(
            "div",
            {
                class: { "border-t-2": true, "border-foreground": true, "self-stretch": true },
                style: { display: "flex", flexDirection: "column" }
            },
            ...content
        )
    );
}
