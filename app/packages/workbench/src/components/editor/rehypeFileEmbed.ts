import type { Root, Element, ElementContent } from "hast";
import { visit } from "unist-util-visit";

/**
 * Descriptor for a file embedded within markdown content.
 * Produced by the {@link rehypeFileEmbed} plugin during AST traversal.
 */
export interface FileEmbed {
    /**
     * Unique DOM element ID for the placeholder div.
     * Used as the Teleport target in the Vue component.
     */
    id: string;
    /**
     * The raw `src` attribute from the original image node.
     * Relative paths refer to execution result files, absolute paths to regular project files.
     */
    src: string;
    /**
     * The alt text from the original image node, used as a display label.
     */
    alt: string;
}

/**
 * Checks whether a file path has an extension recognized as embeddable.
 *
 * @param src The file path or URL to check
 * @param embeddableExtensions A set of file extensions that are considered embeddable (e.g. `.m_gen`, `.m`)
 * @returns `true` if the file extension is embeddable
 */
function isEmbeddableFile(src: string, embeddableExtensions: Set<string>): boolean {
    const lastDot = src.lastIndexOf(".");
    if (lastDot < 0) {
        return false;
    }
    const ext = src.substring(lastDot);
    return embeddableExtensions.has(ext);
}

/**
 * Rehype plugin that transforms image nodes referencing embeddable model files
 * into placeholder `<div>` elements. The collected embed descriptors are returned
 * via a side-channel callback so the Vue component can mount interactive editors
 * into the placeholders using `<Teleport>`.
 *
 * Markdown images like `![Result Model](result.m_gen)` or `![Source](/files/model.m)`
 * are replaced with `<div id="embed-{n}" data-embed-src="..." data-embed-alt="..."></div>`.
 *
 * @param onEmbed Callback invoked for each discovered embeddable file.
 *                  Receives a {@link FileEmbed} descriptor.
 * @param embeddableExtensions A set of file extensions that should be treated as embeddable.
 * @param idGenerator A function that generates unique IDs for the placeholder divs.
 * @returns A rehype transform function
 */
export function rehypeFileEmbed(
    onEmbed: (embed: FileEmbed) => void,
    embeddableExtensions: Set<string>,
    idGenerator: () => string
) {
    return (tree: Root) => {
        visit(tree, "element", (node: Element, index, parent) => {
            if (node.tagName !== "img" || index == null || parent == null) {
                return;
            }

            const src = String(node.properties?.src ?? "");
            const alt = String(node.properties?.alt ?? "");

            if (src == undefined || !isEmbeddableFile(src, embeddableExtensions)) {
                return;
            }

            const embedId = idGenerator();

            const placeholder: Element = {
                type: "element",
                tagName: "div",
                properties: {
                    id: embedId,
                    "data-embed-src": src,
                    "data-embed-alt": alt
                },
                children: []
            };

            parent.children[index] = placeholder as ElementContent;

            onEmbed({ id: embedId, src, alt });
        });
    };
}
