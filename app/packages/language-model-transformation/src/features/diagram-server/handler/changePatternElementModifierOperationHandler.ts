import { BaseOperationHandler, OperationHandlerCommand, sharedImport } from "@mdeo/language-shared";
import type { Command, GModelElement } from "@eclipse-glsp/server";
import {
    ChangePatternElementModifierOperation,
    ModelTransformationElementType,
    PatternModifierKind
} from "@mdeo/protocol-model-transformation";
import type { ContextActionRequestContext, ContextItemProvider } from "@mdeo/language-shared";
import type { ContextItem } from "@mdeo/protocol-common";
import {
    PatternObjectInstance,
    PatternLink,
    type PatternObjectInstanceType,
    type PatternLinkType
} from "../../../grammar/modelTransformationTypes.js";
import type { WorkspaceEdit } from "vscode-languageserver-types";

const { injectable } = sharedImport("inversify");
const { GrammarUtils } = sharedImport("langium");
const { TextEdit } = sharedImport("vscode-languageserver-types");

/**
 * Handler for changing create/delete/forbid/require modifiers on pattern nodes and links.
 */
@injectable()
export class ChangePatternElementModifierOperationHandler extends BaseOperationHandler implements ContextItemProvider {
    override readonly operationType = "changePatternElementModifier";

    /**
     * Creates a command to apply a modifier change on the target pattern element.
     * Supports both inserting a new modifier (when none exists) and replacing or removing an existing one.
     * When the target element is a `PatternObjectInstance`, connected `PatternLink` instances that
     * already carry a modifier are updated to the same modifier value.
     *
     * @param operation - The change-modifier operation containing the element ID and the new modifier
     * @returns A command applying the workspace edit, or `undefined` when the element or AST node
     *          cannot be resolved
     */
    override async createCommand(operation: ChangePatternElementModifierOperation): Promise<Command | undefined> {
        const element = this.modelState.index.find(operation.elementId);
        if (element == undefined) {
            return undefined;
        }
        const node = this.index.getAstNode(element);
        if (node == undefined) {
            return undefined;
        }

        let edit: WorkspaceEdit | undefined;
        if (this.reflection.isInstance(node, PatternObjectInstance)) {
            edit = await this.createInstanceModifierEdit(node as PatternObjectInstanceType, operation.modifier);
        } else if (this.reflection.isInstance(node, PatternLink)) {
            edit = await this.createLinkModifierEdit(node as PatternLinkType, operation.modifier);
        }

        if (edit == undefined) {
            return undefined;
        }
        return new OperationHandlerCommand(this.modelState, edit, undefined);
    }

    /**
     * Returns modifier context menu items for locally declared pattern instances and links.
     * References, implicit references, and delete-references are excluded because their modifier
     * is not locally editable.
     *
     * @param element - The selected element
     * @param _context - Additional request context
     * @returns Context actions for this handler, or an empty array if the element is not applicable
     */
    getContextItems(element: GModelElement, _context: ContextActionRequestContext): ContextItem[] {
        if (element.type === ModelTransformationElementType.NODE_PATTERN_INSTANCE) {
            // Only locally declared instances have an editable modifier; references and
            // delete-references map to different AST types.
            const astNode = this.index.getAstNode(element);
            if (astNode == undefined || !this.reflection.isInstance(astNode, PatternObjectInstance)) {
                return [];
            }
        } else if (element.type !== ModelTransformationElementType.EDGE_PATTERN_LINK) {
            return [];
        }

        const modifierOptions = [
            { label: "None", modifier: PatternModifierKind.NONE },
            { label: "Create", modifier: PatternModifierKind.CREATE },
            { label: "Delete", modifier: PatternModifierKind.DELETE },
            { label: "Forbid", modifier: PatternModifierKind.FORBID },
            { label: "Require", modifier: PatternModifierKind.REQUIRE }
        ];

        return [
            {
                id: `change-modifier-${element.id}`,
                label: "Change Modifier",
                icon: "settings-2",
                sortString: "c",
                children: modifierOptions.map((option) => ({
                    id: `change-modifier-${element.id}-${option.modifier}`,
                    label: option.label,
                    action: ChangePatternElementModifierOperation.create({
                        elementId: element.id,
                        modifier: option.modifier
                    })
                }))
            }
        ];
    }

    /**
     * Creates a workspace edit that updates the modifier on a pattern object instance.
     * When the modifier is `NONE`, the existing modifier CST node is deleted.
     * Otherwise the modifier keyword is replaced in-place (if one already exists) or a new keyword
     * is inserted before the instance name (if no modifier is present yet).
     * Connected `PatternLink` elements that already carry a modifier are updated to the same value.
     *
     * @param node - The PatternObjectInstance AST node
     * @param modifier - The new modifier kind to apply
     * @returns The workspace edit, or `undefined` when the change is a no-op or CST nodes cannot
     *          be located
     */
    private async createInstanceModifierEdit(
        node: PatternObjectInstanceType,
        modifier: PatternModifierKind
    ): Promise<WorkspaceEdit | undefined> {
        if (node.$cstNode == undefined) {
            return undefined;
        }

        const edits: WorkspaceEdit[] = [];

        if (modifier === PatternModifierKind.NONE) {
            const instanceModifier = node.modifier;
            if (instanceModifier?.$cstNode == undefined) {
                return undefined;
            }
            edits.push(this.deleteCstNode(instanceModifier.$cstNode));
        } else {
            const modifierText = modifier as string;
            const instanceModifier = node.modifier;

            if (instanceModifier != undefined) {
                const modifierCstNode = GrammarUtils.findNodeForProperty(instanceModifier.$cstNode, "modifier");
                if (modifierCstNode == undefined) {
                    return undefined;
                }
                edits.push(await this.replaceCstNode(modifierCstNode, modifierText));
            } else {
                const nameNode = GrammarUtils.findNodeForProperty(node.$cstNode, "name");
                if (nameNode == undefined) {
                    return undefined;
                }
                const uri = this.getSourceDocument().uri.toString();
                edits.push({ changes: { [uri]: [TextEdit.insert(nameNode.range.start, `${modifierText} `)] } });
            }

            // Propagate to connected links that already carry a modifier.
            const container = node.$container;
            if (container != undefined) {
                const elements = (container as unknown as { elements?: unknown[] }).elements ?? [];
                for (const element of elements) {
                    if (element != undefined && this.reflection.isInstance(element, PatternLink)) {
                        const link = element as PatternLinkType;
                        const sourceRef = link.source?.object?.ref;
                        const targetRef = link.target?.object?.ref;
                        if (
                            (sourceRef === node || targetRef === node) &&
                            link.modifier != undefined &&
                            link.modifier.$cstNode != undefined
                        ) {
                            const linkModCstNode = GrammarUtils.findNodeForProperty(link.modifier.$cstNode, "modifier");
                            if (linkModCstNode != undefined) {
                                edits.push(await this.replaceCstNode(linkModCstNode, modifierText));
                            }
                        }
                    }
                }
            }
        }

        return edits.length > 0 ? this.mergeWorkspaceEdits(edits) : undefined;
    }

    /**
     * Creates a workspace edit that updates the modifier on a pattern link.
     * When the modifier is `NONE`, the existing modifier CST node is deleted.
     * Otherwise the modifier keyword is replaced in-place (if one already exists) or a new keyword
     * is inserted before the link source (if no modifier is present yet).
     *
     * @param node - The PatternLink AST node
     * @param modifier - The new modifier kind to apply
     * @returns The workspace edit, or `undefined` when the change is a no-op or CST nodes cannot
     *          be located
     */
    private async createLinkModifierEdit(
        node: PatternLinkType,
        modifier: PatternModifierKind
    ): Promise<WorkspaceEdit | undefined> {
        if (node.$cstNode == undefined) {
            return undefined;
        }

        if (modifier === PatternModifierKind.NONE) {
            const modifierNode = node.modifier;
            if (modifierNode?.$cstNode == undefined) {
                return undefined;
            }
            return this.deleteCstNode(modifierNode.$cstNode);
        }

        const modifierText = modifier as string;
        const modifierNode = node.modifier;

        if (modifierNode != undefined) {
            const modifierCstNode = GrammarUtils.findNodeForProperty(modifierNode.$cstNode, "modifier");
            if (modifierCstNode == undefined) {
                return undefined;
            }
            return await this.replaceCstNode(modifierCstNode, modifierText);
        } else {
            const sourceNode = GrammarUtils.findNodeForProperty(node.$cstNode, "source");
            if (sourceNode == undefined) {
                return undefined;
            }
            const uri = this.getSourceDocument().uri.toString();
            return { changes: { [uri]: [TextEdit.insert(sourceNode.range.start, `${modifierText} `)] } };
        }
    }
}
