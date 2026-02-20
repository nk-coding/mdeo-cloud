import {
    sharedImport,
    MetadataManager,
    type GraphMetadata,
    type NodeMetadata,
    type EdgeMetadata,
    type ModelIdRegistry,
    DefaultModelIdRegistry,
    ModelIdProvider,
    type ModelIdProvider as ModelIdProviderType
} from "@mdeo/language-shared";
import type { NodeAttributes, EdgeAttributes } from "@mdeo/language-shared";
import { ModelTransformationElementType } from "./model/elementTypes.js";
import type {
    ModelTransformationType,
    BaseTransformationStatementType,
    MatchStatementType,
    IfMatchStatementType,
    WhileMatchStatementType,
    UntilMatchStatementType,
    ForMatchStatementType,
    PatternType,
    PatternObjectInstanceType,
    PatternLinkType,
    PatternObjectInstanceReferenceType,
    PatternObjectInstanceDeleteType
} from "../../grammar/modelTransformationTypes.js";
import {
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    PatternObjectInstance,
    PatternLink,
    PatternObjectInstanceReference,
    PatternObjectInstanceDelete
} from "../../grammar/modelTransformationTypes.js";
import { NodeLayoutMetadataUtil, EdgeLayoutMetadataUtil } from "./metadataTypes.js";
import { ModelTransformationIdGenerator } from "./modelTransformationIdGenerator.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Manages metadata validation and synchronization for model transformation diagrams.
 * Implements cost calculations based on semantic similarity between model elements.
 */
@injectable()
export class ModelTransformationMetadataManager extends MetadataManager<ModelTransformationType> {
    @inject(ModelIdProvider)
    protected modelIdProvider!: ModelIdProviderType;

    /**
     * Tracks pattern instances declared in previous matches for reference resolution.
     * Maps instance name to the node ID of the pattern instance.
     */
    private declaredInstances = new Map<string, string>();

    /**
     * Verifies and corrects invalid metadata for nodes and edges.
     *
     * @param model The node or edge metadata to verify
     * @returns Corrected metadata if invalid, undefined if valid
     */
    protected override verifyMetadata(model: NodeMetadata | EdgeMetadata): object | undefined {
        // Main layout nodes
        if (
            model.type === ModelTransformationElementType.NODE_START ||
            model.type === ModelTransformationElementType.NODE_END ||
            model.type === ModelTransformationElementType.NODE_MATCH ||
            model.type === ModelTransformationElementType.NODE_SPLIT ||
            model.type === ModelTransformationElementType.NODE_MERGE ||
            model.type === ModelTransformationElementType.NODE_PATTERN_INSTANCE
        ) {
            return NodeLayoutMetadataUtil.verify(model.meta, 250);
        }

        // Edge labels
        if (
            model.type === ModelTransformationElementType.NODE_CONTROL_FLOW_LABEL ||
            model.type === ModelTransformationElementType.NODE_PATTERN_LINK_END
        ) {
            return NodeLayoutMetadataUtil.verify(model.meta);
        }

        // Control flow and pattern edges
        if (
            model.type === ModelTransformationElementType.EDGE_CONTROL_FLOW ||
            model.type === ModelTransformationElementType.EDGE_PATTERN_LINK
        ) {
            const edgeModel = model as EdgeMetadata;
            return EdgeLayoutMetadataUtil.verify(edgeModel.meta);
        }

        return undefined;
    }

    /**
     * Calculate the cost of transforming one node to another.
     * Insertion/deletion: cost = 1
     * Substitution: cost = 1 + (1 - similarity)
     *
     * @param node1 First NodeAttributes
     * @param node2 Second NodeAttributes
     * @returns Cost of transformation
     */
    protected calculateNodeCost(node1: NodeAttributes | undefined, node2: NodeAttributes | undefined): number {
        if (node1 == undefined || node2 == undefined) {
            return 1;
        }

        const type1 = node1.type as string;
        const type2 = node2.type as string;

        if (type1 !== type2) {
            return 2;
        }

        // For nodes with the same type, compare by attributes
        if (type1 === ModelTransformationElementType.NODE_MATCH) {
            return this.calculateMatchSimilarity(node1, node2);
        }

        if (type1 === ModelTransformationElementType.NODE_PATTERN_INSTANCE) {
            return this.calculatePatternInstanceSimilarity(node1, node2);
        }

        if (type1.startsWith("label:")) {
            return 1;
        }

        return 1;
    }

    /**
     * Calculate similarity between two match nodes.
     *
     * @param node1 First node
     * @param node2 Second node
     * @returns Cost based on similarity
     */
    private calculateMatchSimilarity(node1: NodeAttributes, node2: NodeAttributes): number {
        // Compare by label attribute if available
        const label1 = node1.label as string | undefined;
        const label2 = node2.label as string | undefined;

        if (label1 === label2) {
            return 1;
        }
        return 1.5;
    }

    /**
     * Calculate similarity between two pattern instance nodes.
     *
     * @param node1 First node
     * @param node2 Second node
     * @returns Cost based on similarity
     */
    private calculatePatternInstanceSimilarity(node1: NodeAttributes, node2: NodeAttributes): number {
        // Compare by name and type attributes
        const name1 = node1.name as string | undefined;
        const name2 = node2.name as string | undefined;
        const type1Attr = node1.typeName as string | undefined;
        const type2Attr = node2.typeName as string | undefined;

        if (name1 === name2 && type1Attr === type2Attr) {
            return 1;
        }

        if (name1 === name2 || type1Attr === type2Attr) {
            return 1.5;
        }

        return 2;
    }

    /**
     * Calculate the cost of transforming one edge to another.
     *
     * @param edge1 First EdgeAttributes
     * @param edge2 Second EdgeAttributes
     * @returns Cost of transformation
     */
    protected calculateEdgeCost(edge1: EdgeAttributes | undefined, edge2: EdgeAttributes | undefined): number {
        if (edge1 == undefined || edge2 == undefined) {
            return 1;
        }

        const type1 = edge1.type as string;
        const type2 = edge2.type as string;

        if (type1 !== type2) {
            return 2;
        }

        // Same type edges have low substitution cost
        return 1;
    }

    /**
     * Extracts the graph metadata from the source model.
     * Creates nodes for all control flow elements, match nodes, and pattern instances.
     * Creates edges for control flow connections and instance-to-match containment.
     *
     * @param sourceModel The source model to extract metadata from
     * @returns The computed graph metadata
     */
    protected extractGraphMetadata(sourceModel: ModelTransformationType): GraphMetadata {
        const nodes: Record<string, NodeMetadata> = {};
        const edges: Record<string, EdgeMetadata> = {};

        this.declaredInstances.clear();

        const idRegistry = new DefaultModelIdRegistry(sourceModel, this.modelIdProvider);

        // Create start node
        const startNodeId = "start";
        nodes[startNodeId] = {
            type: ModelTransformationElementType.NODE_START,
            attrs: {}
        };

        // Process statements
        const statements = sourceModel.statements ?? [];
        const result = this.processStatements(statements, startNodeId, idRegistry, nodes, edges);

        // If the last statement doesn't terminate, create an implicit end node
        if (result.lastNodeId != undefined) {
            const endNodeId = "implicit_end";
            nodes[endNodeId] = {
                type: ModelTransformationElementType.NODE_END,
                attrs: { kind: "stop" }
            };
            this.addControlFlowEdge(result.lastNodeId, endNodeId, undefined, edges);
        }

        return { nodes, edges };
    }

    /**
     * Processes a list of statements and extracts their metadata.
     */
    private processStatements(
        statements: BaseTransformationStatementType[],
        previousNodeId: string,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): { lastNodeId: string | undefined } {
        let currentNodeId: string | undefined = previousNodeId;

        for (const stmt of statements) {
            if (stmt == undefined || currentNodeId == undefined) {
                continue;
            }

            const result = this.processStatement(stmt, currentNodeId, idRegistry, nodes, edges);
            currentNodeId = result.lastNodeId;
        }

        return { lastNodeId: currentNodeId };
    }

    /**
     * Processes a single statement and extracts its metadata.
     */
    private processStatement(
        stmt: BaseTransformationStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): { lastNodeId: string | undefined } {
        if (this.reflection.isInstance(stmt, MatchStatement)) {
            return this.processMatchStatement(stmt as MatchStatementType, previousNodeId, idRegistry, nodes, edges);
        }
        if (this.reflection.isInstance(stmt, IfMatchStatement)) {
            return this.processIfMatchStatement(stmt as IfMatchStatementType, previousNodeId, idRegistry, nodes, edges);
        }
        if (this.reflection.isInstance(stmt, WhileMatchStatement)) {
            return this.processWhileMatchStatement(
                stmt as WhileMatchStatementType,
                previousNodeId,
                idRegistry,
                nodes,
                edges
            );
        }
        if (this.reflection.isInstance(stmt, UntilMatchStatement)) {
            return this.processUntilMatchStatement(
                stmt as UntilMatchStatementType,
                previousNodeId,
                idRegistry,
                nodes,
                edges
            );
        }
        if (this.reflection.isInstance(stmt, ForMatchStatement)) {
            return this.processForMatchStatement(
                stmt as ForMatchStatementType,
                previousNodeId,
                idRegistry,
                nodes,
                edges
            );
        }
        // Handle stop statements
        if ((stmt as { keyword?: string }).keyword === "stop" || (stmt as { keyword?: string }).keyword === "kill") {
            const nodeId = idRegistry.getId(stmt);
            nodes[nodeId] = {
                type: ModelTransformationElementType.NODE_END,
                attrs: { kind: (stmt as { keyword?: string }).keyword }
            };
            this.addControlFlowEdge(previousNodeId, nodeId, undefined, edges);
            return { lastNodeId: undefined };
        }

        return { lastNodeId: previousNodeId };
    }

    /**
     * Processes a match statement.
     */
    private processMatchStatement(
        stmt: MatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): { lastNodeId: string | undefined } {
        const nodeId = idRegistry.getId(stmt);
        this.addMatchNode(nodeId, "match", stmt.pattern, idRegistry, nodes, edges);
        this.addControlFlowEdge(previousNodeId, nodeId, undefined, edges);
        return { lastNodeId: nodeId };
    }

    /**
     * Processes an if-match statement.
     */
    private processIfMatchStatement(
        stmt: IfMatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): { lastNodeId: string | undefined } {
        const stmtId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(stmtId);
        const pattern = stmt.ifBlock?.pattern;

        this.addMatchNode(matchNodeId, "if match", pattern, idRegistry, nodes, edges);
        this.addControlFlowEdge(previousNodeId, matchNodeId, undefined, edges);

        // Process then branch
        const thenStatements = stmt.ifBlock?.thenBlock?.statements ?? [];
        let thenResult: { lastNodeId: string | undefined } = { lastNodeId: matchNodeId };
        if (thenStatements.length > 0) {
            const firstStmt = thenStatements[0];
            if (firstStmt != undefined) {
                const firstResult = this.processStatement(firstStmt, matchNodeId, idRegistry, nodes, edges);
                // Add edge from match to first statement with "then" label
                this.addControlFlowEdge(matchNodeId, this.getFirstNodeId(firstStmt, idRegistry), "then", edges);
                thenResult = this.processStatements(
                    thenStatements.slice(1),
                    firstResult.lastNodeId ?? matchNodeId,
                    idRegistry,
                    nodes,
                    edges
                );
                if (firstResult.lastNodeId == undefined) {
                    thenResult = { lastNodeId: undefined };
                }
            }
        }

        // Process else branch
        let elseResult: { lastNodeId: string | undefined } = { lastNodeId: matchNodeId };
        const elseStatements = stmt.elseBlock?.statements ?? [];
        if (elseStatements.length > 0) {
            const firstStmt = elseStatements[0];
            if (firstStmt != undefined) {
                const firstResult = this.processStatement(firstStmt, matchNodeId, idRegistry, nodes, edges);
                this.addControlFlowEdge(matchNodeId, this.getFirstNodeId(firstStmt, idRegistry), "else", edges);
                elseResult = this.processStatements(
                    elseStatements.slice(1),
                    firstResult.lastNodeId ?? matchNodeId,
                    idRegistry,
                    nodes,
                    edges
                );
                if (firstResult.lastNodeId == undefined) {
                    elseResult = { lastNodeId: undefined };
                }
            }
        }

        // Create merge node if needed
        if (thenResult.lastNodeId != undefined || elseResult.lastNodeId != undefined) {
            const mergeNodeId = ModelTransformationIdGenerator.mergeNode(stmtId);
            nodes[mergeNodeId] = {
                type: ModelTransformationElementType.NODE_MERGE,
                attrs: {}
            };

            if (thenResult.lastNodeId != undefined) {
                this.addControlFlowEdge(thenResult.lastNodeId, mergeNodeId, undefined, edges);
            }
            if (elseResult.lastNodeId != undefined && elseStatements.length > 0) {
                this.addControlFlowEdge(elseResult.lastNodeId, mergeNodeId, undefined, edges);
            } else if (elseResult.lastNodeId != undefined) {
                this.addControlFlowEdge(matchNodeId, mergeNodeId, "else", edges);
            }

            return { lastNodeId: mergeNodeId };
        }

        return { lastNodeId: undefined };
    }

    /**
     * Processes a while-match statement.
     */
    private processWhileMatchStatement(
        stmt: WhileMatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): { lastNodeId: string | undefined } {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);

        this.addMatchNode(matchNodeId, "while match", stmt.pattern, idRegistry, nodes, edges);
        this.addControlFlowEdge(previousNodeId, matchNodeId, undefined, edges);

        const doStatements = stmt.doBlock?.statements ?? [];
        if (doStatements.length > 0) {
            const firstStmt = doStatements[0];
            if (firstStmt != undefined) {
                const firstResult = this.processStatement(firstStmt, matchNodeId, idRegistry, nodes, edges);
                this.addControlFlowEdge(matchNodeId, this.getFirstNodeId(firstStmt, idRegistry), "match", edges);
                const doResult = this.processStatements(
                    doStatements.slice(1),
                    firstResult.lastNodeId ?? matchNodeId,
                    idRegistry,
                    nodes,
                    edges
                );
                // Loop back
                if (doResult.lastNodeId != undefined) {
                    this.addControlFlowEdge(doResult.lastNodeId, matchNodeId, undefined, edges);
                }
            }
        }

        return { lastNodeId: matchNodeId };
    }

    /**
     * Processes an until-match statement.
     */
    private processUntilMatchStatement(
        stmt: UntilMatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): { lastNodeId: string | undefined } {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);

        this.addMatchNode(matchNodeId, "until match", stmt.pattern, idRegistry, nodes, edges);
        this.addControlFlowEdge(previousNodeId, matchNodeId, undefined, edges);

        const doStatements = stmt.doBlock?.statements ?? [];
        if (doStatements.length > 0) {
            const firstStmt = doStatements[0];
            if (firstStmt != undefined) {
                const firstResult = this.processStatement(firstStmt, matchNodeId, idRegistry, nodes, edges);
                this.addControlFlowEdge(matchNodeId, this.getFirstNodeId(firstStmt, idRegistry), "no match", edges);
                const doResult = this.processStatements(
                    doStatements.slice(1),
                    firstResult.lastNodeId ?? matchNodeId,
                    idRegistry,
                    nodes,
                    edges
                );
                // Loop back
                if (doResult.lastNodeId != undefined) {
                    this.addControlFlowEdge(doResult.lastNodeId, matchNodeId, undefined, edges);
                }
            }
        }

        return { lastNodeId: matchNodeId };
    }

    /**
     * Processes a for-match statement.
     */
    private processForMatchStatement(
        stmt: ForMatchStatementType,
        previousNodeId: string,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): { lastNodeId: string | undefined } {
        const nodeId = idRegistry.getId(stmt);
        const matchNodeId = ModelTransformationIdGenerator.matchNode(nodeId);

        this.addMatchNode(matchNodeId, "for match", stmt.pattern, idRegistry, nodes, edges);
        this.addControlFlowEdge(previousNodeId, matchNodeId, undefined, edges);

        const doStatements = stmt.doBlock?.statements ?? [];
        if (doStatements.length > 0) {
            const firstStmt = doStatements[0];
            if (firstStmt != undefined) {
                const firstResult = this.processStatement(firstStmt, matchNodeId, idRegistry, nodes, edges);
                this.addControlFlowEdge(matchNodeId, this.getFirstNodeId(firstStmt, idRegistry), "each", edges);
                const doResult = this.processStatements(
                    doStatements.slice(1),
                    firstResult.lastNodeId ?? matchNodeId,
                    idRegistry,
                    nodes,
                    edges
                );
                // Loop back
                if (doResult.lastNodeId != undefined) {
                    this.addControlFlowEdge(doResult.lastNodeId, matchNodeId, undefined, edges);
                }
            }
        }

        return { lastNodeId: matchNodeId };
    }

    /**
     * Adds a match node with its pattern instances to the metadata.
     * Also adds edges from instances TO the match node for graph edit distance.
     */
    private addMatchNode(
        matchNodeId: string,
        label: string,
        pattern: PatternType | undefined,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): void {
        nodes[matchNodeId] = {
            type: ModelTransformationElementType.NODE_MATCH,
            attrs: { label }
        };

        // Collect instances declared in this pattern
        const localInstances = new Map<string, PatternObjectInstanceType>();
        const referencedInstances = new Set<string>();
        const deletedInstances = new Set<string>();

        if (pattern?.elements != undefined) {
            // First pass: collect what's declared, referenced, and deleted
            for (const element of pattern.elements) {
                if (this.reflection.isInstance(element, PatternObjectInstance)) {
                    const instance = element as PatternObjectInstanceType;
                    if (instance.name) {
                        localInstances.set(instance.name, instance);
                    }
                }
                if (this.reflection.isInstance(element, PatternObjectInstanceReference)) {
                    const ref = element as PatternObjectInstanceReferenceType;
                    const instanceName = ref.instance?.ref?.name;
                    if (instanceName) {
                        referencedInstances.add(instanceName);
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
                    // Collect referenced instances from link ends
                    const sourceInstanceName = link.source?.object?.ref?.name;
                    const targetInstanceName = link.target?.object?.ref?.name;
                    if (sourceInstanceName && !localInstances.has(sourceInstanceName)) {
                        referencedInstances.add(sourceInstanceName);
                    }
                    if (targetInstanceName && !localInstances.has(targetInstanceName)) {
                        referencedInstances.add(targetInstanceName);
                    }
                }
            }

            // Add nodes for instances declared in this pattern
            for (const [instanceName, instance] of localInstances) {
                const instanceNodeId = idRegistry.getId(instance);
                const typeName =
                    instance.class?.$refText ?? (instance.class?.ref as { name?: string } | undefined)?.name;

                nodes[instanceNodeId] = {
                    type: ModelTransformationElementType.NODE_PATTERN_INSTANCE,
                    attrs: {
                        name: instanceName,
                        typeName: typeName ?? undefined
                    }
                };

                // CRITICAL: Add edge from instance TO match for graph edit distance
                // This prevents instances from being matched with instances in different match nodes
                this.addInstanceToMatchEdge(instanceNodeId, matchNodeId, edges);

                // Register instance for future references
                this.declaredInstances.set(instanceName, instanceNodeId);
            }

            // Add nodes for referenced instances (from previous matches)
            for (const instanceName of referencedInstances) {
                if (!localInstances.has(instanceName) && !deletedInstances.has(instanceName)) {
                    const previousNodeId = this.declaredInstances.get(instanceName);
                    if (previousNodeId != undefined) {
                        const refNodeId = ModelTransformationIdGenerator.referencedInstance(matchNodeId, instanceName);
                        nodes[refNodeId] = {
                            type: ModelTransformationElementType.NODE_PATTERN_INSTANCE,
                            attrs: {
                                name: instanceName,
                                isReference: true
                            }
                        };

                        // Add edge from referenced instance TO match
                        this.addInstanceToMatchEdge(refNodeId, matchNodeId, edges);
                    }
                }
            }

            // Add pattern link edges
            for (const element of pattern.elements) {
                if (this.reflection.isInstance(element, PatternLink)) {
                    this.addPatternLinkEdge(element as PatternLinkType, matchNodeId, idRegistry, nodes, edges);
                }
            }
        }
    }

    /**
     * Adds an edge from a pattern instance TO its containing match node.
     * These edges are NOT important for layout but are NECESSARY for graph edit distance
     * to calculate a useful minimum and prevent undesired matches between instances
     * in different match nodes.
     */
    private addInstanceToMatchEdge(
        instanceNodeId: string,
        matchNodeId: string,
        edges: Record<string, EdgeMetadata>
    ): void {
        const edgeId = ModelTransformationIdGenerator.instanceToMatchEdge(instanceNodeId, matchNodeId);
        edges[edgeId] = {
            type: ModelTransformationElementType.EDGE_CONTROL_FLOW, // Reuse edge type
            from: instanceNodeId,
            to: matchNodeId,
            attrs: { isContainment: true }
        };
    }

    /**
     * Adds a pattern link edge.
     */
    private addPatternLinkEdge(
        link: PatternLinkType,
        matchNodeId: string,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): void {
        const edgeId = idRegistry.getId(link);
        const sourceInstanceName = link.source?.object?.ref?.name;
        const targetInstanceName = link.target?.object?.ref?.name;

        if (sourceInstanceName == undefined || targetInstanceName == undefined) {
            return;
        }

        const sourceId = this.resolveInstanceNodeId(sourceInstanceName, matchNodeId);
        const targetId = this.resolveInstanceNodeId(targetInstanceName, matchNodeId);

        if (sourceId == undefined || targetId == undefined) {
            return;
        }

        edges[edgeId] = {
            type: ModelTransformationElementType.EDGE_PATTERN_LINK,
            from: sourceId,
            to: targetId,
            attrs: {
                sourceProperty: link.source?.property?.$refText,
                targetProperty: link.target?.property?.$refText
            }
        };

        // Add link end nodes if properties are specified
        if (link.source?.property != undefined) {
            nodes[ModelTransformationIdGenerator.patternLinkSourceEndNode(edgeId)] = {
                type: ModelTransformationElementType.NODE_PATTERN_LINK_END,
                attrs: {}
            };
        }
        if (link.target?.property != undefined) {
            nodes[ModelTransformationIdGenerator.patternLinkTargetEndNode(edgeId)] = {
                type: ModelTransformationElementType.NODE_PATTERN_LINK_END,
                attrs: {}
            };
        }
    }

    /**
     * Resolves a pattern instance node ID by name.
     */
    private resolveInstanceNodeId(instanceName: string, matchNodeId: string): string | undefined {
        // Check if it's in declared instances
        const declaredId = this.declaredInstances.get(instanceName);
        if (declaredId != undefined) {
            return declaredId;
        }
        // Check if it's a reference in the current match
        return ModelTransformationIdGenerator.referencedInstance(matchNodeId, instanceName);
    }

    /**
     * Gets the first node ID for a statement (for creating edges).
     */
    private getFirstNodeId(stmt: BaseTransformationStatementType, idRegistry: ModelIdRegistry): string {
        if (this.reflection.isInstance(stmt, IfMatchStatement)) {
            return ModelTransformationIdGenerator.matchNode(idRegistry.getId(stmt));
        }
        if (this.reflection.isInstance(stmt, WhileMatchStatement)) {
            return ModelTransformationIdGenerator.matchNode(idRegistry.getId(stmt));
        }
        if (this.reflection.isInstance(stmt, UntilMatchStatement)) {
            return ModelTransformationIdGenerator.matchNode(idRegistry.getId(stmt));
        }
        if (this.reflection.isInstance(stmt, ForMatchStatement)) {
            return ModelTransformationIdGenerator.matchNode(idRegistry.getId(stmt));
        }
        return idRegistry.getId(stmt);
    }

    /**
     * Adds a control flow edge between nodes.
     */
    private addControlFlowEdge(
        sourceId: string,
        targetId: string,
        label: string | undefined,
        edges: Record<string, EdgeMetadata>
    ): void {
        const edgeId = ModelTransformationIdGenerator.controlFlowEdge(sourceId, targetId);
        edges[edgeId] = {
            type: ModelTransformationElementType.EDGE_CONTROL_FLOW,
            from: sourceId,
            to: targetId,
            attrs: { label }
        };

        // Add label node if specified - not needed for metadata
    }
}
