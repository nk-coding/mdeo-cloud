import type { PasteOperation } from "@eclipse-glsp/protocol";
import type { Point } from "@eclipse-glsp/protocol";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { AstNode, CompositeCstNode } from "langium";
import {
    BasePasteOperationHandler,
    sharedImport,
    type PasteInsertionResult,
    type SerializedClipboardNode,
    type ClipboardEdgeMetadata,
} from "@mdeo/language-shared";
import type { MetadataEdits } from "@mdeo/language-shared";
import {
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    Pattern,
    PatternObjectInstance,
    PatternObjectInstanceReference,
    PatternObjectInstanceDelete,
    PatternLink,
    type PatternType,
    type MatchStatementType,
    type IfMatchStatementType,
    type WhileMatchStatementType,
    type UntilMatchStatementType,
    type ForMatchStatementType
} from "../../../grammar/modelTransformationTypes.js";
import { ModelTransformationModelIdProvider } from "../modelTransformationModelIdProvider.js";
import { ModelTransformationElementType } from "@mdeo/protocol-model-transformation";

const { injectable } = sharedImport("inversify");

/** $type values that represent pattern elements (can be inserted inside a match). */
const PATTERN_ELEMENT_TYPES = new Set([
    PatternObjectInstance.name,
    PatternObjectInstanceReference.name,
    PatternObjectInstanceDelete.name,
    PatternLink.name
]);

/** $type values that represent match-type statements (inserted at file level). */
const MATCH_STATEMENT_TYPES = new Set([
    MatchStatement.name,
    IfMatchStatement.name,
    WhileMatchStatement.name,
    UntilMatchStatement.name,
    ForMatchStatement.name
]);

/**
 * Model-transformation-specific paste operation handler.
 *
 * <p>Handles three paste scenarios:
 * <ol>
 *   <li><b>Match node selected + pattern elements in clipboard</b>: pattern elements
 *       are inserted into the selected match node's pattern block.</li>
 *   <li><b>No selection or only match statements in clipboard</b>: match statements
 *       are appended at the end of the file.</li>
 *   <li><b>Only pattern elements + no match node selected</b>: a new match statement
 *       wrapping all pattern elements is appended at the end of the file.</li>
 * </ol>
 */
@injectable()
export class ModelTransformationPasteOperationHandler extends BasePasteOperationHandler {
    protected override async insertNodes(
        astNodeLikes: Record<string, unknown>[],
        serializedNodes: SerializedClipboardNode[],
        operation: PasteOperation,
        offsetPositions: Map<string, Point>,
        _offsetEdgeData: ClipboardEdgeMetadata[]
    ): Promise<PasteInsertionResult | undefined> {
        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (!rootCstNode) {
            return undefined;
        }

        const patternElements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[] = [];
        const matchStatements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[] = [];

        for (let i = 0; i < serializedNodes.length; i++) {
            const $type = serializedNodes[i].$type;
            if (PATTERN_ELEMENT_TYPES.has($type)) {
                patternElements.push({ nodeLike: astNodeLikes[i], serialized: serializedNodes[i] });
            } else if (MATCH_STATEMENT_TYPES.has($type)) {
                matchStatements.push({ nodeLike: astNodeLikes[i], serialized: serializedNodes[i] });
            }
        }

        const selectedPattern = this.findSelectedPattern(operation.editorContext.selectedElementIds);

        if (selectedPattern && patternElements.length > 0) {
            return this.insertPatternElementsIntoMatch(
                selectedPattern,
                patternElements,
                matchStatements,
                offsetPositions
            );
        }

        if (matchStatements.length > 0 && (patternElements.length === 0 || !selectedPattern)) {
            return this.insertMatchStatementsAtEnd(matchStatements, patternElements, offsetPositions);
        }

        if (patternElements.length > 0 && !selectedPattern) {
            return this.insertPatternElementsAsNewMatch(patternElements, offsetPositions);
        }

        return undefined;
    }

    /**
     * Finds the Pattern AST node for a selected match node from the selection IDs.
     * If the selection contains a single match node (or elements inside one match node),
     * returns that match node's pattern.
     */
    private findSelectedPattern(selectedIds: string[]): PatternType | undefined {
        if (selectedIds.length === 0) {
            return undefined;
        }

        for (const id of selectedIds) {
            const gmodelElement = this.modelState.index.get(id);
            if (!gmodelElement) {
                continue;
            }

            if (gmodelElement.type === ModelTransformationElementType.NODE_MATCH) {
                const astNode = this.index.getAstNode(gmodelElement);
                if (astNode) {
                    const pattern = this.extractPattern(astNode);
                    if (pattern) {
                        return pattern;
                    }
                }
            }

            let parent = gmodelElement.parent;
            while (parent) {
                if (parent.type === ModelTransformationElementType.NODE_MATCH) {
                    const parentAstNode = this.index.getAstNode(parent);
                    if (parentAstNode) {
                        const pattern = this.extractPattern(parentAstNode);
                        if (pattern) {
                            return pattern;
                        }
                    }
                }
                parent = parent.parent;
            }
        }

        return undefined;
    }

    /**
     * Extracts the Pattern from a statement AST node.
     * The AST node may be a MatchStatement, Pattern, or a derived match statement.
     */
    private extractPattern(node: AstNode): PatternType | undefined {
        if (this.reflection.isInstance(node, Pattern)) {
            return node as PatternType;
        }
        if (this.reflection.isInstance(node, MatchStatement)) {
            return (node as MatchStatementType).pattern;
        }
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
        return undefined;
    }

    /**
     * Case 1: Insert pattern elements into a selected match node.
     * Also appends any match statements at the end of the file.
     */
    private async insertPatternElementsIntoMatch(
        pattern: PatternType,
        patternElements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[],
        matchStatements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[],
        offsetPositions: Map<string, Point>
    ): Promise<PasteInsertionResult | undefined> {
        const patternCst = pattern.$cstNode as CompositeCstNode | undefined;
        if (!patternCst) {
            return undefined;
        }
        const content = patternCst.content;
        const openBrace = content[0];
        const closeBrace = content[content.length - 1];
        if (!openBrace || !closeBrace) {
            return undefined;
        }

        const hasContent = (pattern.elements?.length ?? 0) > 0;
        const edits: WorkspaceEdit[] = [];
        const metadataNodes: Record<string, { type: string; meta: object }> = {};

        for (let i = 0; i < patternElements.length; i++) {
            const { nodeLike, serialized } = patternElements[i];
            const serializedText = await this.serializeNode(nodeLike as any);
            const edit = this.insertIntoScope(openBrace, closeBrace, hasContent || i > 0, serializedText);
            edits.push(edit);

            this.collectPatternElementMetadata(nodeLike, serialized, offsetPositions, metadataNodes);
        }

        if (matchStatements.length > 0) {
            const matchEdits = await this.insertMatchStatementsAtEndEdits(
                matchStatements,
                offsetPositions,
                metadataNodes
            );
            edits.push(...matchEdits);
        }

        if (edits.length === 0) {
            return undefined;
        }

        const metadataEdits: MetadataEdits | undefined =
            Object.keys(metadataNodes).length > 0 ? { nodes: metadataNodes } : undefined;

        return {
            workspaceEdit: this.mergeWorkspaceEdits(edits),
            metadataEdits
        };
    }

    /**
     * Case 2: Insert match statements at the end of the file, plus wrap any orphan
     * pattern elements in a new match.
     */
    private async insertMatchStatementsAtEnd(
        matchStatements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[],
        patternElements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[],
        offsetPositions: Map<string, Point>
    ): Promise<PasteInsertionResult | undefined> {
        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (!rootCstNode) {
            return undefined;
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        const edits: WorkspaceEdit[] = [];
        const metadataNodes: Record<string, { type: string; meta: object }> = {};

        for (const { nodeLike } of matchStatements) {
            const serializedText = await this.serializeNode(nodeLike as any);
            const edit = await this.createInsertAfterNodeEdit(
                rootCstNode,
                serializedText,
                !isEmpty || edits.length > 0
            );
            edits.push(edit);
        }

        if (patternElements.length > 0) {
            const wrappedText = await this.buildWrappedMatchText(patternElements);
            const edit = await this.createInsertAfterNodeEdit(rootCstNode, wrappedText, !isEmpty || edits.length > 0);
            edits.push(edit);
        }

        if (edits.length === 0) {
            return undefined;
        }

        const metadataEdits: MetadataEdits | undefined =
            Object.keys(metadataNodes).length > 0 ? { nodes: metadataNodes } : undefined;

        return {
            workspaceEdit: this.mergeWorkspaceEdits(edits),
            metadataEdits
        };
    }

    /**
     * Case 3: Only pattern elements, no match selected → create a new match statement
     * wrapping all elements.
     */
    private async insertPatternElementsAsNewMatch(
        patternElements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[],
        offsetPositions: Map<string, Point>
    ): Promise<PasteInsertionResult | undefined> {
        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (!rootCstNode) {
            return undefined;
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        const wrappedText = await this.buildWrappedMatchText(patternElements);
        const edit = await this.createInsertAfterNodeEdit(rootCstNode, wrappedText, !isEmpty);

        return {
            workspaceEdit: edit,
            metadataEdits: undefined
        };
    }

    /**
     * Builds match statement text that wraps the given pattern elements.
     * Produces: `match {\n  element1\n  element2\n  ...\n}`
     */
    private async buildWrappedMatchText(
        patternElements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[]
    ): Promise<string> {
        const elementTexts: string[] = [];
        for (const { nodeLike } of patternElements) {
            const text = await this.serializeNode(nodeLike as any);
            elementTexts.push(text);
        }

        if (elementTexts.length === 0) {
            return "match {}";
        }

        const indented = elementTexts.map((t) => "    " + t.split("\n").join("\n    ")).join("\n");
        return `match {\n${indented}\n}`;
    }

    /**
     * Helper to create workspace edits for inserting match statements at the end of the file.
     */
    private async insertMatchStatementsAtEndEdits(
        matchStatements: { nodeLike: Record<string, unknown>; serialized: SerializedClipboardNode }[],
        offsetPositions: Map<string, Point>,
        metadataNodes: Record<string, { type: string; meta: object }>
    ): Promise<WorkspaceEdit[]> {
        const rootCstNode = this.modelState.sourceModel?.$cstNode;
        if (!rootCstNode) {
            return [];
        }

        const document = this.modelState.sourceModel?.$document;
        const isEmpty = document?.textDocument.getText().trim().length === 0;

        const edits: WorkspaceEdit[] = [];
        for (const { nodeLike } of matchStatements) {
            const serializedText = await this.serializeNode(nodeLike as any);
            const edit = await this.createInsertAfterNodeEdit(
                rootCstNode,
                serializedText,
                !isEmpty || edits.length > 0
            );
            edits.push(edit);
        }
        return edits;
    }

    /**
     * Collects metadata edits for a pattern element (currently just pattern instances
     * with positions).
     */
    private collectPatternElementMetadata(
        nodeLike: Record<string, unknown>,
        serialized: SerializedClipboardNode,
        offsetPositions: Map<string, Point>,
        metadataNodes: Record<string, { type: string; meta: object }>
    ): void {
        if (serialized.$type === PatternObjectInstance.name) {
            const name = nodeLike.name as string | undefined;
            if (name) {
                const position = offsetPositions.get(name);
                if (position) {
                    const elementId = `${PatternObjectInstance.name}_${ModelTransformationModelIdProvider.escapeIdPart(name)}`;
                    metadataNodes[elementId] = {
                        type: ModelTransformationElementType.NODE_PATTERN_INSTANCE,
                        meta: { position }
                    };
                }
            }
        }
    }
}
