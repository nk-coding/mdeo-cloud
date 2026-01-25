import { BaseCreateNodeOperationHandler, sharedImport } from "@mdeo/language-shared";
import type { CreateNodeResult } from "@mdeo/language-shared";
import type {
    CreateNodeOperation,
    TriggerNodeCreationAction as TriggerNodeCreationActionType
} from "@eclipse-glsp/protocol";
import type { CreateNodeOperationHandler } from "@eclipse-glsp/server";
import type { EnumType } from "../../../grammar/metamodelTypes.js";
import { MetamodelElementType } from "../model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { CreateNodeOperation: CreateNodeOperationKind, TriggerNodeCreationAction } =
    sharedImport("@eclipse-glsp/protocol");

/**
 * Operation handler for creating new Enum nodes in the metamodel diagram.
 * Creates enums with an optional default entry.
 */
@injectable()
export class CreateEnumOperationHandler extends BaseCreateNodeOperationHandler implements CreateNodeOperationHandler {
    readonly operationType = CreateNodeOperationKind.KIND;
    override readonly label = "Enum";
    readonly elementTypeIds = [MetamodelElementType.NODE_ENUM];

    /**
     * Creates a new enum node based on the given operation.
     *
     * @param operation The create node operation
     * @returns The result of the node creation including workspace edit
     */
    override async createNode(operation: CreateNodeOperation): Promise<CreateNodeResult | undefined> {
        if (operation.elementTypeId !== MetamodelElementType.NODE_ENUM) {
            return undefined;
        }
        const edit = await this.createEnumNode(operation);
        const nodeId = "Enum_NewEnum";
        return {
            nodeId,
            nodeType: MetamodelElementType.NODE_ENUM,
            workspaceEdit: edit
        };
    }

    /**
     * Returns the trigger actions for creating enum nodes.
     *
     * @returns Array of trigger actions for enum creation
     */
    getTriggerActions(): TriggerNodeCreationActionType[] {
        return [
            TriggerNodeCreationAction.create(MetamodelElementType.NODE_ENUM),
            TriggerNodeCreationAction.create(MetamodelElementType.NODE_ENUM, { args: { includeEntry: true } })
        ];
    }

    /**
     * Creates a workspace edit for adding a new Enum node to the document.
     *
     * @param operation The create operation
     * @returns The workspace edit to perform the insertion
     */
    protected async createEnumNode(operation: CreateNodeOperation) {
        const includeEntry = operation.args?.includeEntry === true;
        const enumNode = this.createEnumAst(includeEntry);
        const serialized = await this.serializeNode(enumNode);
        const rootCstNode = this.modelState.sourceModel?.$cstNode;

        if (rootCstNode == undefined) {
            throw new Error("Root CST node is not available.");
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        return await this.createInsertAfterNodeEdit(rootCstNode, serialized, !isEmpty);
    }

    /**
     * Creates an AST node for a new Enum.
     *
     * @param includeEntry Whether to include a default entry
     * @returns The created Enum AST node
     */
    protected createEnumAst(includeEntry: boolean): EnumType {
        const enumNode: EnumType = {
            $type: "Enum",
            name: "NewEnum",
            entries: []
        };

        if (includeEntry) {
            enumNode.entries.push({
                $type: "EnumEntry",
                name: "VALUE"
            });
        }

        return enumNode;
    }
}
