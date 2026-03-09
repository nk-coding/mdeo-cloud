import type { GEdge } from "@mdeo/language-shared";
import {
    BaseReconnectEdgeOperationHandler,
    sharedImport,
    DefaultModelIdRegistry,
    ModelIdProvider as ModelIdProviderKey,
    type ModelIdProvider,
    type ModelIdRegistry,
    type ReconnectEndpoints,
    type ReconnectEdgeResult
} from "@mdeo/language-shared";
import type { ReconnectEdgeOperation } from "@mdeo/editor-protocol";
import type { AstNode, CstNode } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import {
    Association,
    ClassExtension,
    ClassExtensions,
    type AssociationType,
    type ClassExtensionsType,
    type ClassExtensionType,
    type ClassType
} from "../../../grammar/metamodelTypes.js";
import { MetamodelElementType } from "../model/elementTypes.js";

const { injectable, inject } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");

/**
 * Handler for applying reconnect edge operations from the client.
 * Handles both extension edges (inheritance) and association edges.
 */
@injectable()
export class MetamodelReconnectEdgeOperationHandler extends BaseReconnectEdgeOperationHandler {
    @inject(ModelIdProviderKey)
    protected idProvider!: ModelIdProvider;
    override async createReconnectEdit(
        node: AstNode,
        operation: ReconnectEdgeOperation,
        edge: GEdge,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        if (edge.type === MetamodelElementType.EDGE_INHERITANCE) {
            return await this.handleExtensionEdgeReconnect(node, operation, endpoints);
        } else if (edge.type === MetamodelElementType.EDGE_ASSOCIATION) {
            return await this.handleAssociationEdgeReconnect(node, operation, endpoints);
        }
        return undefined;
    }

    /**
     * Handles reconnection of extension (inheritance) edges.
     *
     * @param node The ClassExtension AST node
     * @param operation The reconnect edge operation
     * @param endpoints The old and new source/target nodes
     * @returns The reconnect result with workspace edit and new edge ID
     */
    private async handleExtensionEdgeReconnect(
        node: AstNode,
        operation: ReconnectEdgeOperation,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        if (!this.reflection.isInstance(node, ClassExtension)) {
            return undefined;
        }

        const extension = node as ClassExtensionType;
        const sourceChanged = endpoints.oldSource.id !== endpoints.newSource.id;
        const targetChanged = endpoints.oldTarget.id !== endpoints.newTarget.id;

        if (sourceChanged && targetChanged) {
            return await this.handleExtensionBothChanged(extension, endpoints);
        } else if (sourceChanged) {
            return await this.handleExtensionSourceChanged(extension, endpoints);
        } else if (targetChanged) {
            return await this.handleExtensionTargetChanged(extension, endpoints);
        }

        return undefined;
    }

    /**
     * Handles case where both source and target of extension changed.
     * This happens when reconnecting both ends simultaneously.
     *
     * @param extension The ClassExtension AST node
     * @param endpoints The old and new source/target nodes
     * @returns The reconnect result with workspace edit and new edge ID
     */
    private async handleExtensionBothChanged(
        extension: ClassExtensionType,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        const edits: WorkspaceEdit[] = [];

        const oldSourceNode = this.index.getAstNode(endpoints.oldSource);
        const newSourceNode = this.index.getAstNode(endpoints.newSource);
        const newTargetNode = this.index.getAstNode(endpoints.newTarget);

        if (oldSourceNode == undefined || newSourceNode == undefined || newTargetNode == undefined) {
            return undefined;
        }

        if (
            !this.reflection.isInstance(oldSourceNode, "Class") ||
            !this.reflection.isInstance(newSourceNode, "Class")
        ) {
            return undefined;
        }

        const oldSourceClass = oldSourceNode as ClassType;
        const newSourceClass = newSourceNode as ClassType;

        edits.push(await this.removeExtensionFromClass(oldSourceClass, extension));
        edits.push(await this.addExtensionToClass(newSourceClass, newTargetNode));

        const sourceClassName = this.getNodeName(newSourceNode) ?? "unknown";
        const targetClassName = this.getNodeName(newTargetNode) ?? "unknown";
        const newEdgeId = this.calculateExtensionEdgeId(sourceClassName, targetClassName);

        return {
            newEdgeId,
            workspaceEdit: this.mergeWorkspaceEdits(edits)
        };
    }

    /**
     * Handles case where the source class (the extending class) changed.
     * Removes extension from old class and adds it to new class.
     *
     * @param extension The ClassExtension AST node
     * @param endpoints The old and new source/target nodes
     * @returns The reconnect result with workspace edit and new edge ID
     */
    private async handleExtensionSourceChanged(
        extension: ClassExtensionType,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        const edits: WorkspaceEdit[] = [];

        const oldSourceNode = this.index.getAstNode(endpoints.oldSource);
        const newSourceNode = this.index.getAstNode(endpoints.newSource);
        const targetNode = this.index.getAstNode(endpoints.newTarget);

        if (oldSourceNode == undefined || newSourceNode == undefined || targetNode == undefined) {
            return undefined;
        }

        if (
            !this.reflection.isInstance(oldSourceNode, "Class") ||
            !this.reflection.isInstance(newSourceNode, "Class")
        ) {
            return undefined;
        }

        const oldSourceClass = oldSourceNode as ClassType;
        const newSourceClass = newSourceNode as ClassType;

        edits.push(await this.removeExtensionFromClass(oldSourceClass, extension));
        edits.push(await this.addExtensionToClass(newSourceClass, targetNode));

        const sourceClassName = this.getNodeName(newSourceNode) ?? "unknown";
        const targetClassName = this.getNodeName(targetNode) ?? "unknown";
        const newEdgeId = this.calculateExtensionEdgeId(sourceClassName, targetClassName);

        return {
            newEdgeId,
            workspaceEdit: this.mergeWorkspaceEdits(edits)
        };
    }

    /**
     * Handles case where the target class (the extended class) changed.
     * Replaces the reference in the extends list.
     *
     * @param extension The ClassExtension AST node
     * @param endpoints The old and new source/target nodes
     * @returns The reconnect result with workspace edit and new edge ID
     */
    private async handleExtensionTargetChanged(
        extension: ClassExtensionType,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        const cstNode = extension.$cstNode;
        if (cstNode == undefined) {
            return undefined;
        }

        const newTargetNode = this.index.getAstNode(endpoints.newTarget);
        if (newTargetNode == undefined) {
            return undefined;
        }

        const newTargetName = this.getNodeName(newTargetNode);
        if (newTargetName == undefined) {
            return undefined;
        }

        const classRefNode = GrammarUtils.findNodeForProperty(cstNode, "class");
        if (classRefNode == undefined) {
            return undefined;
        }

        const workspaceEdit = await this.replaceCstNode(classRefNode, newTargetName);
        const sourceNode = this.index.getAstNode(endpoints.newSource);
        const sourceClassName = sourceNode != undefined ? (this.getNodeName(sourceNode) ?? "unknown") : "unknown";
        const newEdgeId = this.calculateExtensionEdgeId(sourceClassName, newTargetName);

        return {
            newEdgeId,
            workspaceEdit
        };
    }

    /**
     * Handles reconnection of association edges.
     *
     * @param node The Association AST node
     * @param operation The reconnect edge operation
     * @param endpoints The old and new source/target nodes
     * @returns The reconnect result with workspace edit and optional new edge ID
     */
    private async handleAssociationEdgeReconnect(
        node: AstNode,
        operation: ReconnectEdgeOperation,
        endpoints: ReconnectEndpoints
    ): Promise<ReconnectEdgeResult | undefined> {
        if (!this.reflection.isInstance(node, Association)) {
            return undefined;
        }

        const association = node as AssociationType;
        const edits: WorkspaceEdit[] = [];

        const sourceChanged = endpoints.oldSource.id !== endpoints.newSource.id;
        const targetChanged = endpoints.oldTarget.id !== endpoints.newTarget.id;

        if (sourceChanged) {
            const sourceEdit = await this.updateAssociationEnd(association.source, endpoints.newSource.id);
            if (sourceEdit == undefined) {
                return undefined;
            }
            edits.push(sourceEdit);
        }

        if (targetChanged) {
            const targetEdit = await this.updateAssociationEnd(association.target, endpoints.newTarget.id);
            if (targetEdit == undefined) {
                return undefined;
            }
            edits.push(targetEdit);
        }

        const sourceModel = this.modelState.sourceModel;
        const registry: ModelIdRegistry | undefined =
            sourceModel != undefined ? new DefaultModelIdRegistry(sourceModel, this.idProvider) : undefined;

        const sourceClassName = this.getClassNameFromRegistry(
            registry,
            sourceChanged ? endpoints.newSource.id : endpoints.oldSource.id
        );
        const targetClassName = this.getClassNameFromRegistry(
            registry,
            targetChanged ? endpoints.newTarget.id : endpoints.oldTarget.id
        );
        const newEdgeId = this.calculateAssociationEdgeId(sourceClassName, targetClassName, association);

        const hasDuplicateId = this.checkDuplicateEdgeId(newEdgeId, operation.edgeElementId);

        return {
            newEdgeId: hasDuplicateId ? undefined : newEdgeId,
            workspaceEdit: this.mergeWorkspaceEdits(edits)
        };
    }

    /**
     * Updates an association end to reference a new class.
     *
     * @param end The association end to update
     * @param newClassId The new class ID
     * @returns The workspace edit to update the association end
     */
    private async updateAssociationEnd(
        end: AstNode | undefined,
        newClassId: string
    ): Promise<WorkspaceEdit | undefined> {
        if (end == undefined || end.$cstNode == undefined) {
            return undefined;
        }

        const newClassElement = this.modelState.index.find(newClassId);
        if (newClassElement == undefined) {
            return undefined;
        }

        const newClassNode = this.index.getAstNode(newClassElement);
        if (newClassNode == undefined) {
            return undefined;
        }

        const newClassName = this.getNodeName(newClassNode);
        if (newClassName == undefined) {
            return undefined;
        }

        const classRefNode = GrammarUtils.findNodeForProperty(end.$cstNode, "class");
        if (classRefNode == undefined) {
            return undefined;
        }

        return await this.replaceCstNode(classRefNode, newClassName);
    }

    /**
     * Removes an extension from a class's extends list.
     *
     * @param cls The class to remove the extension from
     * @param extension The extension to remove
     * @returns The workspace edit to remove the extension
     */
    private async removeExtensionFromClass(cls: ClassType, extension: ClassExtensionType): Promise<WorkspaceEdit> {
        const extensionsNode = cls.extensions;
        if (extensionsNode == undefined || extensionsNode.$cstNode == undefined) {
            throw new Error("ClassExtensions node or CST node not found.");
        }

        const extensions = extensionsNode.extensions ?? [];
        if (extensions.length === 1) {
            return this.deleteCstNode(extensionsNode.$cstNode);
        } else {
            const remainingExtensions = extensions.filter((ext) => ext !== extension);
            const newClassExtensions: ClassExtensionsType = {
                $type: ClassExtensions.name,
                extensions: remainingExtensions.map((ext) => ({
                    $type: ClassExtension.name,
                    class: {
                        $refText: ext.class.$refText,
                        ref: ext.class.ref
                    }
                }))
            };

            return await this.replaceCstNode(extensionsNode.$cstNode, await this.serializeNode(newClassExtensions));
        }
    }

    /**
     * Adds an extension to a class.
     *
     * @param cls The class to add the extension to
     * @param targetNode The target class node to extend
     * @returns The workspace edit to add the extension
     */
    private async addExtensionToClass(cls: ClassType, targetNode: AstNode): Promise<WorkspaceEdit> {
        const targetName = this.getNodeName(targetNode);
        if (targetName == undefined) {
            throw new Error("Target class name not found.");
        }

        const classCstNode = cls.$cstNode;
        if (classCstNode == undefined) {
            throw new Error("Class CST node not found.");
        }

        const existingExtensions = cls.extensions?.extensions ?? [];

        const allExtensions: ClassExtensionType[] = [
            ...existingExtensions.map((ext) => ({
                $type: ClassExtension.name,
                class: {
                    $refText: ext.class.$refText,
                    ref: ext.class.ref
                }
            })),
            {
                $type: ClassExtension.name,
                class: {
                    $refText: targetName,
                    ref: undefined
                }
            }
        ];

        const classExtensions: ClassExtensionsType = {
            $type: ClassExtensions.name,
            extensions: allExtensions
        };

        const serialized = await this.serializeNode(classExtensions);

        if (existingExtensions.length === 0) {
            const nameNode = GrammarUtils.findNodeForProperty(classCstNode, "name");
            if (nameNode == undefined) {
                throw new Error("Class name node not found.");
            }
            return this.createInsertAfterNode(nameNode, ` ${serialized}`);
        } else {
            const extensionsNode = cls.extensions;
            if (extensionsNode?.$cstNode == undefined) {
                throw new Error("ClassExtensions CST node not found.");
            }
            return await this.replaceCstNode(extensionsNode.$cstNode, serialized);
        }
    }

    /**
     * Calculates the ID for an extension edge using the subclass and superclass names.
     * Matches the MetamodelModelIdProvider.getClassExtensionName format:
     * ClassExtension_{subclassName}_{superclassName}
     *
     * @param sourceClassName The name of the extending (sub) class
     * @param targetClassName The name of the extended (super) class
     * @returns The extension edge ID
     */
    private calculateExtensionEdgeId(sourceClassName: string, targetClassName: string): string {
        return `${ClassExtension.name}_${sourceClassName}_${targetClassName}`;
    }

    /**
     * Calculates the ID for an association edge using class names.
     * Matches the MetamodelModelIdProvider.getAssociationName format:
     * Association_{sourceClass}_{sourceProp}_{operator}_{targetClass}_{targetProp}
     *
     * @param sourceClassName The name of the source class
     * @param targetClassName The name of the target class
     * @param association The association AST node
     * @returns The association edge ID
     */
    private calculateAssociationEdgeId(
        sourceClassName: string,
        targetClassName: string,
        association: AssociationType
    ): string {
        const sourceProperty = association.source?.name ?? "";
        const targetProperty = association.target?.name ?? "";
        const operator = association.operator ?? "--";

        return `${Association.name}_${sourceClassName}_${sourceProperty}_${operator}_${targetClassName}_${targetProperty}`;
    }

    /**
     * Looks up the class name for a GModel endpoint by finding its AST node and
     * querying the registry. Falls back to {@link getNodeName} if the registry is
     * not available or returns no name.
     *
     * @param registry The model ID registry (may be undefined)
     * @param endpointId The GModel element ID of the class endpoint
     * @returns The class name, or "unknown" if not resolvable
     */
    private getClassNameFromRegistry(registry: ModelIdRegistry | undefined, endpointId: string): string {
        const element = this.modelState.index.find(endpointId);
        if (element == undefined) return "unknown";
        const node = this.index.getAstNode(element);
        if (node == undefined) return "unknown";
        return registry?.getName(node as AstNode) ?? this.getNodeName(node) ?? "unknown";
    }

    /**
     * Checks if an edge ID already exists in the model (duplicate detection).
     *
     * @param edgeId The edge ID to check
     * @param currentEdgeId The current edge ID being reconnected
     * @returns True if the ID is a duplicate, false otherwise
     */
    private checkDuplicateEdgeId(edgeId: string, currentEdgeId: string): boolean {
        if (edgeId === currentEdgeId) {
            return false;
        }

        const existingEdge = this.modelState.index.find(edgeId);
        return existingEdge != undefined;
    }

    /**
     * Gets the name of a node (Class or ClassImport).
     *
     * @param node The node to get the name from
     * @returns The node name, or undefined if not found
     */
    private getNodeName(node: AstNode): string | undefined {
        if ("name" in node && typeof node.name === "string") {
            return node.name;
        }
        return undefined;
    }

    /**
     * Creates a workspace edit that inserts text after a CST node.
     *
     * @param node The CST node to insert after
     * @param text The text to insert
     * @returns The workspace edit
     */
    private createInsertAfterNode(node: CstNode, text: string): WorkspaceEdit {
        const document = this.modelState.sourceModel?.$document;
        if (document == undefined) {
            throw new Error("Source model document is not available.");
        }

        const pos = document.textDocument.positionAt(node.end);

        return {
            changes: {
                [this.modelState.sourceUri!]: [
                    {
                        range: { start: pos, end: pos },
                        newText: text
                    }
                ]
            }
        };
    }
}
