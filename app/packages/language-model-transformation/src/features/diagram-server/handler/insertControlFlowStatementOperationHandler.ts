import { BaseOperationHandler, OperationHandlerCommand, sharedImport } from "@mdeo/language-shared";
import type { Command, GModelElement } from "@eclipse-glsp/server";
import {
    InsertControlFlowStatementOperation,
    ModelTransformationElementType
} from "@mdeo/protocol-model-transformation";
import type { InsertControlFlowStatementKind } from "@mdeo/protocol-model-transformation";
import { EdgeAttachmentPosition, type ContextItem } from "@mdeo/protocol-common";
import type { ContextActionRequestContext, ContextItemProvider, GEdge } from "@mdeo/language-shared";
import type { WorkspaceEdit } from "vscode-languageserver-types";
import type { CstNode, AstNode } from "langium";
import {
    MatchStatement,
    IfMatchStatement,
    ForMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    IfExpressionStatement,
    WhileExpressionStatement,
    StopStatement,
    Pattern,
    IfMatchConditionAndBlock,
    ModelTransformation,
    StatementsScope,
    type BaseTransformationStatementType,
    type IfMatchStatementType,
    type IfMatchConditionAndBlockType,
    type PatternType,
    type ModelTransformationType
} from "../../../grammar/modelTransformationTypes.js";

const { injectable } = sharedImport("inversify");
const { Range } = sharedImport("vscode-languageserver-types");
const { AstUtils } = sharedImport("langium");

/**
 * Handler for inserting control-flow statements from edge context actions.
 */
@injectable()
export class InsertControlFlowStatementOperationHandler extends BaseOperationHandler implements ContextItemProvider {
    override readonly operationType = "insertControlFlowStatement";

    /**
     * Creates a command that inserts a new control-flow statement on the selected edge.
     *
     * The insertion position is determined from {@link sourceNodeId} and {@link targetNodeId}
     * using the following priority order:
     * 1. If the target node maps to a transformation statement, insert before it.
     * 2. Otherwise, if the source node maps to a statement, insert after it.
     * 3. If the source is the graph start node, insert after the import statement.
     * 4. If the source is a merge node, find the containing structural statement and insert after it.
     *
     * @param operation The insert-control-flow-statement operation
     * @returns A command wrapping the workspace edit, or `undefined` when the insertion
     *          position cannot be resolved
     */
    override async createCommand(operation: InsertControlFlowStatementOperation): Promise<Command | undefined> {
        const { sourceNodeId, targetNodeId, statementKind } = operation;

        if (sourceNodeId == undefined || targetNodeId == undefined) {
            return undefined;
        }

        const sourceModel = this.modelState.sourceModel as ModelTransformationType | undefined;
        if (sourceModel == undefined) {
            return undefined;
        }

        const newText = this.buildStatementText(statementKind);

        const targetStmt = this.findOuterStatementForNodeId(targetNodeId);
        if (targetStmt?.$cstNode != undefined) {
            const prevCst = this.findInsertAfterCstForStatement(targetStmt);
            if (prevCst != undefined) {
                const edit = await this.createInsertAfterNodeEdit(prevCst, newText);
                return new OperationHandlerCommand(this.modelState, edit, undefined);
            }
            const edit = this.createInsertBeforeStatementEdit(targetStmt.$cstNode, newText);
            return new OperationHandlerCommand(this.modelState, edit, undefined);
        }

        const sourceStmt = this.findOuterStatementForNodeId(sourceNodeId);
        if (sourceStmt?.$cstNode != undefined) {
            const edit = await this.createInsertAfterNodeEdit(sourceStmt.$cstNode, newText);
            return new OperationHandlerCommand(this.modelState, edit, undefined);
        }

        if (sourceNodeId === "start") {
            const importCst = sourceModel.import?.$cstNode;
            if (importCst != undefined) {
                const edit = await this.createInsertAfterNodeEdit(importCst, newText);
                return new OperationHandlerCommand(this.modelState, edit, undefined);
            }
        }

        const stmtIdFromMerge = this.extractStmtIdFromMergeNode(sourceNodeId);
        if (stmtIdFromMerge != undefined) {
            const containingStmt = this.findStatementById(stmtIdFromMerge, sourceModel);
            if (containingStmt?.$cstNode != undefined) {
                const edit = await this.createInsertAfterNodeEdit(containingStmt.$cstNode, newText);
                return new OperationHandlerCommand(this.modelState, edit, undefined);
            }
        }

        return undefined;
    }

    /**
     * Returns insertion context items for control-flow edges.
     *
     * @param element The selected element
     * @param _context Additional request context
     * @returns Context actions for this handler
     */
    getContextItems(element: GModelElement, _context: ContextActionRequestContext): ContextItem[] {
        if (element.type !== ModelTransformationElementType.EDGE_CONTROL_FLOW) {
            return [];
        }

        const statementKinds: { label: string; kind: InsertControlFlowStatementKind }[] = [
            { label: "Match", kind: "match" },
            { label: "For Match", kind: "for-match" },
            { label: "If Match", kind: "if-match" },
            { label: "While Match", kind: "while-match" },
            { label: "Until Match", kind: "until-match" },
            { label: "If", kind: "if" },
            { label: "While", kind: "while" },
            { label: "Stop", kind: "stop" },
            { label: "Kill", kind: "kill" }
        ];

        const edge = element as GEdge;

        return [
            {
                id: `insert-control-flow-${element.id}`,
                label: "Insert Statement",
                icon: "between-horizontal-start",
                sortString: "a",
                position: EdgeAttachmentPosition.MIDDLE,
                children: statementKinds.map((statementKind) => ({
                    id: `insert-control-flow-${element.id}-${statementKind.kind}`,
                    label: statementKind.label,
                    action: InsertControlFlowStatementOperation.create({
                        edgeId: element.id,
                        statementKind: statementKind.kind,
                        sourceNodeId: edge.sourceId,
                        targetNodeId: edge.targetId
                    })
                }))
            }
        ];
    }

    /**
     * Returns the placeholder text for a new statement of the given kind.
     * Multi-line texts use `\n` as line separator; continuation lines carry no
     * extra leading indentation (the workspace-edit utilities add the correct
     * indentation when inserting the text).
     *
     * @param kind The statement kind to generate text for
     * @returns The placeholder source text
     */
    private buildStatementText(kind: InsertControlFlowStatementKind): string {
        switch (kind) {
            case "match":
                return "match {}";
            case "for-match":
                return "for match {} do {}";
            case "if-match":
                return "if match {} then {\n} else {\n}";
            case "while-match":
                return "while match {} do {}";
            case "until-match":
                return "until match {} do {}";
            case "if":
                return "if (true) {\n} else {\n}";
            case "while":
                return "while (false) {}";
            case "stop":
                return "stop";
            case "kill":
                return "kill";
        }
    }

    /**
     * Tries to map a CFG node ID to its enclosing transformation statement.
     * Returns `undefined` for synthetic nodes (start, end, split, merge) that
     * do not correspond directly to an AST statement.
     *
     * @param nodeId The CFG node ID
     * @returns The enclosing transformation statement, or `undefined`
     */
    private findOuterStatementForNodeId(nodeId: string): BaseTransformationStatementType | undefined {
        const element = this.modelState.index.find(nodeId);
        if (element == undefined) {
            return undefined;
        }
        const astNode = this.index.getAstNode(element);
        if (astNode == undefined) {
            return undefined;
        }
        return this.toOuterStatement(astNode);
    }

    /**
     * Resolves an AST node to its enclosing transformation statement.
     *
     * Handles two cases:
     * - The node is itself a statement (e.g. `MatchStatement`, `IfExpressionStatement`).
     * - The node is a `Pattern` embedded inside an if-match / for-match / while-match /
     *   until-match statement; the method then ascends to the containing statement.
     *
     * @param astNode An AST node returned by {@link GModelIndex.getAstNode}
     * @returns The enclosing transformation statement, or `undefined`
     */
    private toOuterStatement(astNode: AstNode): BaseTransformationStatementType | undefined {
        if (
            this.reflection.isInstance(astNode, MatchStatement) ||
            this.reflection.isInstance(astNode, IfMatchStatement) ||
            this.reflection.isInstance(astNode, ForMatchStatement) ||
            this.reflection.isInstance(astNode, WhileMatchStatement) ||
            this.reflection.isInstance(astNode, UntilMatchStatement) ||
            this.reflection.isInstance(astNode, IfExpressionStatement) ||
            this.reflection.isInstance(astNode, WhileExpressionStatement) ||
            this.reflection.isInstance(astNode, StopStatement)
        ) {
            return astNode as BaseTransformationStatementType;
        }

        if (this.reflection.isInstance(astNode, Pattern)) {
            const patternNode = astNode as PatternType;
            const container = patternNode.$container;
            if (this.reflection.isInstance(container, IfMatchConditionAndBlock)) {
                return (container as IfMatchConditionAndBlockType).$container as IfMatchStatementType;
            }
            return container as BaseTransformationStatementType;
        }

        return undefined;
    }

    /**
     * Returns the CST node to be used as the "previous item" reference when inserting
     * new content before `targetStmt`.
     *
     * - At the top level: returns the previous statement's CST, or the import's CST when
     *   `targetStmt` is the very first top-level statement.
     * - Inside a nested scope: returns the previous statement's CST when one exists,
     *   otherwise `undefined` (the caller falls back to
     *   {@link createInsertBeforeStatementEdit}).
     *
     * @param targetStmt The statement before which insertion is desired
     * @param sourceModel The transformation root AST node
     * @returns The CST node to insert after, or `undefined`
     */
    private findInsertAfterCstForStatement(targetStmt: BaseTransformationStatementType): CstNode | undefined {
        const parent = targetStmt.$container;

        if (this.reflection.isInstance(parent, ModelTransformation)) {
            const idx = parent.statements.indexOf(targetStmt);
            if (idx > 0) {
                return parent.statements[idx - 1]!.$cstNode;
            }
            return parent.import?.$cstNode;
        }

        if (this.reflection.isInstance(parent, StatementsScope)) {
            const idx = parent.statements.indexOf(targetStmt);
            if (idx > 0) {
                return parent.statements[idx - 1]!.$cstNode;
            }
            return undefined;
        }

        return undefined;
    }

    /**
     * Creates a workspace edit that inserts `text` on a new line immediately before
     * the line on which `targetCst` starts, using the same indentation as the target
     * statement.  A blank line is added after the new text to separate it visually.
     *
     * @param targetCst The CST node whose line the new text should precede
     * @param text The source text to insert (first line should carry no leading indentation)
     * @returns The workspace edit
     */
    private createInsertBeforeStatementEdit(targetCst: CstNode, text: string): WorkspaceEdit {
        const document = this.getSourceDocument();
        const uri = document.uri.toString();

        const indentation = this.getIndentationForLine(targetCst.range.start);
        const indentedLines = text.split("\n").map((line) => indentation + line);
        const indentedText = indentedLines.join("\n");

        const insertPos = { line: targetCst.range.start.line, character: 0 };
        return {
            changes: {
                [uri]: [
                    {
                        range: Range.create(insertPos, insertPos),
                        newText: indentedText + "\n\n"
                    }
                ]
            }
        };
    }

    /**
     * Scans all AST nodes of the source model looking for a transformation statement
     * whose registry ID equals `stmtId`.
     *
     * @param stmtId The registry ID to search for
     * @param sourceModel The transformation root AST node
     * @returns The matching statement, or `undefined`
     */
    private findStatementById(
        stmtId: string,
        sourceModel: ModelTransformationType
    ): BaseTransformationStatementType | undefined {
        for (const node of AstUtils.streamAllContents(sourceModel)) {
            if (this.index.getElementId(node) === stmtId) {
                return this.toOuterStatement(node);
            }
        }
        return undefined;
    }

    /**
     * If `nodeId` ends with `"_merge"`, returns the base statement ID (the registry ID
     * of the structural statement that owns the merge node).  Otherwise returns `undefined`.
     *
     * @param nodeId A CFG node ID
     * @returns The base statement ID, or `undefined`
     */
    private extractStmtIdFromMergeNode(nodeId: string): string | undefined {
        const suffix = "_merge";
        if (nodeId.endsWith(suffix)) {
            return nodeId.slice(0, -suffix.length);
        }
        return undefined;
    }
}
