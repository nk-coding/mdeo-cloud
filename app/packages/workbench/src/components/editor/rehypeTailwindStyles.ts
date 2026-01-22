import type { Root, Element, ElementContent } from "hast";
import { visit } from "unist-util-visit";

/**
 * Rehype plugin to add Tailwind CSS classes to HTML elements based on shadcn-vue Prose components
 */
export function rehypeAddClasses() {
    return (tree: Root) => {
        visit(tree, "element", (node: Element) => {
            const classes = getClassesForElement(node.tagName);
            if (classes) {
                node.properties = node.properties || {};
                node.properties.className = classes;
            }
        });

        visit(tree, "element", (node: Element, index, parent) => {
            if (node.tagName === "table" && parent && index !== undefined) {
                const wrapper: Element = {
                    type: "element",
                    tagName: "div",
                    properties: {
                        className: "no-scrollbar my-6 w-full overflow-y-auto rounded-lg border"
                    },
                    children: [node]
                };
                parent.children[index] = wrapper as ElementContent;
            }
        });
    };
}

/**
 * Gets Tailwind CSS classes for a given HTML element tag
 * Heavily inspired by the shadcn-vue Prose component styles
 *
 * @param tagName The HTML tag name
 * @returns Tailwind CSS classes as a string or undefined if no classes are defined
 */
function getClassesForElement(tagName: string): string | undefined {
    switch (tagName) {
        case "h1":
            return "font-heading mt-2 scroll-m-28 text-3xl font-bold tracking-tight";
        case "h2":
            return "font-heading [&+*]:![code]:text-xl mt-10 scroll-m-28 text-xl font-medium tracking-tight first:mt-0 lg:mt-16 [&+.steps]:!mt-0 [&+.steps>h3]:!mt-4 [&+h3]:!mt-6 [&+p]:!mt-4";
        case "h3":
            return "font-heading mt-12 scroll-m-28 text-lg font-medium tracking-tight [&+p]:!mt-4 *:[code]:text-xl";
        case "h4":
            return "font-heading mt-8 scroll-m-28 text-base font-medium tracking-tight";
        case "h5":
            return "mt-8 scroll-m-28 text-base font-medium tracking-tight";
        case "h6":
            return "mt-8 scroll-m-28 text-base font-medium tracking-tight";
        case "p":
            return "leading-relaxed [&:not(:first-child)]:mt-6";
        case "a":
            return "font-medium underline underline-offset-4";
        case "ul":
            return "my-6 ml-6 list-disc";
        case "ol":
            return "my-6 ml-6 list-decimal";
        case "li":
            return "mt-2";
        case "blockquote":
            return "mt-6 border-l-2 pl-6 italic";
        case "code":
            return "bg-muted relative rounded-md px-[0.3rem] py-[0.2rem] font-mono text-[0.8rem] outline-none";
        case "pre":
            return "no-scrollbar min-w-0 overflow-x-auto px-4 py-3.5 outline-none";
        case "strong":
            return "font-medium";
        case "em":
            return "italic";
        case "img":
            return "rounded-md";
        case "hr":
            return "my-4 md:my-8";
        case "table":
            return "relative w-full overflow-hidden border-none text-sm [&_tbody_tr:last-child]:border-b-0";
        case "thead":
            return "";
        case "tbody":
            return "";
        case "tr":
            return "last:border-b-none m-0 border-b";
        case "th":
            return "px-4 py-2 text-left font-bold [&[align=center]]:text-center [&[align=right]]:text-right";
        case "td":
            return "px-4 py-2 text-left [&[align=center]]:text-center [&[align=right]]:text-right";
        default:
            return undefined;
    }
}
