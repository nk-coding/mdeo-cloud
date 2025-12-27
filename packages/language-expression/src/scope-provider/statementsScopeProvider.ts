import type { TypirLangiumSpecifics } from "typir-langium";
import { BaseScopeProvider } from "../typir-extensions/langium/baseScopeProvider.js";
import {
    DefaultScope,
    type BoundScope,
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

export class StatementsScopeProvider<Specifics extends TypirLangiumSpecifics> extends BaseScopeProvider<Specifics> {
    protected readonly reflection: AstReflection;
    protected readonly inference: TypeInferenceCollector<Specifics>;

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

    protected createScopeForStatementsNode(
        node: StatementsScopeType,
        parentScope: BoundScope<Specifics> | undefined
    ): Scope<Specifics> {
        return new DefaultScope<Specifics>(
            parentScope,
            (scope) => this.getStatementsScopeEntries(node, scope),
            () => this.getStatementsSequentialChildScopes(node),
            this.getStatementsLocalInitializations(node)
        );
    }

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
            ]
        );
    }

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

    protected getStatementsSequentialChildScopes(node: StatementsScopeType): (Scope<Specifics>[] | undefined)[] {
        const childScopes: (Scope<Specifics>[] | undefined)[] = [];
        for (const statement of node.statements) {
            if (
                this.reflection.isInstance(statement, this.statementTypes.ifStatementType) &&
                statement.elseBlock != undefined
            ) {
                childScopes.push([
                    this.getScope(statement.thenBlock),
                    ...statement.elseIfs.map((elseIf) => this.getScope(elseIf.thenBlock)),
                    this.getScope(statement.elseBlock)
                ]);
            } else if (this.reflection.isInstance(statement, this.statementTypes.doWhileStatementType)) {
                childScopes.push([this.getScope(statement.body)]);
            } else {
                childScopes.push(undefined);
            }
        }
        return childScopes;
    }

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
