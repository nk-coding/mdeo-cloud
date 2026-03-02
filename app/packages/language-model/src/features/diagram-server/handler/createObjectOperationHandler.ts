import {
    BaseCreateNodeOperationHandler,
    MetadataManager,
    PlaceholderModelIdRegistry,
    resolveRelativePath,
    sharedImport,
    type CreateNodeResult,
    type GroupedToolboxItem,
    type ToolboxItemProvider
} from "@mdeo/language-shared";
import type { CreateNodeOperation, GhostElement } from "@eclipse-glsp/protocol";
import {
    EnumValue,
    ListValue,
    ObjectInstance,
    PropertyAssignment,
    SimpleValue,
    type EnumValueType,
    type ListValueType,
    type ObjectInstanceType,
    type PropertyAssignmentType,
    type SimpleValueType
} from "../../../grammar/modelTypes.js";
import { ModelElementType } from "../model/elementTypes.js";
import type { ModelGModelFactory } from "../modelGModelFactory.js";
import type { ModelMetadataManager } from "../modelMetadataManager.js";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { PartialModel } from "../../../grammar/modelPartialTypes.js";
import {
    Class,
    type ClassType,
    type PropertyType,
    PrimitiveType,
    MetamodelPrimitiveTypes,
    EnumTypeReference,
    Enum,
    isMultipleMultiplicity,
    isOptionalMultiplicity,
    getExportedEntitiesFromMetamodelFile
} from "@mdeo/language-metamodel";
import type { AstReflection } from "@mdeo/language-common";

const { injectable, inject } = sharedImport("inversify");
const { CreateNodeOperation: CreateNodeOperationKind, TriggerNodeCreationAction } =
    sharedImport("@eclipse-glsp/protocol");
const { GModelFactory } = sharedImport("@eclipse-glsp/server");

/**
 * Operation handler for creating new object instances in the model diagram.
 * Gets available non-abstract classes from the metamodel and provides
 * one palette item per class type.
 *
 * This handler also implements ToolboxItemProvider to supply palette items
 * with ghost element templates for visual preview during node creation.
 */
@injectable()
export class CreateObjectOperationHandler extends BaseCreateNodeOperationHandler implements ToolboxItemProvider {
    readonly operationType = CreateNodeOperationKind.KIND;
    override readonly label = "Object";
    readonly elementTypeIds = [ModelElementType.NODE_OBJECT];

    @inject(GModelFactory)
    protected gModelFactory!: ModelGModelFactory;

    @inject(MetadataManager)
    protected metadataManager!: ModelMetadataManager;

    /**
     * Creates a new object node based on the operation.
     *
     * @param operation The create node operation
     * @returns The create node result or undefined
     */
    override async createNode(operation: CreateNodeOperation): Promise<CreateNodeResult | undefined> {
        if (operation.elementTypeId === ModelElementType.NODE_OBJECT) {
            const className = operation.args?.className as string | undefined;
            const classes = await this.getAvailableNonAbstractClasses();
            const classType = classes.find((cls) => cls.name === className)?.classType;
            const node = await this.createObjectAst(className, classType);
            const edit = await this.createObjectNode(node);
            const typeName = node.class?.$refText ?? "Unknown";
            const nodeId = `${ObjectInstance.name}_${typeName}_${node.name}`;

            return {
                nodeId,
                nodeType: ModelElementType.NODE_OBJECT,
                workspaceEdit: edit
            };
        }
        return undefined;
    }

    /**
     * Implements ToolboxItemProvider to supply grouped palette items.
     * Provides one item per non-abstract class from the metamodel.
     *
     * @returns Array of grouped toolbox items with ghost elements
     */
    async getToolboxItems(): Promise<GroupedToolboxItem[]> {
        const classes = this.getAvailableNonAbstractClasses();
        const items: GroupedToolboxItem[] = [];

        for (const classInfo of classes) {
            items.push({
                item: {
                    id: `create-object-${classInfo.name}`,
                    sortString: classInfo.name,
                    label: classInfo.name,
                    actions: [
                        TriggerNodeCreationAction.create(ModelElementType.NODE_OBJECT, {
                            args: { className: classInfo.name },
                            ghostElement: await this.createGhostElement(classInfo.name, classInfo.classType)
                        })
                    ]
                },
                groupId: "create-group"
            });
        }

        return items;
    }

    /**
     * Gets available non-abstract classes from the imported metamodel.
     * Uses the new simplified import system that transitively collects all classes
     * from the imported metamodel file and its imports.
     *
     * @returns Array of objects with class names and ClassType for object creation
     */
    private getAvailableNonAbstractClasses(): Array<{ name: string; classType: ClassType }> {
        const sourceModel = this.modelState.sourceModel as PartialModel;
        const sourceModelDoc = sourceModel?.$document;
        const importedFile = sourceModel?.import?.file;

        if (importedFile == undefined || sourceModelDoc == undefined) {
            return [];
        }

        const metamodelUri = resolveRelativePath(sourceModelDoc, importedFile);
        const documents = this.modelState.languageServices.shared.workspace.LangiumDocuments;
        const doc = documents.getDocument(metamodelUri);

        if (doc == undefined) {
            return [];
        }

        const exports = getExportedEntitiesFromMetamodelFile(doc, documents);

        const classes: Array<{ name: string; classType: ClassType }> = [];
        for (const classType of exports.classes) {
            if (!classType.isAbstract) {
                classes.push({ name: classType.name ?? "Unknown", classType });
            }
        }

        return classes;
    }

    /**
     * Creates a ghost element for visual preview during node creation.
     *
     * @param className The class name for the object
     * @param classType The ClassType for the object
     * @returns Ghost element configuration with template schema
     */
    protected async createGhostElement(className: string, classType?: ClassType): Promise<GhostElement> {
        const objectAst = await this.createObjectAst(className, classType);
        const idRegistry = new PlaceholderModelIdRegistry("__ghost_object");
        const template = this.gModelFactory.createObjectNode(
            objectAst,
            idRegistry.getId(objectAst),
            this.metadataManager.getDefaultMetadata({ type: ModelElementType.NODE_OBJECT }),
            idRegistry
        );
        return {
            template,
            dynamic: true
        };
    }

    /**
     * Creates a workspace edit for adding a new object instance to the document.
     *
     * @param node The ObjectInstance AST node to insert
     * @returns The workspace edit to perform the insertion
     */
    protected async createObjectNode(node: ObjectInstanceType): Promise<WorkspaceEdit> {
        const serialized = await this.serializeNode(node);

        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (rootCstNode == undefined) {
            throw new Error("Root CST node is not available.");
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        return await this.createInsertAfterNodeEdit(rootCstNode, serialized, !isEmpty);
    }

    /**
     * Creates an AST node for a new ObjectInstance.
     *
     * @param className The class name for the object type
     * @param classType The ClassType for the object type
     * @returns The created ObjectInstance AST node
     */
    protected async createObjectAst(className?: string, classType?: ClassType): Promise<ObjectInstanceType> {
        const typeName = className ?? "Object";
        const baseName = `new${typeName}`;
        const name = await this.findUniqueName(baseName);

        const properties = classType ? this.createDefaultProperties(classType) : [];

        const objectNode: ObjectInstanceType = {
            $type: ObjectInstance.name,
            name,
            class: { $refText: typeName, ref: undefined },
            properties
        };
        return objectNode;
    }

    /**
     * Creates default properties for a class based on its property definitions.
     * Sets default values based on property type and multiplicity:
     * - For multiplicity > 1: empty list ([])
     * - For numbers: 0
     * - For booleans: false
     * - For strings: empty string ("")
     * - For 0..1 (optional): not included
     *
     * @param classType The ClassType to create properties for
     * @returns Array of PropertyAssignment AST nodes with default values
     */
    private createDefaultProperties(classType: ClassType): PropertyAssignmentType[] {
        const reflection = this.modelState.languageServices.shared.AstReflection;
        const properties: PropertyAssignmentType[] = [];

        const allProperties = this.getAllClassProperties(classType, reflection);

        for (const prop of allProperties) {
            const multiplicity = prop.multiplicity;
            const isMultiple = isMultipleMultiplicity(multiplicity, reflection);
            const isOptional = isOptionalMultiplicity(multiplicity, reflection);

            if (isOptional) {
                continue;
            }

            if (isMultiple) {
                const value: ListValueType = {
                    $type: ListValue.name,
                    values: []
                };
                properties.push({
                    $type: PropertyAssignment.name,
                    name: { $refText: prop.name ?? "", ref: prop },
                    value
                });
                continue;
            }

            const defaultValue = this.getDefaultValueForType(prop);
            if (defaultValue) {
                properties.push({
                    $type: PropertyAssignment.name,
                    name: { $refText: prop.name ?? "", ref: prop },
                    value: defaultValue
                });
            }
        }

        return properties;
    }

    /**
     * Gets all properties from a class including inherited properties.
     *
     * @param classType The ClassType to get properties from
     * @param reflection The AST reflection service
     * @returns Array of all properties
     */
    private getAllClassProperties(classType: ClassType, reflection: AstReflection): PropertyType[] {
        const properties: PropertyType[] = [];
        const visited = new Set<ClassType>();

        const collectProperties = (cls: ClassType) => {
            if (visited.has(cls)) {
                return;
            }
            visited.add(cls);

            properties.push(...(cls.properties ?? []));

            for (const extension of cls.extensions?.extensions ?? []) {
                const parentClass = extension.class?.ref;
                if (parentClass && reflection.isInstance(parentClass, Class)) {
                    collectProperties(parentClass as ClassType);
                }
            }
        };

        collectProperties(classType);
        return properties;
    }

    /**
     * Gets the default value for a property based on its type.
     *
     * @param property The property to get the default value for
     * @returns The default value AST node
     */
    private getDefaultValueForType(property: PropertyType): SimpleValueType | EnumValueType | undefined {
        const type = property.type;

        if (this.reflection.isInstance(type, PrimitiveType)) {
            const typeName = type.name;

            switch (typeName) {
                case MetamodelPrimitiveTypes.INT:
                case MetamodelPrimitiveTypes.LONG:
                case MetamodelPrimitiveTypes.DOUBLE:
                case MetamodelPrimitiveTypes.FLOAT:
                    return {
                        $type: SimpleValue.name,
                        numberValue: 0,
                        stringValue: undefined,
                        booleanValue: undefined
                    };
                case MetamodelPrimitiveTypes.BOOLEAN:
                    return {
                        $type: SimpleValue.name,
                        booleanValue: false,
                        numberValue: undefined,
                        stringValue: undefined
                    };
                case MetamodelPrimitiveTypes.STRING:
                    return {
                        $type: SimpleValue.name,
                        stringValue: "",
                        numberValue: undefined,
                        booleanValue: undefined
                    };
            }
        } else if (this.reflection.isInstance(type, EnumTypeReference)) {
            const enumType = type.enum?.ref;
            if (!this.reflection.isInstance(enumType, Enum)) {
                return undefined;
            }
            const enumEntry = enumType.entries?.[0]?.name;
            if (enumEntry == undefined) {
                return undefined;
            }
            return {
                $type: EnumValue.name,
                enumRef: { $refText: enumType.name, ref: undefined },
                value: { $refText: enumEntry, ref: undefined }
            };
        }

        return undefined;
    }
}
