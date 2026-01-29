import type { DeleteElementOperation, GModelElement } from "@eclipse-glsp/server";
import { BaseDeleteElementOperationHandler, type DeleteOperationResult, sharedImport } from "@mdeo/language-shared";
import { ObjectInstance, Link } from "../../../grammar/modelTypes.js";
import type { PartialObjectInstance, PartialLink } from "../../../grammar/modelPartialTypes.js";
import type { AstNode } from "langium";
import type { WorkspaceEdit } from "vscode-languageserver-types";

const { injectable } = sharedImport("inversify");
const { AstUtils } = sharedImport("langium");

/**
 * Operation handler for deleting elements in the model diagram.
 * Handles deletion of objects and their connected links.
 */
@injectable()
export class ModelDeleteElementOperationHandler extends BaseDeleteElementOperationHandler {
    /**
     * Executes the delete operation.
     *
     * @param operation The delete element operation
     * @returns The delete operation result
     */
    protected override async executeDelete(operation: DeleteElementOperation): Promise<DeleteOperationResult> {
        const elementsToDelete = this.convertIdsToElements(operation.elementIds);
        const allElementsToDelete = this.formTransitiveClosure(elementsToDelete);
        const workspaceEdit = await this.createDeleteEdits(allElementsToDelete);
        const deletedElements = this.collectDeletedGModelElements(allElementsToDelete);

        return { workspaceEdit, deletedElements };
    }

    /**
     * Converts element IDs to their corresponding AST nodes.
     *
     * @param elementIds Array of element IDs to delete
     * @returns Array of AST nodes to delete
     */
    private convertIdsToElements(elementIds: string[]): AstNode[] {
        const elements: AstNode[] = [];
        for (const id of elementIds) {
            const element = this.modelState.index.find(id);
            if (element != undefined) {
                const astNode = this.index.getAstNode(element);
                if (astNode != undefined) {
                    elements.push(astNode);
                }
            }
        }
        return elements;
    }

    /**
     * Forms transitive closure by finding all connected elements that should be deleted.
     * When an object is deleted, all links connected to it are also deleted.
     *
     * @param elements Initial elements to delete
     * @returns All elements to delete including transitively connected ones
     */
    private formTransitiveClosure(elements: AstNode[]): Set<AstNode> {
        const result = new Set<AstNode>(elements);
        const deletedObjectNames = new Set<string>();

        for (const element of elements) {
            if (this.reflection.isInstance(element, ObjectInstance)) {
                const obj = element as PartialObjectInstance;
                if (obj.name != undefined) {
                    deletedObjectNames.add(obj.name);
                }
            }
        }

        const sourceModel = this.modelState.sourceModel;
        if (sourceModel == undefined) {
            return result;
        }

        for (const node of AstUtils.streamAst(sourceModel)) {
            if (this.reflection.isInstance(node, Link)) {
                const link = node as PartialLink;
                const sourceObjectName = link.source?.object?.$refText;
                const targetObjectName = link.target?.object?.$refText;

                if (
                    (sourceObjectName != undefined && deletedObjectNames.has(sourceObjectName)) ||
                    (targetObjectName != undefined && deletedObjectNames.has(targetObjectName))
                ) {
                    result.add(link);
                }
            }
        }

        return result;
    }

    /**
     * Creates a workspace edit to delete all specified elements.
     *
     * @param elements Elements to delete
     * @returns Merged workspace edit for all deletions
     */
    private async createDeleteEdits(elements: Set<AstNode>): Promise<WorkspaceEdit> {
        const edits: WorkspaceEdit[] = [];

        for (const element of elements) {
            if (element.$cstNode != undefined) {
                edits.push(this.deleteCstNode(element.$cstNode));
            }
        }

        return this.mergeWorkspaceEdits(edits);
    }

    /**
     * Collects GModelElements that were deleted.
     *
     * @param elements Set of deleted AST nodes
     * @returns Array of deleted GModelElements
     */
    private collectDeletedGModelElements(elements: Set<AstNode>): GModelElement[] {
        const deletedElements: GModelElement[] = [];

        for (const element of elements) {
            const elementId = this.index.getElementId(element);
            if (elementId != undefined) {
                const gElement = this.modelState.index.find(elementId);
                if (gElement != undefined) {
                    deletedElements.push(gElement);
                }
            }
        }

        return deletedElements;
    }
}
