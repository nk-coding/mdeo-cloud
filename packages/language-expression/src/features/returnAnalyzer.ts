import type { InferenceProblem, Type, TypirProblem, TypirSpecifics, ValidationProblem } from "typir";
import type { ExtendedTypirServices } from "../typir-extensions/service/extendedTypirServices.js";
import type { CustomValueType } from "../typir-extensions/kinds/custom-value/custom-value-type.js";
import type { CustomVoidType } from "../typir-extensions/kinds/custom-void/custom-void-type.js";
import type { BoundScope, Scope } from "../typir-extensions/scope/scope.js";
import { findCommonParentType } from "../typir-extensions/rules/commonParentType.js";

/**
 * Interface for accessing return statement information from language nodes.
 * Implementations provide language-specific logic for identifying and extracting
 * information from return statements.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export interface ReturnStatementAccessor<Specifics extends TypirSpecifics> {
    /**
     * Determines if a language node is a return statement.
     *
     * @param node The language node to check
     * @returns true if the node is a return statement, false otherwise
     */
    isReturnStatement(node: Specifics["LanguageType"]): boolean;

    /**
     * Extracts the returned expression from a return statement.
     * Should return undefined if the return statement has no expression (void return).
     *
     * @param node The return statement node
     * @returns The expression being returned, or undefined for void returns
     */
    getReturnExpression(node: Specifics["LanguageType"]): Specifics["LanguageType"] | undefined;

    /**
     * Gets the statements from a statements scope node.
     * By default, only handles statements scope nodes.
     *
     * @param node The language node (should be a statements scope)
     * @returns Array of statement nodes, or undefined if not a statements scope
     */
    getStatementsFromScope(node: Specifics["LanguageType"]): Specifics["LanguageType"][] | undefined;
}

/**
 * Base abstract class for analyzing return statements in a statements block.
 * Handles both validation and inference modes.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @template TProblem The type of validation problems to collect
 */
export abstract class ReturnAnalyzerBase<Specifics extends TypirSpecifics, TProblem> {
    /**
     * List of validation/inference errors collected during analysis
     */
    readonly errors: TProblem[] = [];

    /**
     * The analyzed return type, if it could be determined
     */
    returnType: CustomValueType | CustomVoidType | undefined = undefined;

    /**
     * Whether all paths in the block return a value
     */
    allPathsReturn = false;

    /**
     * Creates a new return analyzer.
     *
     * @param scope The scope containing the statements to analyze
     * @param services Extended Typir services for type operations
     * @param accessor Language-specific accessor for return statement information
     */
    constructor(
        protected readonly scope: BoundScope<Specifics>,
        readonly services: ExtendedTypirServices<Specifics>,
        protected readonly accessor: ReturnStatementAccessor<Specifics>
    ) {}

    /**
     * Creates an error/problem object for a validation/inference failure.
     * Must be implemented by subclasses to create language-specific error objects.
     *
     * @param languageNode The AST node where the error occurred
     * @param message Human-readable error message
     * @param subProblems Optional nested problems that caused this error
     * @returns A problem object of type TProblem
     */
    protected abstract createError(
        languageNode: Specifics["LanguageType"],
        message: string,
        subProblems?: TypirProblem[]
    ): TProblem;

    /**
     * Analyzes a return statement and updates the analyzer state.
     * Must be implemented by subclasses based on whether in inference or validation mode.
     *
     * @param statement The return statement node itself
     * @param returnExpr The expression being returned, or undefined for void returns
     */
    protected abstract analyzeReturnExpression(
        statement: Specifics["LanguageType"],
        returnExpr: Specifics["LanguageType"] | undefined
    ): void;

    /**
     * Analyzes all return statements in the scope.
     * Should be called by subclasses after construction.
     */
    protected analyze(): void {
        this.allPathsReturn = this.analyzeScope(this.scope.scope);
    }

    /**
     * Analyzes a single scope for return statements.
     *
     * @param scope The scope to analyze
     * @returns Whether all paths in this scope return a value
     */
    private analyzeScope(scope: Scope<Specifics>): boolean {
        let hasDirectReturn = false;

        const languageNode = scope.languageNode;
        if (languageNode == undefined) {
            return false;
        }

        const statements = this.accessor.getStatementsFromScope(languageNode);
        if (statements == undefined) {
            return false;
        }

        for (const statement of statements) {
            if (this.accessor.isReturnStatement(statement)) {
                hasDirectReturn = true;
                const returnExpr = this.accessor.getReturnExpression(statement);
                this.analyzeReturnExpression(statement, returnExpr);
            }
        }

        const controlFlowEntries = scope.getControlFlowEntries();
        let hasCompleteReturningControlFlow = false;

        for (const controlFlowEntry of controlFlowEntries) {
            const childResults = controlFlowEntry.scopes.map((childScope) => this.analyzeScope(childScope));

            if (controlFlowEntry.isComplete && childResults.every((result) => result)) {
                hasCompleteReturningControlFlow = true;
            }
        }

        return hasDirectReturn || hasCompleteReturningControlFlow;
    }

    /**
     * Infers the return type from a return expression.
     *
     * @param returnExpr The expression being returned, or undefined for void returns
     * @returns The inferred type or a list of inference problems
     */
    protected inferReturnType(returnExpr: Specifics["LanguageType"]): Type | InferenceProblem<Specifics>[] {
        if (returnExpr != undefined) {
            return this.services.Inference.inferType(returnExpr);
        } else {
            return this.services.factory.CustomVoid.getOrCreate();
        }
    }
}

/**
 * Return analyzer for inference mode.
 * Infers the return type by finding the common parent type of all returned values.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export class ReturnInferenceAnalyzer<Specifics extends TypirSpecifics> extends ReturnAnalyzerBase<
    Specifics,
    InferenceProblem<Specifics>
> {
    /**
     * Creates a new return inference analyzer.
     *
     * @param scope The scope containing the statements to analyze
     * @param services Extended Typir services for type operations
     * @param accessor Language-specific accessor for return statement information
     */
    constructor(
        scope: BoundScope<Specifics>,
        services: ExtendedTypirServices<Specifics>,
        accessor: ReturnStatementAccessor<Specifics>
    ) {
        super(scope, services, accessor);
        this.analyze();
    }

    /**
     * Analyzes a return statement and combines its type with previously seen return types.
     *
     * @param statement The return statement node itself
     * @param returnExpr The expression being returned, or undefined for void returns
     */
    protected override analyzeReturnExpression(
        statement: Specifics["LanguageType"],
        returnExpr: Specifics["LanguageType"] | undefined
    ): void {
        const inferredType = this.inferReturnType(returnExpr);

        if (Array.isArray(inferredType)) {
            this.errors.push(
                this.createError(returnExpr, `Return expression type could not be determined.`, inferredType)
            );
        } else if (this.services.factory.CustomValues.isCustomValueType(inferredType)) {
            if (this.returnType == undefined) {
                this.returnType = inferredType;
            } else if (this.services.factory.CustomValues.isCustomValueType(this.returnType)) {
                this.returnType = findCommonParentType(this.returnType, inferredType, this.services);
            }
        } else {
            this.errors.push(this.createError(returnExpr, `Return expression type is not a valid value type.`));
        }
    }

    /**
     * Creates an inference problem for a validation/inference failure.
     *
     * @param languageNode The AST node where the error occurred
     * @param message Human-readable error message
     * @param subProblems Optional nested problems that caused this error
     * @returns An inference problem object
     */
    protected override createError(
        languageNode: Specifics["LanguageType"],
        message: string,
        subProblems?: TypirProblem[]
    ): InferenceProblem<Specifics> {
        const { InferenceProblem } = this.services.context.typir;
        return {
            $problem: InferenceProblem,
            languageNode,
            location: message,
            subProblems: subProblems ?? []
        };
    }
}

/**
 * Return analyzer for validation mode.
 * Validates that all return statements match the expected return type.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
export class ReturnValidationAnalyzer<Specifics extends TypirSpecifics> extends ReturnAnalyzerBase<
    Specifics,
    ValidationProblem<Specifics>
> {
    /**
     * Creates a new return validation analyzer.
     *
     * @param scope The scope containing the statements to analyze
     * @param expectedReturnType The expected return type to validate against
     * @param expectedReturnTypeLanguageNode The language node representing the expected return type (for error reporting)
     * @param services Extended Typir services for type operations
     * @param accessor Language-specific accessor for return statement information
     */
    constructor(
        scope: BoundScope<Specifics>,
        private readonly expectedReturnType: CustomValueType | CustomVoidType,
        private readonly expectedReturnTypeLanguageNode: Specifics["LanguageType"],
        services: ExtendedTypirServices<Specifics>,
        accessor: ReturnStatementAccessor<Specifics>
    ) {
        super(scope, services, accessor);
        this.returnType = expectedReturnType;
        this.analyze();
        this.validateAllPathsReturn();
    }

    /**
     * Analyzes a return statement and validates its type against the expected return type.
     *
     * @param statement The return statement node itself
     * @param returnExpr The expression being returned, or undefined for void returns
     */
    protected override analyzeReturnExpression(
        statement: Specifics["LanguageType"],
        returnExpr: Specifics["LanguageType"] | undefined
    ): void {
        const inferredType = this.inferReturnType(returnExpr);

        if (Array.isArray(inferredType)) {
            return;
        }
        if (
            !(
                this.services.factory.CustomValues.isCustomValueType(inferredType) ||
                this.services.factory.CustomVoid.isCustomVoidType(inferredType)
            )
        ) {
            this.errors.push(this.createError(statement, `Return expression type is not a valid value type.`));
            return;
        }

        const isAssignable = this.services.Assignability.isAssignable(inferredType, this.expectedReturnType);
        if (!isAssignable) {
            this.errors.push(
                this.createError(
                    statement,
                    `Return type '${inferredType.getName()}' is not assignable to expected return type '${this.expectedReturnType.getName()}'.`
                )
            );
        }
    }

    /**
     * Creates an error/problem object for a validation failure.
     *
     * @param languageNode The AST node where the error occurred
     * @param message Human-readable error message
     * @param subProblems Optional nested problems that caused this error
     * @returns A problem object of type ValidationProblem
     */
    protected override createError(
        languageNode: Specifics["LanguageType"],
        message: string,
        subProblems?: TypirProblem[]
    ): ValidationProblem<Specifics> {
        const { ValidationProblem } = this.services.context.typir;
        return {
            $problem: ValidationProblem,
            languageNode,
            severity: "error",
            message,
            subProblems
        };
    }

    /**
     * Validates that all paths return a value if the expected return type is not void.
     */
    private validateAllPathsReturn(): void {
        const isVoidReturn = this.services.factory.CustomVoid.isCustomVoidType(this.expectedReturnType);

        if (!isVoidReturn && !this.allPathsReturn) {
            this.errors.push(
                this.createError(this.expectedReturnTypeLanguageNode, `Not all code paths return a value.`)
            );
        }
    }
}
