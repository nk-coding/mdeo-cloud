/**
 * @module ModelTransformationTypirScopeProvider
 *
 * This module provides scoping rules for type inference in the Model Transformation language.
 * Scoping determines which variables, pattern object instances, and other declarations are
 * visible at each point in a transformation.
 *
 * ## Scoping Architecture
 *
 * The scope provider uses a hierarchical scope model where each scope-creating node
 * establishes a new scope that can access entries from parent scopes. Scopes are created
 * lazily during type inference and cached for performance.
 *
 * ## Scope-Creating Nodes
 *
 * The following AST nodes create their own scope:
 *
 * 1. **ModelTransformation** (root scope)
 *    - Contains all top-level match statements and their declared variables
 *    - Variables from consecutive match statements are visible to subsequent statements
 *
 * 2. **StatementsScope** (block scope)
 *    - Contains variables from match statements within the block
 *    - Inherits from parent scope
 *    - Operates identically to ModelTransformation but for nested blocks
 *
 * 3. **IfMatchCondition** (condition scope)
 *    - Inner node wrapping pattern and then block for if-match statements
 *    - Pattern variables are visible only within the then block
 *    - Creates isolated scope that prevents pattern variables from leaking to else block
 *
 * 4. **WhileMatchStatement / ForMatchStatement**
 *    - Pattern variables are visible only within the associated do block
 *    - Creates isolated scope for pattern bindings
 *
 * 5. **LambdaExpression**
 *    - Creates a specialized scope for lambda parameters
 *    - Infers parameter types from the calling context
 *
 * ## Key Scoping Rules
 *
 * - **Match statements do NOT create their own scope** - Consecutive matches in the same
 *   block share variables, allowing later matches to reference earlier bindings.
 *
 * - **Control flow match statements CREATE their own scope** - If/while/for match
 *   statements isolate their pattern bindings to prevent leakage.
 *
 * - **IfMatchCondition isolates pattern scope** - The new inner node ensures the then block
 *   has access to pattern variables while the else block does not.
 *
 * - **If/While expression statements do NOT create scope** - These are pure control flow
 *   and don't introduce variable bindings.
 *
 * - **Object instances are scoped by name** - Pattern object instances (e.g., `obj: MyClass`)
 *   are available in scope under their declared name.
 *
 * - **Lambda parameters shadow outer variables** - Lambda parameter names take precedence
 *   over any same-named variables in enclosing scopes.
 *
 * ## Position-Based Visibility
 *
 * Variables become visible only after their declaration point within a scope. This is
 * tracked via the `position` field in scope entries, allowing forward reference detection.
 */
import {
    BaseScopeProvider,
    DefaultScope,
    inferLambdaTypeFromContext,
    type BoundScope,
    type Scope,
    type ScopeEntry,
    type ScopeLocalInitialization,
    type ExpressionTypirServices,
    type LambdaTypeInferenceResult
} from "@mdeo/language-expression";
import type { TypirLangiumSpecifics } from "typir-langium";
import type { AstReflection } from "@mdeo/language-common";
import type { TypeInferenceCollector } from "typir";
import {
    ModelTransformation,
    StatementsScope,
    IfMatchConditionAndBlock,
    WhileMatchStatement,
    ForMatchStatement,
    LambdaExpression,
    expressionTypes,
    statementTypes,
    type ModelTransformationType,
    type StatementsScopeType,
    type IfMatchConditionAndBlockType,
    type WhileMatchStatementType,
    type ForMatchStatementType,
    type LambdaExpressionType,
    type PatternType,
    type PatternObjectInstanceType,
    type PatternVariableType
} from "../../grammar/modelTransformationTypes.js";
import { ModelTransformationLambdaScope } from "./modelTransformationLambdaScope.js";

/**
 * Type definition for scope creator functions.
 * These functions create a scope for a specific node type.
 */
type ScopeCreator<T> = (
    node: T,
    parentScope: BoundScope<TypirLangiumSpecifics> | undefined
) => Scope<TypirLangiumSpecifics>;

/**
 * Typir scope provider for the Model Transformation language.
 * Provides scoping for type inference within transformation constructs.
 *
 * Uses a registry-based dispatch pattern for creating scopes based on node type.
 * See module documentation for detailed scoping rules.
 */
export class ModelTransformationTypirScopeProvider extends BaseScopeProvider<
    TypirLangiumSpecifics,
    ExpressionTypirServices<TypirLangiumSpecifics>
> {
    /**
     * AST reflection service for type checking.
     */
    protected readonly reflection: AstReflection;

    /**
     * Type inference collector for inferring types.
     */
    protected readonly inference: TypeInferenceCollector<TypirLangiumSpecifics>;

    /**
     * Registry mapping node types to their scope creator functions.
     * This provides a cleaner dispatch pattern than if-else chains.
     */
    private readonly scopeCreatorRegistry: Map<string, ScopeCreator<any>>;

    /**
     * Creates an instance of ModelTransformationTypirScopeProvider.
     *
     * @param typir The typir services for Model Transformation.
     */
    constructor(typir: ExpressionTypirServices<TypirLangiumSpecifics>) {
        super(typir);
        this.reflection = typir.langium.LangiumServices.AstReflection;
        this.inference = typir.Inference;
        this.scopeCreatorRegistry = this.initializeScopeCreatorRegistry();
    }

    /**
     * Initializes the scope creator registry with all supported node types.
     *
     * @returns A map from node type names to scope creator functions.
     */
    private initializeScopeCreatorRegistry(): Map<string, ScopeCreator<any>> {
        return new Map<string, ScopeCreator<any>>([
            [ModelTransformation.name, (node, parent) => this.createTransformationScope(node, parent)],
            [StatementsScope.name, (node, parent) => this.createStatementsScopeScope(node, parent)],
            [IfMatchConditionAndBlock.name, (node, parent) => this.createIfMatchConditionScope(node, parent)],
            [WhileMatchStatement.name, (node, parent) => this.createWhileMatchScope(node, parent)],
            [ForMatchStatement.name, (node, parent) => this.createForMatchScope(node, parent)],
            [LambdaExpression.name, (node, parent) => this.createLambdaScope(node, parent)]
        ]);
    }

    /**
     * Determines if a node creates a new scope for type inference.
     *
     * @param node The AST node to check.
     * @returns True if the node creates a new scope.
     */
    override isScopeRelevantNode(node: TypirLangiumSpecifics["LanguageType"]): boolean {
        return (
            this.reflection.isInstance(node, ModelTransformation) ||
            this.reflection.isInstance(node, StatementsScope) ||
            this.reflection.isInstance(node, IfMatchConditionAndBlock) ||
            this.reflection.isInstance(node, WhileMatchStatement) ||
            this.reflection.isInstance(node, ForMatchStatement) ||
            this.reflection.isInstance(node, LambdaExpression)
        );
    }

    /**
     * Creates a scope for a given node using the registry-based dispatch pattern.
     *
     * @param languageNode The AST node to create a scope for.
     * @param parentScope The parent scope.
     * @returns A new scope for the node.
     */
    override createScope(
        languageNode: TypirLangiumSpecifics["LanguageType"],
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined
    ): Scope<TypirLangiumSpecifics> {
        const nodeType = languageNode.$type;
        const scopeCreator = this.scopeCreatorRegistry.get(nodeType);

        if (scopeCreator != undefined) {
            return scopeCreator(languageNode, parentScope);
        }

        return this.createEmptyScope(parentScope, languageNode);
    }

    /**
     * Creates a scope for the root ModelTransformation node.
     * This scope contains all top-level pattern matches and their variables.
     *
     * @param node The ModelTransformation node.
     * @param parentScope The parent scope.
     * @returns A new scope for the transformation.
     */
    private createTransformationScope(
        node: ModelTransformationType,
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined
    ): Scope<TypirLangiumSpecifics> {
        return new DefaultScope<TypirLangiumSpecifics>(
            parentScope,
            (scope) => this.getStatementsScopeEntries(node, scope),
            () => [],
            this.getStatementsScopeInitializations(node),
            node
        );
    }

    /**
     * Gets scope entries for the root transformation / statements scope.
     * Collects object instances and pattern variables from top-level match statements.
     *
     * @param node The transformation node.
     * @param scope The current scope.
     * @returns Array of scope entries.
     */
    private getStatementsScopeEntries(
        node: ModelTransformationType | StatementsScopeType,
        scope: Scope<TypirLangiumSpecifics>
    ): ScopeEntry<TypirLangiumSpecifics>[] {
        const entries: ScopeEntry<TypirLangiumSpecifics>[] = [];

        for (let i = 0; i < node.statements.length; i++) {
            const statement = node.statements[i];
            if (
                this.reflection.isInstance(statement, statementTypes.matchStatementType) ||
                this.reflection.isInstance(statement, statementTypes.untilMatchStatementType)
            ) {
                this.collectPatternEntries(statement.pattern, i, scope, entries);
            }
        }
        return entries;
    }

    /**
     * Gets local initializations for the root transformation / statements scope.
     *
     * @param node The transformation node.
     * @returns Array of local initialization records.
     */
    private getStatementsScopeInitializations(
        node: ModelTransformationType | StatementsScopeType
    ): ScopeLocalInitialization[] {
        const initializations: ScopeLocalInitialization[] = [];

        for (let i = 0; i < node.statements.length; i++) {
            const statement = node.statements[i];
            if (
                this.reflection.isInstance(statement, statementTypes.matchStatementType) ||
                this.reflection.isInstance(statement, statementTypes.untilMatchStatementType)
            ) {
                this.collectPatternInitializations(statement.pattern, i, initializations);
            }
        }

        return initializations;
    }

    /**
     * Creates a scope for a StatementsScope block.
     * This includes variables from preceding match statements in the same block.
     *
     * @param node The StatementsScope node.
     * @param parentScope The parent scope.
     * @returns A new scope for the statements block.
     */
    private createStatementsScopeScope(
        node: StatementsScopeType,
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined
    ): Scope<TypirLangiumSpecifics> {
        return new DefaultScope<TypirLangiumSpecifics>(
            parentScope,
            (scope) => this.getStatementsScopeEntries(node, scope),
            () => [],
            this.getStatementsScopeInitializations(node),
            node
        );
    }

    /**
     * Creates a scope for an if-match condition (inner node).
     * Pattern variables are visible in the then block but not in the else block.
     *
     * @param node The if-match condition node.
     * @param parentScope The parent scope.
     * @returns A new scope for the if-match condition.
     */
    private createIfMatchConditionScope(
        node: IfMatchConditionAndBlockType,
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined
    ): Scope<TypirLangiumSpecifics> {
        return new DefaultScope<TypirLangiumSpecifics>(
            parentScope,
            (scope) => this.collectPatternEntriesFromPattern(node.pattern, scope),
            () => [],
            this.collectPatternInitializationsFromPattern(node.pattern),
            node
        );
    }

    /**
     * Creates a scope for a while-match statement.
     * Pattern variables are visible in the do block.
     *
     * @param node The while-match statement node.
     * @param parentScope The parent scope.
     * @returns A new scope for the while-match statement.
     */
    private createWhileMatchScope(
        node: WhileMatchStatementType,
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined
    ): Scope<TypirLangiumSpecifics> {
        return new DefaultScope<TypirLangiumSpecifics>(
            parentScope,
            (scope) => this.collectPatternEntriesFromPattern(node.pattern, scope),
            () => [],
            this.collectPatternInitializationsFromPattern(node.pattern),
            node
        );
    }

    /**
     * Creates a scope for a for-match statement.
     * Pattern variables are visible in the do block.
     *
     * @param node The for-match statement node.
     * @param parentScope The parent scope.
     * @returns A new scope for the for-match statement.
     */
    private createForMatchScope(
        node: ForMatchStatementType,
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined
    ): Scope<TypirLangiumSpecifics> {
        return new DefaultScope<TypirLangiumSpecifics>(
            parentScope,
            (scope) => this.collectPatternEntriesFromPattern(node.pattern, scope),
            () => [],
            this.collectPatternInitializationsFromPattern(node.pattern),
            node
        );
    }

    /**
     * Creates a scope for a lambda expression.
     * This scope contains the lambda parameters.
     *
     * @param node The lambda expression node.
     * @param parentScope The parent scope.
     * @returns A new LambdaScope for the lambda expression.
     */
    private createLambdaScope(
        node: LambdaExpressionType,
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined
    ): Scope<TypirLangiumSpecifics> {
        const lambdaTypeInference = inferLambdaTypeFromContext<TypirLangiumSpecifics>(
            node,
            this.typir,
            expressionTypes,
            undefined
        );

        return new ModelTransformationLambdaScope(
            parentScope,
            (scope) => this.getLambdaScopeEntries(node, scope, lambdaTypeInference),
            () => [],
            node.parameterList.parameters.map((param) => ({
                name: param.name,
                position: -1
            })),
            node,
            lambdaTypeInference
        );
    }

    /**
     * Gets scope entries for a lambda expression.
     *
     * @param node The lambda expression node.
     * @param scope The current scope.
     * @param lambdaTypeInference The lambda type inference result.
     * @returns Array of scope entries for lambda parameters.
     */
    private getLambdaScopeEntries(
        node: LambdaExpressionType,
        scope: Scope<TypirLangiumSpecifics>,
        lambdaTypeInference: LambdaTypeInferenceResult<TypirLangiumSpecifics>
    ): ScopeEntry<TypirLangiumSpecifics>[] {
        if (Array.isArray(lambdaTypeInference)) {
            return node.parameterList.parameters.map((param) => ({
                name: param.name,
                position: -1,
                languageNode: param,
                definingScope: scope,
                inferType: () => lambdaTypeInference
            }));
        }

        const parameterTypes =
            "type" in lambdaTypeInference
                ? lambdaTypeInference.type.details.parameterTypes
                : lambdaTypeInference.parameterTypes;

        return node.parameterList.parameters.map((param, index) => ({
            name: param.name,
            position: -1,
            languageNode: param,
            definingScope: scope,
            inferType: () => parameterTypes[index] ?? lambdaTypeInference
        }));
    }

    /**
     * Collects pattern entries (object instances and variables) from a pattern.
     *
     * @param pattern The pattern to collect entries from.
     * @param position The position in the parent scope.
     * @param scope The current scope.
     * @param entries The array to add entries to.
     */
    private collectPatternEntries(
        pattern: PatternType | undefined,
        position: number,
        scope: Scope<TypirLangiumSpecifics>,
        entries: ScopeEntry<TypirLangiumSpecifics>[]
    ): void {
        for (const element of pattern?.elements ?? []) {
            if (this.reflection.isInstance(element, statementTypes.patternObjectInstanceType)) {
                entries.push(this.createObjectInstanceEntry(element, position, scope));
            } else if (this.reflection.isInstance(element, statementTypes.patternVariableType)) {
                entries.push(this.createPatternVariableEntry(element, position, scope));
            }
        }
    }

    /**
     * Collects pattern entries from a pattern (for match statement scopes).
     *
     * @param pattern The pattern to collect entries from.
     * @param scope The current scope.
     * @returns Array of scope entries.
     */
    private collectPatternEntriesFromPattern(
        pattern: PatternType | undefined,
        scope: Scope<TypirLangiumSpecifics>
    ): ScopeEntry<TypirLangiumSpecifics>[] {
        const entries: ScopeEntry<TypirLangiumSpecifics>[] = [];
        this.collectPatternEntries(pattern, -1, scope, entries);
        return entries;
    }

    /**
     * Collects pattern initializations from a pattern.
     *
     * @param pattern The pattern to collect initializations from.
     * @param position The position in the parent scope.
     * @param initializations The array to add initializations to.
     */
    private collectPatternInitializations(
        pattern: PatternType | undefined,
        position: number,
        initializations: ScopeLocalInitialization[]
    ): void {
        for (const element of pattern?.elements ?? []) {
            if (this.reflection.isInstance(element, statementTypes.patternObjectInstanceType)) {
                initializations.push({ name: element.name, position });
            } else if (this.reflection.isInstance(element, statementTypes.patternVariableType)) {
                initializations.push({ name: element.name, position });
            }
        }
    }

    /**
     * Collects pattern initializations from a pattern (for match statement scopes).
     *
     * @param pattern The pattern to collect initializations from.
     * @returns Array of local initialization records.
     */
    private collectPatternInitializationsFromPattern(pattern: PatternType | undefined): ScopeLocalInitialization[] {
        const initializations: ScopeLocalInitialization[] = [];
        this.collectPatternInitializations(pattern, -1, initializations);
        return initializations;
    }

    /**
     * Creates a scope entry for a pattern object instance.
     *
     * @param instance The object instance node.
     * @param position The position in the parent scope.
     * @param scope The current scope.
     * @returns A scope entry for the object instance.
     */
    private createObjectInstanceEntry(
        instance: PatternObjectInstanceType,
        position: number,
        scope: Scope<TypirLangiumSpecifics>
    ): ScopeEntry<TypirLangiumSpecifics> {
        return {
            name: instance.name,
            position,
            languageNode: instance,
            definingScope: scope,
            inferType: () => this.inference.inferType(instance)
        };
    }

    /**
     * Creates a scope entry for a pattern variable.
     *
     * @param variable The pattern variable node.
     * @param position The position in the parent scope.
     * @param scope The current scope.
     * @returns A scope entry for the pattern variable.
     */
    private createPatternVariableEntry(
        variable: PatternVariableType,
        position: number,
        scope: Scope<TypirLangiumSpecifics>
    ): ScopeEntry<TypirLangiumSpecifics> {
        return {
            name: variable.name,
            position,
            languageNode: variable,
            definingScope: scope,
            inferType: () => this.inference.inferType(variable)
        };
    }

    /**
     * Creates an empty scope for nodes that don't add entries.
     *
     * @param parentScope The parent scope.
     * @param languageNode The AST node.
     * @returns A new empty scope.
     */
    private createEmptyScope(
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined,
        languageNode: TypirLangiumSpecifics["LanguageType"]
    ): Scope<TypirLangiumSpecifics> {
        return new DefaultScope<TypirLangiumSpecifics>(
            parentScope,
            () => [],
            () => [],
            [],
            languageNode
        );
    }
}
