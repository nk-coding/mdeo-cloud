import type { GNode, CreateEdgeResult } from "@mdeo/language-shared";
import { sharedImport } from "@mdeo/language-shared";
import type { CreateEdgeOperation } from "@mdeo/editor-protocol";
import {
    ClassExtension,
    ClassExtensions,
    type ClassType,
    type ClassExtensionType,
    type ClassExtensionsType
} from "../../../grammar/metamodelTypes.js";
import { MetamodelElementType } from "../model/elementTypes.js";
import { MetamodelBaseCreateEdgeOperationHandler } from "./metamodelBaseCreateEdgeOperationHandler.js";
import { GClassNode } from "../model/classNode.js";

const { injectable } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");

/**
 * Handles create-edge operations that produce inheritance (extends) edges in the metamodel diagram.
 * Adds a class extension entry to the source class, extending the target class.
 */
@injectable()
export class CreateInheritanceOperationHandler extends MetamodelBaseCreateEdgeOperationHandler {
    override readonly operationType = "createEdge";
    override label = "Create inheritance edge";
    readonly elementTypeIds = [MetamodelElementType.EDGE_INHERITANCE];

    protected override async createEdge(
        operation: CreateEdgeOperation,
        sourceElement: GNode,
        targetElement: GNode
    ): Promise<CreateEdgeResult | undefined> {
        const sourceClass = this.resolveClass(sourceElement);
        if (!sourceClass) {
            return undefined;
        }

        if (!(targetElement instanceof GClassNode) || !targetElement.name) {
            return undefined;
        }

        return this.createExtensionEdge(sourceClass, targetElement.name);
    }

    /**
     * Creates an inheritance edge by adding a class extension to the source class.
     *
     * @param sourceClass The source class AST node
     * @param targetClass The target class AST node
     * @returns A CreateEdgeResult with the workspace edit to apply, or undefined on failure
     */
    private async createExtensionEdge(
        sourceClass: ClassType,
        targetClassName: string
    ): Promise<CreateEdgeResult | undefined> {
        const classCstNode = sourceClass.$cstNode;
        if (!classCstNode) {
            return undefined;
        }

        const existingExtensions = sourceClass.extensions?.extensions ?? [];

        const allExtensions: ClassExtensionType[] = [
            ...existingExtensions.map((ext) => ({
                $type: ClassExtension.name,
                class: { $refText: ext.class.$refText, ref: ext.class.ref }
            })),
            {
                $type: ClassExtension.name,
                class: { $refText: targetClassName, ref: undefined }
            }
        ];

        const classExtensions: ClassExtensionsType = {
            $type: ClassExtensions.name,
            extensions: allExtensions
        };

        const serialized = await this.serializeNode(classExtensions);

        let workspaceEdit;
        if (existingExtensions.length === 0) {
            const nameNode = GrammarUtils.findNodeForProperty(classCstNode, "name");
            if (!nameNode) {
                return undefined;
            }
            workspaceEdit = this.insertAfterCstNode(nameNode, ` ${serialized}`);
        } else {
            const extensionsNode = sourceClass.extensions;
            if (!extensionsNode?.$cstNode) {
                return undefined;
            }
            workspaceEdit = await this.replaceCstNode(extensionsNode.$cstNode, serialized);
        }

        const edgeId = `${ClassExtension.name}_${sourceClass.name}_${targetClassName}`;
        return {
            edgeId,
            edgeType: MetamodelElementType.EDGE_INHERITANCE,
            workspaceEdit
        };
    }
}
