import type { GModelElement, GModelRoot } from "@eclipse-glsp/server";
import { sharedImport, BaseGModelFactory, GCompartment, GHorizontalDivider } from "@mdeo/language-shared";
import type { ModelIdRegistry, GraphMetadata } from "@mdeo/language-shared";
import type { NodeLayoutMetadata } from "@mdeo/editor-protocol";
import { NodeLayoutMetadataUtil, EdgeLayoutMetadataUtil } from "./metadataTypes.js";
import {
    type ModelTransformationType,
    type BaseTransformationStatementType,
    type MatchStatementType,
    type IfMatchStatementType,
    type WhileMatchStatementType,
    type UntilMatchStatementType,
    type ForMatchStatementType,
    type IfExpressionStatementType,
    type WhileExpressionStatementType,
    type StopStatementType,
    type PatternType,
    type PatternObjectInstanceType,
    type PatternLinkType,
    type PatternPropertyAssignmentType,
    type WhereClauseType,
    type PatternVariableType,
    type ElseIfBranchType,
    type PatternObjectInstanceReferenceType,
    type PatternObjectInstanceDeleteType,
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    IfExpressionStatement,
    WhileExpressionStatement,
    StopStatement,
    PatternObjectInstance,
    PatternLink,
    WhereClause,
    PatternVariable,
    PatternObjectInstanceReference,
    PatternObjectInstanceDelete
} from "../../grammar/modelTransformationTypes.js";
import { GStartNode } from "./model/startNode.js";
import { GEndNode } from "./model/endNode.js";
import { GMatchNode } from "./model/matchNode.js";
import { GSplitNode } from "./model/splitNode.js";
import { GMergeNode } from "./model/mergeNode.js";
import { GControlFlowEdge } from "./model/controlFlowEdge.js";
import { GControlFlowLabelNode } from "./model/controlFlowLabelNode.js";
import { GControlFlowLabel } from "./model/controlFlowLabel.js";
import { GPatternInstanceNode } from "./model/patternInstanceNode.js";
import { GPatternInstanceNameLabel } from "./model/patternInstanceNameLabel.js";
import { GPatternModifierTitleCompartment } from "./model/patternModifierTitleCompartment.js";
import { GMatchNodeCompartments } from "./model/matchNodeCompartments.js";
import { GPatternPropertyLabel } from "./model/patternPropertyLabel.js";
import { GPatternLinkEdge } from "./model/patternLinkEdge.js";
import { GPatternLinkEndNode } from "./model/patternLinkEndNode.js";
import { GPatternLinkEndLabel } from "./model/patternLinkEndLabel.js";
import { GPatternLinkModifierLabel } from "./model/patternLinkModifierLabel.js";
import { GWhereClauseLabel } from "./model/whereClauseLabel.js";
import { GVariableLabel } from "./model/variableLabel.js";
import { EndNodeKind, ModelTransformationElementType, PatternModifierKind } from "./model/elementTypes.js";
import { ModelTransformationIdGenerator } from "./modelTransformationIdGenerator.js";

const { injectable } = sharedImport("inversify");
const { GGraph } = sharedImport("@eclipse-glsp/server");

type GGraphType = ReturnType<typeof GGraph.builder>["proxy"];

/**
 * Result of processing a statements scope.
 * Contains the optional last node ID if the scope does not terminate.
 */
interface ScopeProcessingResult {
    /**
     * The ID of the last node in the scope, if the scope does not terminate with stop/kill.
     * Undefined if the scope terminates.
     */
    lastNodeId: string | undefined;
}

/**
 * Factory for creating graph models from model transformation source models.
 * Creates a control flow graph with nested pattern elements.
 */
@injectable()
export class ModelTransformationGModelFactory extends BaseGModelFactory<ModelTransformationType> {
    /**
     * Tracks instances that have been referenced in the current match.
     * Used to avoid duplicating referenced instances.
     */
    private referencedInstancesInCurrentMatch = new Set<string>();

    /**
     * Creates the graph model from the transformation source model.
     *
     * @param sourceModel The transformation source model
     * @param idRegistry The model ID registry
     * @returns The created graph model root
     */
    override createModelInternal(sourceModel: ModelTransformationType, idRegistry: ModelIdRegistry): GModelRoot {
        const graph = GGraph.builder().id("transformation-graph").addCssClass("editor-model-transformation").build();

        const startNodeId = this.createStartNode(graph);

        const statements = sourceModel.statements ?? [];
        const result = this.processStatements(graph, statements, startNodeId, idRegistry);

        if (result.lastNodeId != undefined) {
            const endNodeId = this.createImplicitEndNode(graph);
            this.createControlFlowEdge(graph, result.lastNodeId, endNodeId, undefined);
        }

        return graph;
    }

    /**
     * Creates a start node for the transformation.
     *
     * @param graph The graph to add the node to
     * @returns The ID of the created start node
     */
    private createStartNode(graph: GGraphType): string {
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const nodeId = "start";
        const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

        const node = GStartNode.builder().id(nodeId).meta(metadata).build();
        graph.children.push(node);

        return nodeId;
    }

    /**
     * Creates an implicit end node when the transformation doesn't explicitly terminate.
     *
     * @param graph The graph to add the node to
     * @returns The ID of the created end node
     */
    private createImplicitEndNode(graph: GGraphType): string {
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const nodeId = "implicit_end";
        const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

        const node = GEndNode.builder().id(nodeId).kind(EndNodeKind.STOP).meta(metadata).build();
        graph.children.push(node);

        return nodeId;
    }

    /**
     * Creates an explicit end node for stop/kill statements.
     *
     * @param graph The graph to add the node to
     * @param stmt The stop statement
     * @param idRegistry The model ID registry
     * @returns The ID of the created end node
     */
    private createEndNode(graph: GGraphType, stmt: StopStatementType, idRegistry: ModelIdRegistry): string {
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const nodeId = idRegistry.getId(stmt);
        const metadata = this.getNodeMetadata(validatedMetadata, nodeId);
        const kind = stmt.keyword === "kill" ? EndNodeKind.KILL : EndNodeKind.STOP;

        const node = GEndNode.builder().id(nodeId).kind(kind).meta(metadata).build();
        graph.children.push(node);

        return nodeId;
    }

    /**
     * Processes a list of statements and creates the control flow graph.
     *
     * @param graph The graph to add elements to
     * @param statements The statements to process
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the last node ID if not terminated
     */
    private processStatements(
        graph: GGraphType,
        statements: BaseTransformationStatementType[],
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        let currentNodeId: string | undefined = previousNodeId;

        for (const stmt of statements) {
            if (stmt == undefined || currentNodeId == undefined) {
                continue;
            }

            const result = this.processStatement(graph, stmt, currentNodeId, idRegistry);
            currentNodeId = result.lastNodeId;
        }

        return { lastNodeId: currentNodeId };
    }

    /**
     * Processes a single statement and creates the appropriate graph elements.
     *
     * @param graph The graph to add elements to
     * @param stmt The statement to process
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the last node ID if not terminated
     */
    private processStatement(
        graph: GGraphType,
        stmt: BaseTransformationStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        if (this.reflection.isInstance(stmt, MatchStatement)) {
            return this.processMatchStatement(graph, stmt as MatchStatementType, previousNodeId, idRegistry);
        }
        if (this.reflection.isInstance(stmt, IfMatchStatement)) {
            return this.processIfMatchStatement(graph, stmt as IfMatchStatementType, previousNodeId, idRegistry);
        }
        if (this.reflection.isInstance(stmt, WhileMatchStatement)) {
            return this.processWhileMatchStatement(graph, stmt as WhileMatchStatementType, previousNodeId, idRegistry);
        }
        if (this.reflection.isInstance(stmt, UntilMatchStatement)) {
            return this.processUntilMatchStatement(graph, stmt as UntilMatchStatementType, previousNodeId, idRegistry);
        }
        if (this.reflection.isInstance(stmt, ForMatchStatement)) {
            return this.processForMatchStatement(graph, stmt as ForMatchStatementType, previousNodeId, idRegistry);
        }
        if (this.reflection.isInstance(stmt, IfExpressionStatement)) {
            return this.processIfExpressionStatement(
                graph,
                stmt as IfExpressionStatementType,
                previousNodeId,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, WhileExpressionStatement)) {
            return this.processWhileExpressionStatement(
                graph,
                stmt as WhileExpressionStatementType,
                previousNodeId,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, StopStatement)) {
            return this.processStopStatement(graph, stmt as StopStatementType, previousNodeId, idRegistry);
        }

        return { lastNodeId: previousNodeId };
    }

    /**
     * Processes a simple match statement.
     *
     * @param graph The graph to add elements to
     * @param stmt The match statement
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the match node ID
     */
    private processMatchStatement(
        graph: GGraphType,
        stmt: MatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const nodeId = idRegistry.getId(stmt);
        this.createMatchNode(graph, nodeId, "match", false, stmt.pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, nodeId, undefined);

        return { lastNodeId: nodeId };
    }

    /**
     * Processes an if-match statement.
     * Creates a match node with two outgoing branches (then/else).
     *
     * @param graph The graph to add elements to
     * @param stmt The if-match statement
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the merge node ID if not all branches terminate
     */
    private processIfMatchStatement(
        graph: GGraphType,
        stmt: IfMatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const stmtId = idRegistry.getId(stmt);

        const matchNodeId = ModelTransformationIdGenerator.matchNode(stmtId);
        const pattern = stmt.ifBlock?.pattern;
        this.createMatchNode(graph, matchNodeId, "if match", false, pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, matchNodeId, undefined);

        return this.processIfMatchStatementBranches(graph, stmt, stmtId, matchNodeId, idRegistry);
    }

    /**
     * Processes the branches of an if-match statement (shared logic).
     */
    private processIfMatchStatementBranches(
        graph: GGraphType,
        stmt: IfMatchStatementType,
        stmtId: string,
        matchNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const thenBlock = stmt.ifBlock?.thenBlock;
        const thenStatements = thenBlock?.statements ?? [];
        const thenResult = this.processStatementsWithEdge(graph, thenStatements, matchNodeId, "then", idRegistry);

        let elseResult: ScopeProcessingResult = { lastNodeId: matchNodeId };
        if (stmt.elseBlock != undefined) {
            const elseStatements = stmt.elseBlock.statements ?? [];
            if (elseStatements.length > 0) {
                elseResult = this.processStatementsWithEdge(graph, elseStatements, matchNodeId, "else", idRegistry);
            }
        }

        if (thenResult.lastNodeId != undefined || elseResult.lastNodeId != undefined) {
            const mergeNodeId = ModelTransformationIdGenerator.mergeNode(stmtId);
            this.createMergeNode(graph, mergeNodeId);

            if (thenResult.lastNodeId != undefined) {
                this.createControlFlowEdge(graph, thenResult.lastNodeId, mergeNodeId, undefined);
            }
            if (elseResult.lastNodeId != undefined && stmt.elseBlock != undefined) {
                if (elseResult.lastNodeId === matchNodeId) {
                    if (thenResult.lastNodeId !== matchNodeId) {
                        this.createControlFlowEdge(graph, matchNodeId, mergeNodeId, "else");
                    }
                } else {
                    this.createControlFlowEdge(graph, elseResult.lastNodeId, mergeNodeId, undefined);
                }
            } else if (elseResult.lastNodeId != undefined && thenResult.lastNodeId !== matchNodeId) {
                this.createControlFlowEdge(graph, matchNodeId, mergeNodeId, "else");
            }

            return { lastNodeId: mergeNodeId };
        }

        return { lastNodeId: undefined };
    }

    /**
     * Processes a while-match statement.
     * Creates a loop back from the do block to the match.
     *
     * @param graph The graph to add elements to
     * @param stmt The while-match statement
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the match node ID as exit point
     */
    private processWhileMatchStatement(
        graph: GGraphType,
        stmt: WhileMatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);

        this.createMatchNode(graph, matchNodeId, "while match", false, stmt.pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, matchNodeId, undefined);

        return this.processWhileMatchStatementBody(graph, stmt, matchNodeId, idRegistry);
    }

    /**
     * Processes the body of a while-match statement (shared logic).
     */
    private processWhileMatchStatementBody(
        graph: GGraphType,
        stmt: WhileMatchStatementType,
        matchNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const doStatements = stmt.doBlock?.statements ?? [];
        if (doStatements.length > 0) {
            const doResult = this.processStatementsWithEdge(graph, doStatements, matchNodeId, "match", idRegistry);

            if (doResult.lastNodeId != undefined) {
                this.createControlFlowEdge(graph, doResult.lastNodeId, matchNodeId, undefined);
            }
        }

        return { lastNodeId: matchNodeId };
    }

    /**
     * Processes an until-match statement.
     * Executes the do block until the pattern matches.
     *
     * @param graph The graph to add elements to
     * @param stmt The until-match statement
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the match node ID as exit point
     */
    private processUntilMatchStatement(
        graph: GGraphType,
        stmt: UntilMatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);

        this.createMatchNode(graph, matchNodeId, "until match", false, stmt.pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, matchNodeId, undefined);

        return this.processUntilMatchStatementBody(graph, stmt, matchNodeId, idRegistry);
    }

    /**
     * Processes the body of an until-match statement (shared logic).
     */
    private processUntilMatchStatementBody(
        graph: GGraphType,
        stmt: UntilMatchStatementType,
        matchNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const doStatements = stmt.doBlock?.statements ?? [];
        if (doStatements.length > 0) {
            const doResult = this.processStatementsWithEdge(graph, doStatements, matchNodeId, "no match", idRegistry);

            if (doResult.lastNodeId != undefined) {
                this.createControlFlowEdge(graph, doResult.lastNodeId, matchNodeId, undefined);
            }
        }

        return { lastNodeId: matchNodeId };
    }

    /**
     * Processes a for-match statement.
     * Iterates over all matches of the pattern.
     *
     * @param graph The graph to add elements to
     * @param stmt The for-match statement
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the match node ID as exit point
     */
    private processForMatchStatement(
        graph: GGraphType,
        stmt: ForMatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);

        this.createMatchNode(graph, matchNodeId, "for match", true, stmt.pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, matchNodeId, undefined);

        return this.processForMatchStatementBody(graph, stmt, matchNodeId, idRegistry);
    }

    /**
     * Processes the body of a for-match statement (shared logic).
     */
    private processForMatchStatementBody(
        graph: GGraphType,
        stmt: ForMatchStatementType,
        matchNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const doStatements = stmt.doBlock?.statements ?? [];
        if (doStatements.length > 0) {
            const doResult = this.processStatementsWithEdge(graph, doStatements, matchNodeId, "each", idRegistry);

            if (doResult.lastNodeId != undefined) {
                this.createControlFlowEdge(graph, doResult.lastNodeId, matchNodeId, undefined);
            }
        }

        return { lastNodeId: matchNodeId };
    }

    /**
     * Processes an if-expression statement.
     * Creates a split node for the condition and branches.
     *
     * @param graph The graph to add elements to
     * @param stmt The if-expression statement
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the merge node ID if not all branches terminate
     */
    private processIfExpressionStatement(
        graph: GGraphType,
        stmt: IfExpressionStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const stmtId = idRegistry.getId(stmt);

        const splitId = `${stmtId}_split`;
        const conditionText = stmt.condition?.$cstNode?.text ?? "?";
        this.createSplitNode(graph, splitId, conditionText);
        this.createControlFlowEdge(graph, previousNodeId, splitId, undefined);

        return this.processIfExpressionStatementBranches(graph, stmt, stmtId, splitId, idRegistry);
    }

    /**
     * Processes the branches of an if-expression statement (shared logic).
     */
    private processIfExpressionStatementBranches(
        graph: GGraphType,
        stmt: IfExpressionStatementType,
        stmtId: string,
        splitId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const branchResults: ScopeProcessingResult[] = [];

        const thenStatements = stmt.thenBlock?.statements ?? [];
        const thenResult = this.processStatementsWithEdge(graph, thenStatements, splitId, "true", idRegistry);
        branchResults.push(thenResult);

        let lastElseIfSplitId = splitId;
        for (let i = 0; i < (stmt.elseIfBranches?.length ?? 0); i++) {
            const elseIfBranch = stmt.elseIfBranches![i] as ElseIfBranchType;
            const elseIfSplitId = `${stmtId}_elseif_${i}_split`;
            const elseIfConditionText = elseIfBranch.condition?.$cstNode?.text ?? "?";

            this.createSplitNode(graph, elseIfSplitId, elseIfConditionText);
            this.createControlFlowEdge(graph, lastElseIfSplitId, elseIfSplitId, "false");

            const elseIfStatements = elseIfBranch.block?.statements ?? [];
            const elseIfResult = this.processStatementsWithEdge(
                graph,
                elseIfStatements,
                elseIfSplitId,
                "true",
                idRegistry
            );
            branchResults.push(elseIfResult);

            lastElseIfSplitId = elseIfSplitId;
        }

        if (stmt.elseBlock != undefined) {
            const elseStatements = stmt.elseBlock.statements ?? [];
            const elseResult = this.processStatementsWithEdge(
                graph,
                elseStatements,
                lastElseIfSplitId,
                "false",
                idRegistry
            );
            branchResults.push(elseResult);
        } else {
            branchResults.push({ lastNodeId: lastElseIfSplitId });
        }

        const nonTerminatingBranches = branchResults.filter((r) => r.lastNodeId != undefined);
        if (nonTerminatingBranches.length > 0) {
            const mergeNodeId = ModelTransformationIdGenerator.mergeNode(stmtId);
            this.createMergeNode(graph, mergeNodeId);

            const connectedToMerge = new Set<string>();
            for (const result of nonTerminatingBranches) {
                const sourceId = result.lastNodeId!;
                if (connectedToMerge.has(sourceId)) {
                    continue;
                }
                connectedToMerge.add(sourceId);

                if (sourceId === lastElseIfSplitId && stmt.elseBlock == undefined) {
                    this.createControlFlowEdge(graph, sourceId, mergeNodeId, "false");
                } else {
                    this.createControlFlowEdge(graph, sourceId, mergeNodeId, undefined);
                }
            }

            return { lastNodeId: mergeNodeId };
        }

        return { lastNodeId: undefined };
    }

    /**
     * Processes a while-expression statement.
     * Creates a split node for the condition with a loop.
     *
     * @param graph The graph to add elements to
     * @param stmt The while-expression statement
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result containing the split node ID as exit point
     */
    private processWhileExpressionStatement(
        graph: GGraphType,
        stmt: WhileExpressionStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const stmtId = idRegistry.getId(stmt);

        const splitId = `${stmtId}_split`;
        const conditionText = stmt.condition?.$cstNode?.text ?? "?";
        this.createSplitNode(graph, splitId, conditionText);
        this.createControlFlowEdge(graph, previousNodeId, splitId, undefined);

        return this.processWhileExpressionStatementBody(graph, stmt, splitId, idRegistry);
    }

    /**
     * Processes the body of a while-expression statement (shared logic).
     */
    private processWhileExpressionStatementBody(
        graph: GGraphType,
        stmt: WhileExpressionStatementType,
        splitId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const bodyStatements = stmt.block?.statements ?? [];
        const bodyResult = this.processStatementsWithEdge(graph, bodyStatements, splitId, "true", idRegistry);

        if (bodyResult.lastNodeId != undefined) {
            this.createControlFlowEdge(graph, bodyResult.lastNodeId, splitId, undefined);
        }

        return { lastNodeId: splitId };
    }

    /**
     * Processes a stop/kill statement.
     *
     * @param graph The graph to add elements to
     * @param stmt The stop statement
     * @param previousNodeId The ID of the previous node to connect from
     * @param idRegistry The model ID registry
     * @returns The result with undefined lastNodeId (terminated)
     */
    private processStopStatement(
        graph: GGraphType,
        stmt: StopStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const endNodeId = this.createEndNode(graph, stmt, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, endNodeId, undefined);

        return { lastNodeId: undefined };
    }

    /**
     * Helper to process statements and create an initial edge with a label.
     *
     * @param graph The graph to add elements to
     * @param statements The statements to process
     * @param sourceNodeId The source node ID for the initial edge
     * @param label The label for the initial edge
     * @param idRegistry The model ID registry
     * @returns The processing result
     */
    private processStatementsWithEdge(
        graph: GGraphType,
        statements: BaseTransformationStatementType[],
        sourceNodeId: string,
        label: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        if (statements.length === 0) {
            return { lastNodeId: sourceNodeId };
        }

        const firstStmt = statements[0]!;
        const firstResult = this.processStatementWithInitialEdge(graph, firstStmt, sourceNodeId, label, idRegistry);

        let currentNodeId = firstResult.lastNodeId;
        for (let i = 1; i < statements.length; i++) {
            const stmt = statements[i];
            if (stmt == undefined || currentNodeId == undefined) {
                continue;
            }
            const result = this.processStatement(graph, stmt, currentNodeId, idRegistry);
            currentNodeId = result.lastNodeId;
        }

        return { lastNodeId: currentNodeId };
    }

    /**
     * Processes a statement with a custom initial edge label.
     * Used when the incoming edge needs a specific label (e.g., "true", "false").
     *
     * @param graph The graph to add elements to
     * @param stmt The statement to process
     * @param previousNodeId The ID of the previous node
     * @param edgeLabel The label for the incoming edge
     * @param idRegistry The model ID registry
     * @returns The processing result
     */
    private processStatementWithInitialEdge(
        graph: GGraphType,
        stmt: BaseTransformationStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        if (this.reflection.isInstance(stmt, MatchStatement)) {
            return this.processMatchStatementWithLabel(
                graph,
                stmt as MatchStatementType,
                previousNodeId,
                edgeLabel,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, IfMatchStatement)) {
            return this.processIfMatchStatementWithLabel(
                graph,
                stmt as IfMatchStatementType,
                previousNodeId,
                edgeLabel,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, WhileMatchStatement)) {
            return this.processWhileMatchStatementWithLabel(
                graph,
                stmt as WhileMatchStatementType,
                previousNodeId,
                edgeLabel,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, UntilMatchStatement)) {
            return this.processUntilMatchStatementWithLabel(
                graph,
                stmt as UntilMatchStatementType,
                previousNodeId,
                edgeLabel,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, ForMatchStatement)) {
            return this.processForMatchStatementWithLabel(
                graph,
                stmt as ForMatchStatementType,
                previousNodeId,
                edgeLabel,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, IfExpressionStatement)) {
            return this.processIfExpressionStatementWithLabel(
                graph,
                stmt as IfExpressionStatementType,
                previousNodeId,
                edgeLabel,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, WhileExpressionStatement)) {
            return this.processWhileExpressionStatementWithLabel(
                graph,
                stmt as WhileExpressionStatementType,
                previousNodeId,
                edgeLabel,
                idRegistry
            );
        }
        if (this.reflection.isInstance(stmt, StopStatement)) {
            return this.processStopStatementWithLabel(
                graph,
                stmt as StopStatementType,
                previousNodeId,
                edgeLabel,
                idRegistry
            );
        }

        return { lastNodeId: previousNodeId };
    }

    /**
     * Processes a match statement with a custom edge label.
     */
    private processMatchStatementWithLabel(
        graph: GGraphType,
        stmt: MatchStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const nodeId = idRegistry.getId(stmt);
        this.createMatchNode(graph, nodeId, "match", false, stmt.pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, nodeId, edgeLabel);
        return { lastNodeId: nodeId };
    }

    /**
     * Processes an if-match statement with a custom edge label.
     */
    private processIfMatchStatementWithLabel(
        graph: GGraphType,
        stmt: IfMatchStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const stmtId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(stmtId);
        const pattern = stmt.ifBlock?.pattern;
        this.createMatchNode(graph, matchNodeId, "if match", false, pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, matchNodeId, edgeLabel);
        return this.processIfMatchStatementBranches(graph, stmt, stmtId, matchNodeId, idRegistry);
    }

    /**
     * Processes a while-match statement with a custom edge label.
     */
    private processWhileMatchStatementWithLabel(
        graph: GGraphType,
        stmt: WhileMatchStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);
        this.createMatchNode(graph, matchNodeId, "while match", false, stmt.pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, matchNodeId, edgeLabel);
        return this.processWhileMatchStatementBody(graph, stmt, matchNodeId, idRegistry);
    }

    /**
     * Processes an until-match statement with a custom edge label.
     */
    private processUntilMatchStatementWithLabel(
        graph: GGraphType,
        stmt: UntilMatchStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);
        this.createMatchNode(graph, matchNodeId, "until match", false, stmt.pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, matchNodeId, edgeLabel);
        return this.processUntilMatchStatementBody(graph, stmt, matchNodeId, idRegistry);
    }

    /**
     * Processes a for-match statement with a custom edge label.
     */
    private processForMatchStatementWithLabel(
        graph: GGraphType,
        stmt: ForMatchStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);
        this.createMatchNode(graph, matchNodeId, "for match", true, stmt.pattern, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, matchNodeId, edgeLabel);
        return this.processForMatchStatementBody(graph, stmt, matchNodeId, idRegistry);
    }

    /**
     * Processes an if-expression statement with a custom edge label.
     */
    private processIfExpressionStatementWithLabel(
        graph: GGraphType,
        stmt: IfExpressionStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const stmtId = idRegistry.getId(stmt);
        const splitId = `${stmtId}_split`;
        const conditionText = stmt.condition?.$cstNode?.text ?? "?";
        this.createSplitNode(graph, splitId, conditionText);
        this.createControlFlowEdge(graph, previousNodeId, splitId, edgeLabel);
        return this.processIfExpressionStatementBranches(graph, stmt, stmtId, splitId, idRegistry);
    }

    /**
     * Processes a while-expression statement with a custom edge label.
     */
    private processWhileExpressionStatementWithLabel(
        graph: GGraphType,
        stmt: WhileExpressionStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const stmtId = idRegistry.getId(stmt);
        const splitId = `${stmtId}_split`;
        const conditionText = stmt.condition?.$cstNode?.text ?? "?";
        this.createSplitNode(graph, splitId, conditionText);
        this.createControlFlowEdge(graph, previousNodeId, splitId, edgeLabel);
        return this.processWhileExpressionStatementBody(graph, stmt, splitId, idRegistry);
    }

    /**
     * Processes a stop statement with a custom edge label.
     */
    private processStopStatementWithLabel(
        graph: GGraphType,
        stmt: StopStatementType,
        previousNodeId: string,
        edgeLabel: string,
        idRegistry: ModelIdRegistry
    ): ScopeProcessingResult {
        const endNodeId = this.createEndNode(graph, stmt, idRegistry);
        this.createControlFlowEdge(graph, previousNodeId, endNodeId, edgeLabel);
        return { lastNodeId: undefined };
    }

    /**
     * Creates a match node with its pattern elements.
     *
     * @param graph The graph to add the node to
     * @param nodeId The node ID
     * @param label The label for the match node
     * @param multiple Whether this is a "for match" (multiple matches)
     * @param pattern The pattern definition
     * @param idRegistry The model ID registry
     */
    private createMatchNode(
        graph: GGraphType,
        nodeId: string,
        label: string,
        multiple: boolean,
        pattern: PatternType | undefined,
        idRegistry: ModelIdRegistry
    ): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

        const node = GMatchNode.builder().id(nodeId).label(label).multiple(multiple).meta(metadata).build();

        this.referencedInstancesInCurrentMatch.clear();

        const localInstances = new Map<string, PatternObjectInstanceType>();
        const referencedInstanceNodes = new Map<string, PatternObjectInstanceReferenceType | null>();
        const deletedInstances = new Set<string>();

        if (pattern?.elements != undefined) {
            for (const element of pattern.elements) {
                if (this.reflection.isInstance(element, PatternObjectInstance)) {
                    const instance = element as PatternObjectInstanceType;
                    if (instance.name) {
                        localInstances.set(instance.name, instance);
                    }
                }
                if (this.reflection.isInstance(element, PatternObjectInstanceReference)) {
                    const ref = element as PatternObjectInstanceReferenceType;
                    if (ref.instance?.ref != undefined) {
                        const instanceRef = ref.instance.ref;
                        if (instanceRef.name) {
                            referencedInstanceNodes.set(instanceRef.name, ref);
                        }
                    }
                }
                if (this.reflection.isInstance(element, PatternObjectInstanceDelete)) {
                    const del = element as PatternObjectInstanceDeleteType;
                    const instanceName = del.instance?.ref?.name;
                    if (instanceName) {
                        deletedInstances.add(instanceName);
                    }
                }
                if (this.reflection.isInstance(element, PatternLink)) {
                    const link = element as PatternLinkType;
                    const sourceRef = link.source?.object?.ref;
                    const targetRef = link.target?.object?.ref;
                    if (sourceRef?.name && !localInstances.has(sourceRef.name)) {
                        if (!referencedInstanceNodes.has(sourceRef.name)) {
                            referencedInstanceNodes.set(sourceRef.name, null);
                        }
                    }
                    if (targetRef?.name && !localInstances.has(targetRef.name)) {
                        if (!referencedInstanceNodes.has(targetRef.name)) {
                            referencedInstanceNodes.set(targetRef.name, null);
                        }
                    }
                }
            }
        }

        if (pattern?.elements != undefined) {
            for (const element of pattern.elements) {
                if (this.reflection.isInstance(element, PatternObjectInstance)) {
                    const instance = element as PatternObjectInstanceType;
                    this.createPatternInstanceNode(node, instance, idRegistry);
                }
            }
        }

        for (const [instanceName, reference] of referencedInstanceNodes) {
            if (!localInstances.has(instanceName) && !deletedInstances.has(instanceName)) {
                this.createReferencedInstanceNode(node, instanceName, nodeId, reference ?? undefined, idRegistry);
            }
        }

        if (pattern?.elements != undefined) {
            for (const element of pattern.elements) {
                if (this.reflection.isInstance(element, PatternLink)) {
                    this.createPatternLinkEdge(node, element as PatternLinkType, nodeId, idRegistry);
                }
            }
        }

        if (pattern?.elements != undefined) {
            const container = this.createConstraintCompartments(nodeId, pattern.elements, idRegistry);
            if (container != undefined) {
                node.children.push(container);
            }
        }

        graph.children.push(node);
    }

    /**
     * Creates a pattern instance node.
     *
     * @param parent The parent node (match node)
     * @param instance The pattern object instance
     * @param idRegistry The model ID registry
     */
    private createPatternInstanceNode(
        parent: GMatchNode,
        instance: PatternObjectInstanceType,
        idRegistry: ModelIdRegistry
    ): void {
        const nodeId = idRegistry.getId(instance);
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

        const name = instance.name ?? "unnamed";
        const typeName = instance.class?.$refText ?? instance.class?.ref?.name ?? undefined;
        const modifier = this.getPatternModifierKind(instance.modifier?.modifier);

        const node = GPatternInstanceNode.builder().id(nodeId).name(name).modifier(modifier).meta(metadata).build();

        if (typeName != undefined) {
            node.typeName = typeName;
        }

        if (modifier !== PatternModifierKind.NONE) {
            const modifierCompartment = GPatternModifierTitleCompartment.builder()
                .id(`${nodeId}#modifier-title`)
                .build();
            const labelText = typeName != undefined ? `${name} : ${typeName}` : name;
            const label = GPatternInstanceNameLabel.builder()
                .id(`${nodeId}#name`)
                .text(labelText)
                .readonly(true)
                .build();
            modifierCompartment.children.push(label);
            node.children.push(modifierCompartment);
        } else {
            const headerChildren = this.createPatternInstanceHeader(nodeId, name, typeName);
            node.children.push(...headerChildren);
        }

        const propertyChildren = this.createPatternPropertyAssignments(nodeId, instance.properties ?? [], idRegistry);
        if (propertyChildren.length > 0) {
            node.children.push(...propertyChildren);
        }

        parent.children.push(node);
    }

    /**
     * Creates a referenced instance node (instance from previous match).
     *
     * @param parent The parent node (match node)
     * @param instanceName The name of the referenced instance
     * @param matchNodeId The ID of the containing match node
     */
    private createReferencedInstanceNode(
        parent: GMatchNode,
        instanceName: string,
        matchNodeId: string,
        reference: PatternObjectInstanceReferenceType | undefined,
        idRegistry: ModelIdRegistry
    ): void {
        const nodeId = ModelTransformationIdGenerator.referencedInstance(matchNodeId, instanceName);
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

        const node = GPatternInstanceNode.builder()
            .id(nodeId)
            .name(instanceName)
            .modifier(PatternModifierKind.NONE)
            .meta(metadata)
            .build();

        const headerChildren = this.createPatternInstanceHeader(nodeId, instanceName, undefined);
        node.children.push(...headerChildren);

        const propertyChildren = this.createPatternPropertyAssignments(nodeId, reference?.properties ?? [], idRegistry);
        if (propertyChildren.length > 0) {
            node.children.push(...propertyChildren);
        }

        parent.children.push(node);
        this.referencedInstancesInCurrentMatch.add(instanceName);
    }

    /**
     * Creates the header for a pattern instance node.
     *
     * @param nodeId The node ID
     * @param name The instance name
     * @param typeName The optional type name
     * @returns Array of header elements
     */
    private createPatternInstanceHeader(nodeId: string, name: string, typeName: string | undefined): GModelElement[] {
        const compartment = GCompartment.builder()
            .type(ModelTransformationElementType.COMPARTMENT)
            .id(`${nodeId}#header`)
            .build();

        const labelText = typeName != undefined ? `${name} : ${typeName}` : name;
        const label = GPatternInstanceNameLabel.builder().id(`${nodeId}#name`).text(labelText).readonly(true).build();

        compartment.children.push(label);
        return [compartment];
    }

    /**
     * Creates property assignment labels for a pattern instance.
     *
     * @param nodeId The parent node ID
     * @param properties The property assignments
     * @param idRegistry The model ID registry
     * @returns Array of property elements
     */
    private createPatternPropertyAssignments(
        nodeId: string,
        properties: PatternPropertyAssignmentType[],
        idRegistry: ModelIdRegistry
    ): GModelElement[] {
        if (properties.length === 0) {
            return [];
        }

        const children: GModelElement[] = [];

        const compartment = GCompartment.builder()
            .type(ModelTransformationElementType.COMPARTMENT)
            .id(`${nodeId}#properties`)
            .build();

        for (const prop of properties) {
            if (prop == undefined) continue;

            const propId = idRegistry.getId(prop);
            const propName = prop.name?.$refText ?? prop.name?.ref?.name ?? "?";
            const operator = prop.operator ?? "=";
            const valueText = prop.value?.$cstNode?.text ?? "?";
            const propText = `${propName} ${operator} ${valueText}`;

            const label = GPatternPropertyLabel.builder()
                .id(ModelTransformationIdGenerator.propertyLabel(propId))
                .text(propText)
                .readonly(true)
                .build();

            compartment.children.push(label);
        }

        const divider = GHorizontalDivider.builder()
            .type(ModelTransformationElementType.DIVIDER)
            .id(`${nodeId}#divider`)
            .build();

        children.push(divider);
        children.push(compartment);
        return children;
    }

    /**
     * Creates a container wrapping the where-clause and variable compartments.
     * The container also includes horizontal dividers between compartments.
     * Returns undefined if there are no where-clauses or variables.
     *
     * @param nodeId The parent node ID
     * @param elements The pattern elements
     * @param idRegistry The model ID registry
     * @returns A GMatchNodeCompartments container, or undefined if empty
     */
    private createConstraintCompartments(
        nodeId: string,
        elements: unknown[],
        idRegistry: ModelIdRegistry
    ): GMatchNodeCompartments | undefined {
        const whereClauseLabels: GModelElement[] = [];
        for (const element of elements) {
            if (this.reflection.isInstance(element, WhereClause)) {
                const where = element as WhereClauseType;
                const whereId = idRegistry.getId(where);
                const exprText = where.expression?.$cstNode?.text ?? "?";
                const label = GWhereClauseLabel.builder()
                    .id(ModelTransformationIdGenerator.whereClauseLabel(whereId))
                    .text(`where ${exprText}`)
                    .readonly(true)
                    .build();
                whereClauseLabels.push(label);
            }
        }

        const variableLabels: GModelElement[] = [];
        for (const element of elements) {
            if (this.reflection.isInstance(element, PatternVariable)) {
                const variable = element as PatternVariableType;
                const varId = idRegistry.getId(variable);
                const name = variable.name ?? "?";
                const typeText = variable.type?.$cstNode?.text;
                const valueText = variable.value?.$cstNode?.text ?? "?";
                let varText = `var ${name}`;
                if (typeText != undefined) {
                    varText += `: ${typeText}`;
                }
                varText += ` = ${valueText}`;

                const label = GVariableLabel.builder()
                    .id(ModelTransformationIdGenerator.variableLabel(varId))
                    .text(varText)
                    .readonly(true)
                    .build();
                variableLabels.push(label);
            }
        }

        const compartments: GCompartment[] = [];

        if (whereClauseLabels.length > 0) {
            const compartment = GCompartment.builder()
                .type(ModelTransformationElementType.COMPARTMENT)
                .id(`${nodeId}#where-clauses`)
                .build();

            compartment.children.push(...whereClauseLabels);
            compartments.push(compartment);
        }

        if (variableLabels.length > 0) {
            const compartment = GCompartment.builder()
                .type(ModelTransformationElementType.COMPARTMENT)
                .id(`${nodeId}#variables`)
                .build();

            compartment.children.push(...variableLabels);
            compartments.push(compartment);
        }

        if (compartments.length === 0) {
            return undefined;
        }

        const container = GMatchNodeCompartments.builder().id(`${nodeId}#compartments`).build();

        const topDivider = GHorizontalDivider.builder()
            .type(ModelTransformationElementType.DIVIDER)
            .id(`${nodeId}#compartments-top-divider`)
            .build();
        container.children.push(topDivider);

        for (let i = 0; i < compartments.length; i++) {
            if (i > 0) {
                const divider = GHorizontalDivider.builder()
                    .type(ModelTransformationElementType.DIVIDER)
                    .id(`${nodeId}#compartment-divider-${i}`)
                    .build();
                container.children.push(divider);
            }
            container.children.push(compartments[i]);
        }

        return container;
    }

    /**
     * Creates a pattern link edge between instances.
     *
     * @param parent The parent node (match node)
     * @param link The pattern link
     * @param matchNodeId The ID of the containing match node
     * @param idRegistry The model ID registry
     */
    private createPatternLinkEdge(
        parent: GMatchNode,
        link: PatternLinkType,
        matchNodeId: string,
        idRegistry: ModelIdRegistry
    ): void {
        const edgeId = idRegistry.getId(link);

        const sourceInstanceRef = link.source?.object?.ref;
        const targetInstanceRef = link.target?.object?.ref;

        if (sourceInstanceRef == undefined || targetInstanceRef == undefined) {
            return;
        }

        const sourceId = this.resolveInstanceNodeId(sourceInstanceRef, matchNodeId, idRegistry);
        const targetId = this.resolveInstanceNodeId(targetInstanceRef, matchNodeId, idRegistry);

        if (sourceId == undefined || targetId == undefined) {
            return;
        }

        const modifier = this.getPatternModifierKind(link.modifier?.modifier);
        const sourceProperty = link.source?.property?.$refText;
        const targetProperty = link.target?.property?.$refText;

        const validatedMetadata = this.modelState.getValidatedMetadata();
        const rawEdgeMeta = validatedMetadata.edges[edgeId]?.meta;
        const edgeMeta =
            rawEdgeMeta != undefined && EdgeLayoutMetadataUtil.isValid(rawEdgeMeta)
                ? rawEdgeMeta
                : EdgeLayoutMetadataUtil.create();

        const edgeBuilder = GPatternLinkEdge.builder()
            .id(edgeId)
            .sourceId(sourceId)
            .targetId(targetId)
            .modifier(modifier)
            .meta(edgeMeta);

        if (sourceProperty != undefined) {
            edgeBuilder.sourceProperty(sourceProperty);
        }
        if (targetProperty != undefined) {
            edgeBuilder.targetProperty(targetProperty);
        }

        const edge = edgeBuilder.build();

        this.addPatternLinkLabels(edge, edgeId, sourceProperty, targetProperty);

        if (modifier !== PatternModifierKind.NONE) {
            const modifierLabel = GPatternLinkModifierLabel.builder()
                .id(`${edgeId}#modifier-label`)
                .modifier(modifier)
                .build();
            edge.children.push(modifierLabel);
        }

        parent.children.push(edge);
    }

    /**
     * Resolves the node ID for a pattern instance from its AST node reference.
     *
     * @param instanceRef The pattern object instance AST node
     * @param matchNodeId The current match node ID
     * @param idRegistry The model ID registry
     * @returns The resolved node ID or undefined
     */
    private resolveInstanceNodeId(
        instanceRef: PatternObjectInstanceType | undefined,
        matchNodeId: string,
        idRegistry: ModelIdRegistry
    ): string | undefined {
        if (instanceRef == undefined) return undefined;
        const instanceName = instanceRef.name;
        if (instanceName && this.referencedInstancesInCurrentMatch.has(instanceName)) {
            return ModelTransformationIdGenerator.referencedInstance(matchNodeId, instanceName);
        }
        return idRegistry.getId(instanceRef);
    }

    /**
     * Adds property labels to a pattern link edge.
     *
     * @param edge The pattern link edge
     * @param edgeId The edge ID
     * @param sourceProperty The optional source property
     * @param targetProperty The optional target property
     */
    private addPatternLinkLabels(
        edge: GPatternLinkEdge,
        edgeId: string,
        sourceProperty: string | undefined,
        targetProperty: string | undefined
    ): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        if (sourceProperty != undefined) {
            const nodeId = ModelTransformationIdGenerator.patternLinkSourceEndNode(edgeId);
            const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

            const endNode = GPatternLinkEndNode.builder().id(nodeId).end("target").meta(metadata).build();

            const label = GPatternLinkEndLabel.builder()
                .id(ModelTransformationIdGenerator.patternLinkSourceEndLabel(edgeId))
                .text(sourceProperty)
                .readonly(true)
                .build();

            endNode.children.push(label);
            edge.children.push(endNode);
        }

        if (targetProperty != undefined) {
            const nodeId = ModelTransformationIdGenerator.patternLinkTargetEndNode(edgeId);
            const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

            const endNode = GPatternLinkEndNode.builder().id(nodeId).end("source").meta(metadata).build();

            const label = GPatternLinkEndLabel.builder()
                .id(ModelTransformationIdGenerator.patternLinkTargetEndLabel(edgeId))
                .text(targetProperty)
                .readonly(true)
                .build();

            endNode.children.push(label);
            edge.children.push(endNode);
        }
    }

    /**
     * Creates a split node for branching.
     *
     * @param graph The graph to add the node to
     * @param nodeId The node ID
     * @param expression The condition expression text
     */
    private createSplitNode(graph: GGraphType, nodeId: string, expression: string): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

        const node = GSplitNode.builder().id(nodeId).expression(expression).meta(metadata).build();
        graph.children.push(node);
    }

    /**
     * Creates a merge node where branches rejoin.
     *
     * @param graph The graph to add the node to
     * @param nodeId The node ID
     */
    private createMergeNode(graph: GGraphType, nodeId: string): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const metadata = this.getNodeMetadata(validatedMetadata, nodeId);

        const node = GMergeNode.builder().id(nodeId).meta(metadata).build();
        graph.children.push(node);
    }

    /**
     * Creates a control flow edge between nodes.
     *
     * @param graph The graph to add the edge to
     * @param sourceId The source node ID
     * @param targetId The target node ID
     * @param label The optional edge label
     */
    private createControlFlowEdge(
        graph: GGraphType,
        sourceId: string,
        targetId: string,
        label: string | undefined
    ): void {
        const edgeId = ModelTransformationIdGenerator.controlFlowEdge(sourceId, targetId);
        const validatedMetadata = this.modelState.getValidatedMetadata();
        const metadata = validatedMetadata.edges[edgeId]?.meta;
        const edgeMeta =
            metadata != undefined && EdgeLayoutMetadataUtil.isValid(metadata)
                ? metadata
                : EdgeLayoutMetadataUtil.create();

        const edge = GControlFlowEdge.builder().id(edgeId).sourceId(sourceId).targetId(targetId).build();

        edge.meta = edgeMeta;

        if (label != undefined) {
            const labelNodeId = ModelTransformationIdGenerator.controlFlowEdgeLabelNode(edgeId);
            const labelMetadata = this.getNodeMetadata(validatedMetadata, labelNodeId);

            const labelNode = GControlFlowLabelNode.builder().id(labelNodeId).end("source").meta(labelMetadata).build();

            const labelElement = GControlFlowLabel.builder()
                .id(ModelTransformationIdGenerator.controlFlowEdgeLabel(edgeId))
                .text(label)
                .readonly(true)
                .build();

            labelNode.children.push(labelElement);
            edge.children.push(labelNode);
        }

        graph.children.push(edge);
    }

    /**
     * Gets the pattern modifier kind from a string.
     *
     * @param modifier The modifier string
     * @returns The PatternModifierKind
     */
    private getPatternModifierKind(modifier: string | undefined): PatternModifierKind {
        switch (modifier) {
            case "create":
                return PatternModifierKind.CREATE;
            case "delete":
                return PatternModifierKind.DELETE;
            case "forbid":
                return PatternModifierKind.FORBID;
            case "require":
                return PatternModifierKind.REQUIRE;
            default:
                return PatternModifierKind.NONE;
        }
    }

    /**
     * Gets node metadata from validated metadata, providing defaults if not found.
     *
     * @param validatedMetadata The validated graph metadata
     * @param nodeId The node ID
     * @returns The node layout metadata
     */
    private getNodeMetadata(validatedMetadata: GraphMetadata, nodeId: string): NodeLayoutMetadata {
        const metadata = validatedMetadata.nodes[nodeId]?.meta;
        if (metadata != undefined && NodeLayoutMetadataUtil.isValid(metadata)) {
            return metadata;
        }
        return NodeLayoutMetadataUtil.create(0, 0);
    }
}
