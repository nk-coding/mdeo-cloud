/**
 * Provides control flow analysis for detecting references to possibly deleted object instances
 * in model transformations. This analysis is critical for validation because:
 *
 * 1. Object instances can be deleted using the "delete" modifier in patterns
 * 2. References to deleted instances after deletion are invalid
 * 3. Control flow affects deletion visibility (loops can delete instances used in later iterations)
 *
 * ## Algorithm Overview
 *
 * The analyzer builds a map of "possibly deleted" instances at each point in the program.
 * For each identifier expression, we check if the referenced object instance is in this set.
 *
 * ## Control Flow Handling
 *
 * - **Sequential statements**: Deletions accumulate linearly
 * - **If branches**: Union of deletions from all branches (conservative)
 * - **Loops (while/until/for/while-match/etc)**: All deletions in the loop body are visible
 *   at the start of subsequent iterations, so they're added to the pre-loop state
 */
import type { AstNode, LangiumSharedCoreServices, ValidationAcceptor } from "langium";
import type { AstReflection } from "@mdeo/language-common";
import { sharedImport } from "@mdeo/language-shared";
import {
    ModelTransformation,
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    IfExpressionStatement,
    WhileExpressionStatement,
    PatternObjectInstance,
    statementTypes,
    type ModelTransformationType,
    type PatternType,
    type TransformationStatementType,
    type PatternObjectInstanceType,
    type IfExpressionStatementType,
    type WhileExpressionStatementType,
    type StatementsScopeType,
    type IfMatchStatementType
} from "../grammar/modelTransformationTypes.js";

const { AstUtils } = sharedImport("langium");

/**
 * Represents the deletion state at a specific program point.
 * Contains the set of object instance names that are possibly deleted.
 */
interface DeletionState {
    /**
     * Set of object instance names that are possibly deleted at this point.
     */
    possiblyDeleted: Set<string>;
}

/**
 * Analysis result for a single transformation document.
 */
interface TransformationAnalysis {
    /**
     * Maps AST nodes to their pre-execution deletion state.
     */
    nodeStates: Map<AstNode, DeletionState>;

    /**
     * Maps object instance names to their AST node definitions.
     */
    instanceDefinitions: Map<string, PatternObjectInstanceType>;
}

/**
 * Identifier expression type from the generated expression types.
 */
type IdentifierExpressionType = AstNode & { name: string };

/**
 * Analyzes model transformations for references to possibly deleted object instances.
 * Uses control flow analysis to track deletions through branches and loops.
 */
export class DeletedInstanceAnalyzer {
    private readonly reflection: AstReflection;
    private readonly analysisCache: WeakMap<ModelTransformationType, TransformationAnalysis>;

    /**
     * Creates a new DeletedInstanceAnalyzer.
     *
     * @param reflection The AST reflection service for type checking.
     * @param _sharedServices The shared Langium services (unused, kept for API compatibility).
     */
    constructor(reflection: AstReflection, _sharedServices: LangiumSharedCoreServices) {
        this.reflection = reflection;
        this.analysisCache = new WeakMap();
    }

    /**
     * Checks if an identifier expression references a possibly deleted object instance.
     *
     * @param identifier The identifier expression to check.
     * @param accept The validation acceptor for reporting errors.
     */
    validateIdentifierReference(identifier: IdentifierExpressionType, accept: ValidationAcceptor): void {
        const transformation = this.getContainingTransformation(identifier);
        if (!transformation) {
            return;
        }

        const analysis = this.getOrCreateAnalysis(transformation);
        if (!analysis) {
            return;
        }

        const instanceDef = analysis.instanceDefinitions.get(identifier.name);
        if (!instanceDef) {
            return;
        }

        const state = this.findNodeState(identifier, analysis);
        if (state && state.possiblyDeleted.has(identifier.name)) {
            accept("error", `Reference to '${identifier.name}' which may have been deleted.`, {
                node: identifier,
                property: "name"
            });
        }
    }

    /**
     * Gets or creates the analysis for a transformation document.
     *
     * @param transformation The transformation to analyze.
     * @returns The analysis result, or undefined if analysis fails.
     */
    private getOrCreateAnalysis(transformation: ModelTransformationType): TransformationAnalysis | undefined {
        let analysis = this.analysisCache.get(transformation);
        if (analysis) {
            return analysis;
        }

        analysis = this.analyzeTransformation(transformation);
        this.analysisCache.set(transformation, analysis);
        return analysis;
    }

    /**
     * Analyzes a transformation to build the deletion state map.
     *
     * @param transformation The transformation to analyze.
     * @returns The complete analysis result.
     */
    private analyzeTransformation(transformation: ModelTransformationType): TransformationAnalysis {
        const analysis: TransformationAnalysis = {
            nodeStates: new Map(),
            instanceDefinitions: new Map()
        };

        this.collectInstanceDefinitions(transformation, analysis.instanceDefinitions);
        const initialState: DeletionState = { possiblyDeleted: new Set() };
        this.analyzeStatements(transformation.statements ?? [], initialState, analysis);

        return analysis;
    }

    /**
     * Collects all object instance definitions from the transformation.
     *
     * @param transformation The transformation to collect from.
     * @param definitions Map to store instance definitions.
     */
    private collectInstanceDefinitions(
        transformation: ModelTransformationType,
        definitions: Map<string, PatternObjectInstanceType>
    ): void {
        for (const node of AstUtils.streamAllContents(transformation)) {
            if (this.reflection.isInstance(node, PatternObjectInstance)) {
                const instance = node as PatternObjectInstanceType;
                if (instance.name) {
                    definitions.set(instance.name, instance);
                }
            }
        }
    }

    /**
     * Analyzes a list of statements, tracking deletion state.
     *
     * @param statements The statements to analyze.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The deletion state after all statements.
     */
    private analyzeStatements(
        statements: TransformationStatementType[],
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        let state = currentState;

        for (const statement of statements) {
            analysis.nodeStates.set(statement, { possiblyDeleted: new Set(state.possiblyDeleted) });
            state = this.analyzeStatement(statement, state, analysis);
        }

        return state;
    }

    /**
     * Analyzes a single statement, dispatching to the appropriate handler.
     *
     * @param statement The statement to analyze.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The deletion state after the statement.
     */
    private analyzeStatement(
        statement: TransformationStatementType,
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        if (this.reflection.isInstance(statement, MatchStatement)) {
            return this.analyzeMatchStatement(statement, currentState, analysis);
        }
        if (this.reflection.isInstance(statement, IfMatchStatement)) {
            return this.analyzeIfMatchStatement(statement, currentState, analysis);
        }
        if (this.reflection.isInstance(statement, WhileMatchStatement)) {
            return this.analyzeWhileMatchStatement(statement, currentState, analysis);
        }
        if (this.reflection.isInstance(statement, UntilMatchStatement)) {
            return this.analyzeUntilMatchStatement(statement, currentState, analysis);
        }
        if (this.reflection.isInstance(statement, ForMatchStatement)) {
            return this.analyzeForMatchStatement(statement, currentState, analysis);
        }
        if (this.reflection.isInstance(statement, IfExpressionStatement)) {
            return this.analyzeIfExpressionStatement(statement, currentState, analysis);
        }
        if (this.reflection.isInstance(statement, WhileExpressionStatement)) {
            return this.analyzeWhileExpressionStatement(statement, currentState, analysis);
        }

        return currentState;
    }

    /**
     * Analyzes a match statement for deletions.
     *
     * @param statement The match statement.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The updated deletion state.
     */
    private analyzeMatchStatement(
        statement: { pattern: PatternType },
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        this.recordPatternExpressionStates(statement.pattern, currentState, analysis);
        return this.applyPatternDeletions(statement.pattern, currentState);
    }

    /**
     * Analyzes an if-match statement with then/else branches.
     *
     * @param statement The if-match statement.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The union of deletion states from all branches.
     */
    private analyzeIfMatchStatement(
        statement: IfMatchStatementType,
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        this.recordPatternExpressionStates(statement.ifBlock.pattern, currentState, analysis);
        const afterPatternState = this.applyPatternDeletions(statement.ifBlock.pattern, currentState);

        const thenState = this.analyzeStatements(
            statement.ifBlock.thenBlock.statements ?? [],
            afterPatternState,
            analysis
        );

        const elseState = statement.elseBlock
            ? this.analyzeStatements(statement.elseBlock.statements ?? [], currentState, analysis)
            : currentState;

        return this.unionStates(thenState, elseState);
    }

    /**
     * Analyzes a while-match statement with loop body.
     * Deletions in the loop body affect subsequent iterations.
     *
     * @param statement The while-match statement.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The deletion state after the loop.
     */
    private analyzeWhileMatchStatement(
        statement: { pattern: PatternType; doBlock?: StatementsScopeType },
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        return this.analyzeLoopWithPattern(statement.pattern, statement.doBlock, currentState, analysis);
    }

    /**
     * Analyzes an until-match statement with loop body.
     *
     * @param statement The until-match statement.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The deletion state after the loop.
     */
    private analyzeUntilMatchStatement(
        statement: { pattern: PatternType; doBlock?: StatementsScopeType },
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        return this.analyzeLoopWithPattern(statement.pattern, statement.doBlock, currentState, analysis);
    }

    /**
     * Analyzes a for-match statement with loop body.
     *
     * @param statement The for-match statement.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The deletion state after the loop.
     */
    private analyzeForMatchStatement(
        statement: { pattern: PatternType; doBlock?: StatementsScopeType },
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        return this.analyzeLoopWithPattern(statement.pattern, statement.doBlock, currentState, analysis);
    }

    /**
     * Analyzes a loop with a pattern, handling the control flow.
     * For loops, deletions anywhere in the body can affect the next iteration.
     *
     * @param pattern The loop pattern.
     * @param block The loop body.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The deletion state after the loop.
     */
    private analyzeLoopWithPattern(
        pattern: PatternType,
        block: StatementsScopeType | undefined,
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        const patternDeletions = this.collectPatternDeletions(pattern);
        const bodyDeletions = block ? this.collectAllDeletionsInBlock(block) : new Set<string>();

        const allLoopDeletions = new Set([...patternDeletions, ...bodyDeletions]);
        const loopEntryState = this.unionWithNames(currentState, allLoopDeletions);

        this.recordPatternExpressionStates(pattern, loopEntryState, analysis);
        const afterPatternState = this.applyPatternDeletions(pattern, loopEntryState);

        if (block) {
            this.analyzeStatements(block.statements ?? [], afterPatternState, analysis);
        }

        return this.unionWithNames(currentState, allLoopDeletions);
    }

    /**
     * Analyzes an if-expression statement with multiple branches.
     *
     * @param statement The if-expression statement.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The union of deletion states from all branches.
     */
    private analyzeIfExpressionStatement(
        statement: IfExpressionStatementType,
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        this.recordExpressionStates(statement.condition, currentState, analysis);

        const branchStates: DeletionState[] = [];

        const thenState = statement.thenBlock
            ? this.analyzeStatements(statement.thenBlock.statements ?? [], currentState, analysis)
            : currentState;
        branchStates.push(thenState);

        for (const branch of statement.elseIfBranches ?? []) {
            this.recordExpressionStates(branch.condition, currentState, analysis);
            const branchState = branch.block
                ? this.analyzeStatements(branch.block.statements ?? [], currentState, analysis)
                : currentState;
            branchStates.push(branchState);
        }

        if (statement.elseBlock) {
            const elseState = this.analyzeStatements(statement.elseBlock.statements ?? [], currentState, analysis);
            branchStates.push(elseState);
        } else {
            branchStates.push(currentState);
        }

        return this.unionAllStates(branchStates);
    }

    /**
     * Analyzes a while-expression statement with loop body.
     *
     * @param statement The while-expression statement.
     * @param currentState The current deletion state.
     * @param analysis The analysis result to update.
     * @returns The deletion state after the loop.
     */
    private analyzeWhileExpressionStatement(
        statement: WhileExpressionStatementType,
        currentState: DeletionState,
        analysis: TransformationAnalysis
    ): DeletionState {
        const bodyDeletions = statement.block ? this.collectAllDeletionsInBlock(statement.block) : new Set<string>();

        const loopEntryState = this.unionWithNames(currentState, bodyDeletions);

        this.recordExpressionStates(statement.condition, loopEntryState, analysis);

        if (statement.block) {
            this.analyzeStatements(statement.block.statements ?? [], loopEntryState, analysis);
        }

        return this.unionWithNames(currentState, bodyDeletions);
    }

    /**
     * Records deletion state for all expressions in a pattern.
     *
     * @param pattern The pattern containing expressions.
     * @param state The current deletion state.
     * @param analysis The analysis result to update.
     */
    private recordPatternExpressionStates(
        pattern: PatternType | undefined,
        state: DeletionState,
        analysis: TransformationAnalysis
    ): void {
        for (const element of pattern?.elements ?? []) {
            if (this.reflection.isInstance(element, PatternObjectInstance)) {
                const obj = element as PatternObjectInstanceType;
                for (const prop of obj.properties ?? []) {
                    if (prop.value) {
                        this.recordExpressionStates(prop.value, state, analysis);
                    }
                }
            } else if (this.reflection.isInstance(element, statementTypes.whereClauseType)) {
                if (element.expression) {
                    this.recordExpressionStates(element.expression, state, analysis);
                }
            } else if (this.reflection.isInstance(element, statementTypes.patternVariableType)) {
                if (element.value) {
                    this.recordExpressionStates(element.value, state, analysis);
                }
            }
        }
    }

    /**
     * Records deletion state for an expression and all its children.
     *
     * @param expression The expression to record states for.
     * @param state The current deletion state.
     * @param analysis The analysis result to update.
     */
    private recordExpressionStates(expression: AstNode, state: DeletionState, analysis: TransformationAnalysis): void {
        analysis.nodeStates.set(expression, { possiblyDeleted: new Set(state.possiblyDeleted) });

        for (const child of AstUtils.streamAllContents(expression)) {
            analysis.nodeStates.set(child, { possiblyDeleted: new Set(state.possiblyDeleted) });
        }
    }

    /**
     * Collects deletions from a pattern (objects and links with delete modifier).
     *
     * @param pattern The pattern to collect deletions from.
     * @returns Set of deleted instance names.
     */
    private collectPatternDeletions(pattern: PatternType | undefined): Set<string> {
        const deletions = new Set<string>();

        for (const element of pattern?.elements ?? []) {
            if (this.reflection.isInstance(element, PatternObjectInstance)) {
                const obj = element as PatternObjectInstanceType;
                if (obj.modifier?.modifier === "delete" && obj.name) {
                    deletions.add(obj.name);
                }
            }
        }

        return deletions;
    }

    /**
     * Applies pattern deletions to a state, returning a new state.
     *
     * @param pattern The pattern with possible deletions.
     * @param currentState The current deletion state.
     * @returns The new deletion state with pattern deletions added.
     */
    private applyPatternDeletions(pattern: PatternType | undefined, currentState: DeletionState): DeletionState {
        const deletions = this.collectPatternDeletions(pattern);
        if (deletions.size === 0) {
            return currentState;
        }

        return {
            possiblyDeleted: new Set([...currentState.possiblyDeleted, ...deletions])
        };
    }

    /**
     * Collects all deletions in a block (including nested statements).
     *
     * @param block The block to analyze.
     * @returns Set of all deleted instance names.
     */
    private collectAllDeletionsInBlock(block: StatementsScopeType): Set<string> {
        const deletions = new Set<string>();

        for (const node of AstUtils.streamAllContents(block)) {
            if (this.reflection.isInstance(node, PatternObjectInstance)) {
                const obj = node as PatternObjectInstanceType;
                if (obj.modifier?.modifier === "delete" && obj.name) {
                    deletions.add(obj.name);
                }
            }
        }

        return deletions;
    }

    /**
     * Computes the union of two deletion states.
     *
     * @param state1 First state.
     * @param state2 Second state.
     * @returns Union of both states.
     */
    private unionStates(state1: DeletionState, state2: DeletionState): DeletionState {
        return {
            possiblyDeleted: new Set([...state1.possiblyDeleted, ...state2.possiblyDeleted])
        };
    }

    /**
     * Computes the union of a state with additional names.
     *
     * @param state The base state.
     * @param names Additional names to add.
     * @returns Union of state and names.
     */
    private unionWithNames(state: DeletionState, names: Set<string>): DeletionState {
        return {
            possiblyDeleted: new Set([...state.possiblyDeleted, ...names])
        };
    }

    /**
     * Computes the union of multiple deletion states.
     *
     * @param states The states to union.
     * @returns Union of all states.
     */
    private unionAllStates(states: DeletionState[]): DeletionState {
        const combined = new Set<string>();
        for (const state of states) {
            for (const name of state.possiblyDeleted) {
                combined.add(name);
            }
        }
        return { possiblyDeleted: combined };
    }

    /**
     * Finds the deletion state for a node by looking up ancestors.
     *
     * @param node The node to find state for.
     * @param analysis The analysis containing node states.
     * @returns The deletion state, or undefined if not found.
     */
    private findNodeState(node: AstNode, analysis: TransformationAnalysis): DeletionState | undefined {
        let current: AstNode | undefined = node;

        while (current) {
            const state = analysis.nodeStates.get(current);
            if (state) {
                return state;
            }
            current = current.$container;
        }

        return undefined;
    }

    /**
     * Gets the containing ModelTransformation for an AST node.
     *
     * @param node The node to find the container for.
     * @returns The containing transformation, or undefined.
     */
    private getContainingTransformation(node: AstNode): ModelTransformationType | undefined {
        return AstUtils.getContainerOfType(node, (n) => this.reflection.isInstance(n, ModelTransformation));
    }
}
