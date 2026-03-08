import {
    BaseCreateNodeOperationHandler,
    MetadataManager,
    resolveRelativePath,
    sharedImport,
    GCompartment,
    type CreateNodeResult,
    type GroupedToolboxItem,
    type ToolboxItemProvider
} from "@mdeo/language-shared";
import type { CreateNodeOperation, GhostElement, Args } from "@eclipse-glsp/protocol";
import type { AstNode } from "langium";
import {
    PatternObjectInstance,
    PatternObjectInstanceReference,
    PatternObjectInstanceDelete,
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    PatternModifier,
    type PatternObjectInstanceType,
    type PatternObjectInstanceReferenceType,
    type PatternObjectInstanceDeleteType,
    type PatternType,
    type ModelTransformationType,
    type PatternModifierType,
    type MatchStatementType,
    type IfMatchStatementType,
    type WhileMatchStatementType,
    type UntilMatchStatementType,
    type ForMatchStatementType
} from "../../../grammar/modelTransformationTypes.js";
import { ModelTransformationElementType, PatternModifierKind } from "../model/elementTypes.js";
import { ModelTransformationIdGenerator } from "../modelTransformationIdGenerator.js";
import type { ModelTransformationMetadataManager } from "../modelTransformationMetadataManager.js";
import type { ModelTransformationGModelFactory } from "../modelTransformationGModelFactory.js";
import { GPatternInstanceNode } from "../model/patternInstanceNode.js";
import { GPatternInstanceNameLabel } from "../model/patternInstanceNameLabel.js";
import { GPatternModifierTitleCompartment } from "../model/patternModifierTitleCompartment.js";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import { Class, type ClassType, getExportedEntitiesFromMetamodelFile } from "@mdeo/language-metamodel";
import type { MetamodelFileImportType } from "../../../grammar/modelTransformationTypes.js";

const { injectable, inject } = sharedImport("inversify");
const { CreateNodeOperation: CreateNodeOperationKind, TriggerNodeCreationAction } =
    sharedImport("@eclipse-glsp/protocol");
const { GModelFactory } = sharedImport("@eclipse-glsp/server");
const { AstUtils } = sharedImport("langium");

/**
 * The modifier arg value used for persist mode (no modifier keyword in grammar).
 */
const MODIFIER_PERSIST = "persist";

/**
 * Operation handler for creating new pattern object instances in a model transformation diagram.
 * Supports all five creation modes: persist (no modifier), create, delete, require, forbid.
 *
 * For persist and delete modes, also provides "add instance" palette items that insert
 * a reference/delete for a previously declared instance.
 */
@injectable()
export class CreatePatternInstanceOperationHandler
    extends BaseCreateNodeOperationHandler
    implements ToolboxItemProvider
{
    readonly operationType = CreateNodeOperationKind.KIND;
    override readonly label = "Pattern Instance";
    readonly elementTypeIds = [ModelTransformationElementType.NODE_PATTERN_INSTANCE];

    @inject(GModelFactory)
    protected gModelFactory!: ModelTransformationGModelFactory;

    @inject(MetadataManager)
    protected metadataManager!: ModelTransformationMetadataManager;

    /**
     * Creates a new pattern instance in the target match node.
     *
     * @param operation The create node operation, carrying args.className / args.modifier
     *   for new instances, or args.instanceName / args.actionType for add-instance items.
     */
    override async createNode(operation: CreateNodeOperation): Promise<CreateNodeResult | undefined> {
        if (operation.elementTypeId !== ModelTransformationElementType.NODE_PATTERN_INSTANCE) {
            return undefined;
        }

        const containerId = operation.containerId;
        if (!containerId) {
            return undefined;
        }

        const pattern = this.findPatternForContainer(containerId);
        if (!pattern) {
            return undefined;
        }

        const actionType = operation.args?.actionType as string | undefined;
        const instanceName = operation.args?.instanceName as string | undefined;
        const modifier = operation.args?.modifier as string | undefined;

        let workspaceEdit: WorkspaceEdit;
        let nodeId: string;

        if (actionType === "add-instance" && instanceName) {
            if (modifier === "delete") {
                const deleteNode = this.createDeleteAst(instanceName);
                workspaceEdit = await this.insertIntoPattern(pattern, deleteNode);
                nodeId = `${PatternObjectInstanceDelete.name}_${instanceName}`;
            } else {
                const refNode = this.createReferenceAst(instanceName);
                workspaceEdit = await this.insertIntoPattern(pattern, refNode);
                nodeId = `${PatternObjectInstanceReference.name}_${instanceName}`;
            }
        } else {
            const className = operation.args?.className as string | undefined;
            if (!className) {
                return undefined;
            }
            const modifierKind = this.modifierStringToKind(modifier);
            const newInstance = await this.createPatternObjectInstanceAst(className, modifierKind);
            workspaceEdit = await this.insertIntoPattern(pattern, newInstance);
            nodeId = `${PatternObjectInstance.name}_${className}_${newInstance.name}`;
        }

        return {
            nodeId,
            nodeType: ModelTransformationElementType.NODE_PATTERN_INSTANCE,
            workspaceEdit
        };
    }

    /**
     * Provides toolbox items based on the current mode (from args.mode).
     *
     * - All modes: one "new instance" item per non-abstract class.
     * - Persist and delete modes only: one "add instance" item per existing PatternObjectInstance.
     *
     * @param args Args from the toolbox request, including `mode` (NodeCreationMode value).
     */
    async getToolboxItems(args: Args | undefined): Promise<GroupedToolboxItem[]> {
        const mode = (args?.mode as string | undefined) ?? MODIFIER_PERSIST;
        const classes = this.getAvailableNonAbstractClasses();
        const items: GroupedToolboxItem[] = [];

        for (const classInfo of classes) {
            const ghostElement = this.createNewInstanceGhostElement(classInfo.name, this.modifierStringToKind(mode));
            items.push({
                item: {
                    id: `create-pattern-instance-${mode}-${classInfo.name}`,
                    sortString: classInfo.name,
                    label: classInfo.name,
                    actions: [
                        TriggerNodeCreationAction.create(ModelTransformationElementType.NODE_PATTERN_INSTANCE, {
                            args: {
                                className: classInfo.name,
                                modifier: mode
                            },
                            ghostElement
                        })
                    ]
                },
                groupId: "create-group"
            });
        }

        if (mode === MODIFIER_PERSIST || mode === "delete") {
            const existingInstances = this.getAllPatternObjectInstances();
            const isDeleteMode = mode === "delete";
            for (const instance of existingInstances) {
                const name = instance.name;
                if (!name) continue;

                const addModifierKind = isDeleteMode ? PatternModifierKind.DELETE : PatternModifierKind.NONE;
                const ghostElement = this.createAddInstanceGhostElement(name, addModifierKind);
                const label = isDeleteMode ? `delete ${name}` : name;
                items.push({
                    item: {
                        id: `add-instance-${mode}-${name}`,
                        sortString: name,
                        label,
                        actions: [
                            TriggerNodeCreationAction.create(ModelTransformationElementType.NODE_PATTERN_INSTANCE, {
                                args: {
                                    instanceName: name,
                                    modifier: mode,
                                    actionType: "add-instance"
                                },
                                ghostElement
                            })
                        ]
                    },
                    groupId: "add-group"
                });
            }
        }

        return items;
    }

    /**
     * Creates a PatternObjectInstance AST node for a new instance.
     */
    private async createPatternObjectInstanceAst(
        className: string,
        modifier: PatternModifierKind
    ): Promise<PatternObjectInstanceType> {
        const baseName = `new${className}`;
        const name = await this.findUniqueName(baseName);

        const modifierNode: PatternModifierType | undefined =
            modifier !== PatternModifierKind.NONE
                ? {
                      $type: PatternModifier.name,
                      modifier: modifier as string
                  }
                : undefined;

        const instance: PatternObjectInstanceType = {
            $type: PatternObjectInstance.name,
            modifier: modifierNode,
            name,
            class: { $refText: className, ref: undefined },
            properties: []
        };

        return instance;
    }

    /**
     * Creates a PatternObjectInstanceReference AST node referencing an existing instance.
     *
     * @param instanceName The name of the instance to reference
     * @returns A new `PatternObjectInstanceReference` AST node with an unresolved ref text
     */
    private createReferenceAst(instanceName: string): PatternObjectInstanceReferenceType {
        return {
            $type: PatternObjectInstanceReference.name,
            instance: { $refText: instanceName, ref: undefined },
            properties: []
        };
    }

    /**
     * Creates a PatternObjectInstanceDelete AST node for deleting an existing instance.
     *
     * @param instanceName The name of the instance to delete
     * @returns A new `PatternObjectInstanceDelete` AST node with an unresolved ref text
     */
    private createDeleteAst(instanceName: string): PatternObjectInstanceDeleteType {
        return {
            $type: PatternObjectInstanceDelete.name,
            instance: { $refText: instanceName, ref: undefined }
        };
    }

    /**
     * Inserts a new pattern element AST node into the given pattern.
     * If the pattern has existing elements, inserts after the last one.
     * If the pattern is empty, inserts just before the closing brace.
     *
     * @param pattern The target pattern whose element list receives the new node
     * @param element The AST node to serialize and insert
     * @returns A workspace edit that inserts the serialized node into the source document
     * @throws {Error} If the pattern has no CST node and is therefore not backed by source text
     */
    private async insertIntoPattern(pattern: PatternType, element: AstNode): Promise<WorkspaceEdit> {
        const document = this.getSourceDocument();
        const serialized = await this.serializeNode(element);

        if (pattern.elements && pattern.elements.length > 0) {
            const lastElement = pattern.elements[pattern.elements.length - 1];
            const lastCst = lastElement.$cstNode;
            if (lastCst) {
                return await this.workspaceEditService.createInsertAfterNodeEdit(lastCst, serialized, document, true);
            }
        }

        const patternCst = pattern.$cstNode;
        if (!patternCst) {
            throw new Error("Pattern has no CST node; cannot insert element.");
        }

        const endPos = patternCst.range.end;
        const insertPos = {
            line: endPos.line,
            character: Math.max(0, endPos.character - 1)
        };

        const uri = document.uri.toString();
        return {
            changes: {
                [uri]: [
                    {
                        range: { start: insertPos, end: insertPos },
                        newText: `\n  ${serialized}\n`
                    }
                ]
            }
        };
    }

    /**
     * Finds the Pattern AST node for the match node identified by containerId.
     *
     * The containerId is the ID of the GMatchNode as generated by the GModel factory:
     * - Simple MatchStatement: id = statement index ("0", "1", …)
     * - Derived match nodes (IfMatch, WhileMatch, …): id = "${stmtId}_match"
     *
     * @param containerId The GModel ID of the target match node
     * @returns The matching `PatternType` AST node, or `undefined` if not found
     */
    private findPatternForContainer(containerId: string): PatternType | undefined {
        const sourceModel = this.modelState.sourceModel as ModelTransformationType;
        if (!sourceModel) {
            return undefined;
        }

        for (const node of AstUtils.streamAllContents(sourceModel)) {
            const pattern = this.tryGetPatternFromNode(node, containerId);
            if (pattern !== undefined) {
                return pattern;
            }
        }

        return undefined;
    }

    /**
     * Tries to extract the Pattern from a given AST node if the node is a
     * match-type statement whose generated match node ID equals containerId.
     *
     * @param node The AST node to inspect
     * @param containerId The GModel ID being searched for
     * @returns The `PatternType` if the node is a matching statement, otherwise `undefined`
     */
    private tryGetPatternFromNode(node: AstNode, containerId: string): PatternType | undefined {
        const elementId = this.index.getElementId(node);

        if (this.reflection.isInstance(node, MatchStatement) && elementId !== undefined) {
            if (elementId === containerId) {
                return (node as MatchStatementType).pattern;
            }
        }

        if (elementId !== undefined) {
            const derivedMatchId = ModelTransformationIdGenerator.matchNode(elementId);
            if (derivedMatchId === containerId) {
                if (this.reflection.isInstance(node, IfMatchStatement)) {
                    return (node as IfMatchStatementType).ifBlock?.pattern;
                }
                if (this.reflection.isInstance(node, WhileMatchStatement)) {
                    return (node as WhileMatchStatementType).pattern;
                }
                if (this.reflection.isInstance(node, UntilMatchStatement)) {
                    return (node as UntilMatchStatementType).pattern;
                }
                if (this.reflection.isInstance(node, ForMatchStatement)) {
                    return (node as ForMatchStatementType).pattern;
                }
            }
        }

        return undefined;
    }

    /**
     * Returns all non-abstract classes exported by the imported metamodel.
     * Reads the metamodel file resolved from the transformation's import statement.
     *
     * @returns An array of `{ name, classType }` entries for each concrete exported class,
     *   or an empty array when the import or its document is unavailable
     */
    private getAvailableNonAbstractClasses(): Array<{ name: string; classType: ClassType }> {
        const sourceModel = this.modelState.sourceModel as ModelTransformationType;
        const sourceModelDoc = sourceModel?.$document;
        const importedFile = (sourceModel?.import as MetamodelFileImportType | undefined)?.file;

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
            if (!this.reflection.isInstance(classType, Class)) {
                continue;
            }
            if (!(classType as ClassType).isAbstract) {
                classes.push({ name: (classType as ClassType).name ?? "Unknown", classType: classType as ClassType });
            }
        }

        return classes;
    }

    /**
     * Returns all PatternObjectInstances declared anywhere in the transformation.
     * Used to populate "add instance" toolbox items.
     *
     * @returns All `PatternObjectInstance` AST nodes found in the entire source model,
     *   or an empty array when the source model is unavailable
     */
    private getAllPatternObjectInstances(): PatternObjectInstanceType[] {
        const sourceModel = this.modelState.sourceModel as ModelTransformationType;
        if (!sourceModel) return [];

        const instances: PatternObjectInstanceType[] = [];
        for (const node of AstUtils.streamAllContents(sourceModel)) {
            if (this.reflection.isInstance(node, PatternObjectInstance)) {
                instances.push(node as PatternObjectInstanceType);
            }
        }
        return instances;
    }

    /**
     * Creates a ghost element for dragging a new instance into a match node.
     *
     * @param className The class name used to derive the default instance name (`new<ClassName>`)
     * @param modifier The modifier kind applied to the new instance
     * @returns A `GhostElement` wrapping a preview `GPatternInstanceNode`
     */
    private createNewInstanceGhostElement(className: string, modifier: PatternModifierKind): GhostElement {
        const nodeId = "__ghost_pattern_instance";
        const instanceName = `new${className}`;
        return this.buildGhostElement(nodeId, instanceName, className, modifier);
    }

    /**
     * Creates a ghost element for dragging an "add instance" reference/delete into a match node.
     *
     * @param instanceName The name of the existing instance to reference or delete
     * @param modifier The modifier kind (NONE for persist/reference, DELETE for delete mode)
     * @returns A `GhostElement` wrapping a preview `GPatternInstanceNode` without a type label
     */
    private createAddInstanceGhostElement(instanceName: string, modifier: PatternModifierKind): GhostElement {
        const nodeId = "__ghost_add_instance";
        return this.buildGhostElement(nodeId, instanceName, undefined, modifier);
    }

    /**
     * Builds a `GPatternInstanceNode` ghost element suitable for drag-and-drop preview.
     * If `modifier` is not NONE, a modifier compartment is used as the header;
     * otherwise a standard header compartment is used.
     *
     * @param nodeId A stable placeholder ID for the ghost node (e.g. `__ghost_pattern_instance`)
     * @param name The instance name shown in the label
     * @param typeName The optional class/type name appended after ` : ` in the label
     * @param modifier The modifier kind that determines the visual appearance of the ghost
     * @returns A dynamic `GhostElement` whose template is the constructed pattern instance node
     */
    private buildGhostElement(
        nodeId: string,
        name: string,
        typeName: string | undefined,
        modifier: PatternModifierKind
    ): GhostElement {
        const defaultMeta = this.metadataManager.getDefaultMetadata({
            type: ModelTransformationElementType.NODE_PATTERN_INSTANCE
        });
        const node = GPatternInstanceNode.builder().id(nodeId).name(name).modifier(modifier).meta(defaultMeta).build();

        if (typeName !== undefined) {
            node.typeName = typeName;
        }

        if (modifier !== PatternModifierKind.NONE) {
            const modifierCompartment = GPatternModifierTitleCompartment.builder()
                .id(`${nodeId}#modifier-title`)
                .build();
            const labelText = typeName !== undefined ? `${name} : ${typeName}` : name;
            const label = GPatternInstanceNameLabel.builder()
                .id(`${nodeId}#name`)
                .text(labelText)
                .readonly(true)
                .build();
            modifierCompartment.children.push(label);
            node.children.push(modifierCompartment);
        } else {
            const compartment = GCompartment.builder()
                .type(ModelTransformationElementType.COMPARTMENT)
                .id(`${nodeId}#header`)
                .build();
            const labelText = typeName !== undefined ? `${name} : ${typeName}` : name;
            const label = GPatternInstanceNameLabel.builder()
                .id(`${nodeId}#name`)
                .text(labelText)
                .readonly(true)
                .build();
            compartment.children.push(label);
            node.children.push(compartment);
        }

        return {
            template: node,
            dynamic: true
        };
    }

    /**
     * Converts a modifier string (as passed in args) to a `PatternModifierKind` enum value.
     * Unknown or missing modifier strings default to `PatternModifierKind.NONE`.
     *
     * @param modifier The raw modifier string from operation args (e.g. `"create"`, `"delete"`)
     * @returns The corresponding `PatternModifierKind` value
     */
    private modifierStringToKind(modifier: string | undefined): PatternModifierKind {
        switch (modifier) {
            case "create":
                return PatternModifierKind.CREATE;
            case "delete":
                return PatternModifierKind.DELETE;
            case "require":
                return PatternModifierKind.REQUIRE;
            case "forbid":
                return PatternModifierKind.FORBID;
            default:
                return PatternModifierKind.NONE;
        }
    }
}
