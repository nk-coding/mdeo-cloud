import type { AstNode } from "langium";
import {
    BaseRequestClipboardDataActionHandler,
    sharedImport,
    AstReflectionKey,
    type ClipboardEdgeMetadata
} from "@mdeo/language-shared";
import type { AstReflection } from "@mdeo/language-common";
import type { EdgeLayoutMetadata } from "@mdeo/protocol-common";
import {
    Class,
    Enum,
    Association,
    ClassExtension,
    ClassExtensions,
    type ClassType,
    type ClassExtensionType,
    type ClassExtensionsType,
    type AssociationType
} from "../../../grammar/metamodelTypes.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Metamodel-specific action handler for {@code RequestClipboardDataAction}.
 *
 * <p>Filters selected AST nodes to the supported top-level types (classes, enums,
 * associations) and transforms class nodes by stripping inheritance extensions
 * whose target class is not also selected.
 */
@injectable()
export class MetamodelRequestClipboardDataActionHandler extends BaseRequestClipboardDataActionHandler {
    /**
     * The AST reflection service for type checking.
     */
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;

    /**
     * Filters selected AST nodes to only include classes, enums, and associations.
     * Classes are transformed so that extensions referencing non-selected classes
     * are removed.
     *
     * @param selectedAstNodes - The raw AST nodes from the selected graph elements.
     * @returns The filtered and transformed top-level nodes for clipboard serialization.
     */
    protected override getTopLevelAstNodes(selectedAstNodes: AstNode[]): AstNode[] {
        const topLevel: AstNode[] = [];
        const selectedClasses = new Set<string>();

        for (const node of selectedAstNodes) {
            if (this.reflection.isInstance(node, Class)) {
                const cls = node as ClassType;
                if (cls.name) {
                    selectedClasses.add(cls.name);
                }
            }
        }

        for (const node of selectedAstNodes) {
            if (this.reflection.isInstance(node, Class)) {
                topLevel.push(this.transformClass(node as ClassType, selectedClasses));
            } else if (this.reflection.isInstance(node, Enum)) {
                topLevel.push(node);
            } else if (this.reflection.isInstance(node, Association)) {
                topLevel.push(node);
            }
        }

        return topLevel;
    }

    /**
     * Collects edge layout metadata for association edges in the selection.
     * For each selected association, reads its routing points and anchor
     * information from the current model metadata and includes them in the
     * clipboard data so they can be restored (with mouse-position offset) on paste.
     *
     * @param selectedAstNodes - The raw selected AST nodes.
     * @returns An array of edge metadata entries, one per association with metadata.
     */
    protected override getClipboardEdgeData(selectedAstNodes: AstNode[]): ClipboardEdgeMetadata[] {
        const edges: ClipboardEdgeMetadata[] = [];
        for (const node of selectedAstNodes) {
            if (!this.reflection.isInstance(node, Association)) {
                continue;
            }
            const assoc = node as AssociationType;
            const elementId = this.index.getElementId(node);
            if (!elementId) {
                continue;
            }
            const edgeMeta = this.modelState.metadata.edges[elementId];
            if (!edgeMeta?.meta) {
                continue;
            }
            const layoutMeta = edgeMeta.meta as EdgeLayoutMetadata | undefined;
            if (!layoutMeta?.routingPoints) {
                continue;
            }
            edges.push({
                sourceClass: assoc.source?.class?.$refText ?? "",
                sourceProperty: assoc.source?.name ?? "",
                targetClass: assoc.target?.class?.$refText ?? "",
                targetProperty: assoc.target?.name ?? "",
                routingPoints: layoutMeta.routingPoints,
                sourceAnchor: layoutMeta.sourceAnchor as { side: string; value: number } | undefined,
                targetAnchor: layoutMeta.targetAnchor as { side: string; value: number } | undefined
            });
        }
        return edges;
    }

    /**
     * Creates a copy of a class node with extensions filtered to only include
     * those whose target class is also being copied.
     *
     * <p>If all extensions reference non-selected classes, the extensions property
     * is set to {@code undefined}. If some remain, a new {@link ClassExtensions}
     * wrapper is created with only the retained extensions.
     *
     * @param cls - The original class node.
     * @param selectedClasses - Names of classes included in the clipboard selection.
     * @returns A shallow copy of the class with filtered extensions.
     */
    private transformClass(cls: ClassType, selectedClasses: Set<string>): ClassType {
        if (!cls.extensions || cls.extensions.extensions.length === 0) {
            return cls;
        }

        const retainedExtensions = cls.extensions.extensions.filter((ext) => {
            const refText = ext.class?.$refText;
            return refText !== undefined && selectedClasses.has(refText);
        });

        if (retainedExtensions.length === cls.extensions.extensions.length) {
            return cls;
        }

        const newExtensions: ClassExtensionsType | undefined =
            retainedExtensions.length > 0
                ? {
                      $type: ClassExtensions.name,
                      extensions: retainedExtensions.map(
                          (ext): ClassExtensionType => ({
                              $type: ClassExtension.name,
                              class: { $refText: ext.class.$refText, ref: ext.class.ref }
                          })
                      )
                  }
                : undefined;

        return {
            ...cls,
            extensions: newExtensions
        };
    }
}
