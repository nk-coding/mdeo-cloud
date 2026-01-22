import { BaseCreateNodeOperationHandler, sharedImport } from "@mdeo/language-shared";
import type { CreateNodeResult } from "@mdeo/language-shared";
import type {
    CreateNodeOperation,
    TriggerNodeCreationAction as TriggerNodeCreationActionType
} from "@eclipse-glsp/protocol";
import type { CreateNodeOperationHandler } from "@eclipse-glsp/server";
import type { ClassType } from "../../../grammar/metamodelTypes.js";
import { MetamodelPrimitiveTypes } from "../../../grammar/metamodelTypes.js";
import { MetamodelElementType } from "../model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { CreateNodeOperation: CreateNodeOperationKind, TriggerNodeCreationAction } =
    sharedImport("@eclipse-glsp/protocol");

/**
 * Operation handler for creating new nodes in the metamodel diagram.
 * Currently supports creating Class nodes.
 */
@injectable()
export class CreateClassOperationHandler extends BaseCreateNodeOperationHandler implements CreateNodeOperationHandler {
    readonly operationType = CreateNodeOperationKind.KIND;
    override readonly label = "Class";
    readonly elementTypeIds = [MetamodelElementType.NODE_CLASS];

    override async createNode(operation: CreateNodeOperation): Promise<CreateNodeResult | undefined> {
        if (operation.elementTypeId === "node:class") {
            const edit = await this.createClassNode(operation);
            const nodeId = "Class_NewClass";
            return {
                nodeId,
                nodeType: MetamodelElementType.NODE_CLASS,
                workspaceEdit: edit
            };
        }
        return undefined;
    }

    getTriggerActions(): TriggerNodeCreationActionType[] {
        return [
            TriggerNodeCreationAction.create(MetamodelElementType.NODE_CLASS),
            TriggerNodeCreationAction.create(MetamodelElementType.NODE_CLASS, { args: { includeProperty: true } })
        ];
    }

    /**
     * Creates a workspace edit for adding a new Class node to the document.
     *
     * @param operation The create operation
     * @returns The workspace edit to perform the insertion
     */
    protected async createClassNode(operation: CreateNodeOperation) {
        const includeProperty = operation.args?.includeProperty === true;

        const classNode = this.createClassAst(includeProperty);

        const serialized = await this.serializeNode(classNode);

        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (rootCstNode == undefined) {
            throw new Error("Root CST node is not available.");
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        return await this.createInsertAfterNodeEdit(rootCstNode, serialized, !isEmpty);
    }

    /**
     * Creates an AST node for a new Class.
     *
     * @param includeProperty Whether to include an example property
     * @returns The created Class AST node
     */
    protected createClassAst(includeProperty: boolean): ClassType {
        const classNode: ClassType = {
            $type: "Class",
            name: "NewClass",
            isAbstract: false,
            properties: [],
            extensions: undefined
        };

        if (includeProperty) {
            classNode.properties.push({
                $type: "Property",
                name: "example",
                type: {
                    $type: "PrimitiveType",
                    name: MetamodelPrimitiveTypes.STRING
                },
                multiplicity: undefined
            });
        }

        return classNode;
    }
}
