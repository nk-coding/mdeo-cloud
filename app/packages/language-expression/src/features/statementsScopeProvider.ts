import type { TypirLangiumSpecifics } from "typir-langium";
import { BaseScopeProvider } from "../typir-extensions/langium/baseScopeProvider.js";
import {
    DefaultScope,
    type BoundScope,
    type ControlFlowEntry,
    type Scope,
    type ScopeEntry,
    type ScopeLocalInitialization
} from "../typir-extensions/scope/scope.js";
import type { ExpressionTypirServices } from "../type-system/services.js";
import type { ForStatementType, StatementsScopeType, StatementTypes } from "../grammar/statementTypes.js";
import type { AstReflection } from "@mdeo/language-common";
import type { TypeInferenceCollector } from "typir";
import type { ExpressionTypes } from "../grammar/expressionTypes.js";
import type { ClassType } from "../typir-extensions/config/type.js";

/**
 * Scope provider for statement-based language constructs.
 *
 * Handles scope creation and management for statements including variable declarations,
 * control flow statements (if/else, loops), and for-loop iteration variables. Manages
 * variable visibility, control flow analysis, and initialization tracking within scopes.
 *
 * @template Specifics The language-specific Typir-Langium configuration.
 */
export class StatementsScopeProvider<Specifics extends TypirLangiumSpecifics> extends BaseScopeProvider<
    Specifics,
    ExpressionTypirServices<Specifics>
> {
    protected readonly reflection: AstReflection;
    protected readonly inference: TypeInferenceCollector<Specifics>;

    /**
     * Creates a new StatementsScopeProvider instance.
     *
     * @param typir The Typir services including type inference and language services.
     * @param statementTypes Type definitions for statement AST nodes.
     * @param expressionTypes Type definitions for expression AST nodes.
     * @param iterableType The class type used for iterable collections in for-loops.
     */
    constructor(
        typir: ExpressionTypirServices<Specifics>,
        protected readonly statementTypes: StatementTypes,
        protected readonly expressionTypes: ExpressionTypes,
        protected iterableType: ClassType
    ) {
        super(typir);
        this.reflection = typir.langium.LangiumServices.AstReflection;
        this.inference = typir.Inference;
    }

    override isScopeRelevantNode(node: Specifics["LanguageType"]): boolean {
        return (
            this.reflection.isInstance(node, this.statementTypes.statementsScopeType) ||
            this.reflection.isInstance(node, this.statementTypes.forStatementType)
        );
    }

    override createScope(
        languageNode: Specifics["LanguageType"],
        parentScope: BoundScope<Specifics> | undefined
    ): Scope<Specifics> {
        if (this.reflection.isInstance(languageNode, this.statementTypes.statementsScopeType)) {
            return this.createScopeForStatementsNode(languageNode, parentScope);
        } else if (this.reflection.isInstance(languageNode, this.statementTypes.forStatementType)) {
            return this.createScopeForForStatementNode(languageNode, parentScope);
        }
        throw new Error("Unsupported language node type for scope creation.");
    }

    /**
     * Creates a scope for a statements block node.
     *
     * The scope includes all variable declarations, control flow entries for conditional
     * and loop statements, and tracks variable initializations within the block.
     *
     * @param node The statements block AST node.
     * @param parentScope The parent scope containing this statements block, if any.
     * @returns A new scope for the statements block.
     */
    protected createScopeForStatementsNode(
        node: StatementsScopeType,
        parentScope: BoundScope<Specifics> | undefined
    ): Scope<Specifics> {
        return new DefaultScope<Specifics>(
            parentScope,
            (scope) => this.getStatementsScopeEntries(node, scope),
            () => this.getStatementsControlFlowEntries(node),
            this.getStatementsLocalInitializations(node),
            node
        );
    }

    /**
     * Creates a scope for a for-statement node.
     *
     * The scope includes the loop iteration variable, which is accessible within
     * the for-loop body. The variable's type is inferred from the loop's iterable.
     *
     * @param node The for-statement AST node.
     * @param parentScope The parent scope containing this for-statement, if any.
     * @returns A new scope for the for-statement.
     */
    protected createScopeForForStatementNode(
        node: ForStatementType,
        parentScope: BoundScope<Specifics> | undefined
    ): Scope<Specifics> {
        const variable = node.variable;
        return new DefaultScope<Specifics>(
            parentScope,
            (scope) => {
                return [
                    {
                        name: variable.name,
                        position: -1,
                        languageNode: node,
                        definingScope: scope,
                        inferType: () => this.inference.inferType(node.variable)
                    }
                ];
            },
            () => [],
            [
                {
                    name: variable.name,
                    position: -1
                }
            ],
            node
        );
    }

    /**
     * Extracts scope entries from a statements block.
     *
     * Collects all variable declarations within the statements block, creating
     * scope entries with their names, positions, and type inference functions.
     *
     * @param node The statements block AST node.
     * @param scope The scope being populated with entries.
     * @returns An array of scope entries for variables declared in the block.
     */
    protected getStatementsScopeEntries(node: StatementsScopeType, scope: Scope<Specifics>): ScopeEntry<Specifics>[] {
        const entries: ScopeEntry<Specifics>[] = [];
        for (let i = 0; i < node.statements.length; i++) {
            const statement = node.statements[i];
            if (this.reflection.isInstance(statement, this.statementTypes.variableDeclarationStatementType)) {
                entries.push({
                    languageNode: statement,
                    name: statement.name,
                    position: i,
                    definingScope: scope,
                    inferType: () => this.inference.inferType(statement)
                });
            }
        }
        return entries;
    }

    /**
     * Extracts control flow entries from a statements block.
     *
     * Identifies control flow statements (if/else-if/else, while, do-while, for)
     * and creates entries that track their nested scopes and completeness.
     * Control flow completeness affects variable initialization analysis.
     *
     * @param node The statements block AST node.
     * @returns An array of control flow entries for statements with branching or loops.
     */
    protected getStatementsControlFlowEntries(node: StatementsScopeType): ControlFlowEntry<Specifics>[] {
        const controlFlowEntries: ControlFlowEntry<Specifics>[] = [];
        for (let i = 0; i < node.statements.length; i++) {
            const statement = node.statements[i];
            if (this.reflection.isInstance(statement, this.statementTypes.ifStatementType)) {
                const scopes: Scope<Specifics>[] = [this.getScope(statement.thenBlock).scope];
                for (const elseIf of statement.elseIfs) {
                    scopes.push(this.getScope(elseIf.thenBlock).scope);
                }
                if (statement.elseBlock != undefined) {
                    scopes.push(this.getScope(statement.elseBlock).scope);
                }
                controlFlowEntries.push({
                    position: i,
                    scopes,
                    isComplete: statement.elseBlock != undefined
                });
            } else if (
                this.reflection.isInstance(statement, this.statementTypes.forStatementType) ||
                this.reflection.isInstance(statement, this.statementTypes.whileStatementType)
            ) {
                controlFlowEntries.push({
                    position: i,
                    scopes: [this.getScope(statement.body).scope],
                    isComplete: false
                });
            }
        }
        return controlFlowEntries;
    }

    /**
     * Extracts local variable initializations from a statements block.
     *
     * Tracks where variables are initialized within the block, including both
     * variable declarations with initial values and assignment statements.
     * This information is used for definite assignment analysis.
     *
     * @param node The statements block AST node.
     * @returns An array of initialization records with variable names and positions.
     */
    protected getStatementsLocalInitializations(node: StatementsScopeType): ScopeLocalInitialization[] {
        const initializations: ScopeLocalInitialization[] = [];
        for (let i = 0; i < node.statements.length; i++) {
            const statement = node.statements[i];
            if (this.reflection.isInstance(statement, this.statementTypes.variableDeclarationStatementType)) {
                if (statement.initialValue != undefined) {
                    initializations.push({
                        name: statement.name,
                        position: i
                    });
                } else if (this.reflection.isInstance(statement, this.statementTypes.assignmentStatementType)) {
                    if (this.reflection.isInstance(statement.left, this.expressionTypes.identifierExpressionType)) {
                        initializations.push({
                            name: statement.left.name,
                            position: i
                        });
                    }
                }
            }
        }
        return initializations;
    }
}
