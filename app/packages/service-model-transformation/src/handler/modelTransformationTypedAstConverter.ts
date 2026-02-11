import type { AstReflection } from "@mdeo/language-common";
import {
    TypedAstConverter,
    type TypedClass,
    type TypedExpression,
    extractMetamodelClasses,
    TypedClassConverter,
    DefaultCollectionTypeFactory,
    type MetamodelClassInfo
} from "@mdeo/language-expression";
import { AssociationResolver } from "@mdeo/language-model";
import {
    expressionTypes,
    type ModelTransformationTypirServices,
    type ModelTransformationType,
    type TypedAst,
    type TypedTransformationStatement,
    statementTypes,
    type MatchStatementType,
    type TypedMatchStatement,
    type IfMatchStatementType,
    type TypedIfMatchStatement,
    type WhileMatchStatementType,
    type TypedWhileMatchStatement,
    type UntilMatchStatementType,
    type TypedUntilMatchStatement,
    type ForMatchStatementType,
    type TypedForMatchStatement,
    type IfExpressionStatementType,
    type TypedIfExpressionStatement,
    type ElseIfBranchType,
    type TypedElseIfBranch,
    type WhileExpressionStatementType,
    type TypedWhileExpressionStatement,
    type StopStatementType,
    type TypedStopStatement,
    type PatternType,
    type TypedPattern,
    type TypedPatternVariableElement,
    type TypedPatternObjectInstanceElement,
    type TypedPatternLinkElement,
    type TypedPatternWhereClauseElement,
    type PatternVariableType,
    type TypedPatternVariable,
    type PatternObjectInstanceType,
    type TypedPatternObjectInstance,
    type PatternLinkType,
    type TypedPatternLink,
    type WhereClauseType,
    type TypedWhereClause,
    LambdaExpression,
    type LambdaExpressionType,
    type TypedLambdaExpression
} from "@mdeo/language-model-transformation";
import {
    Class,
    MetaModel,
    type MetaModelType,
    type ClassType as MetamodelClassType,
    type PropertyType
} from "@mdeo/language-metamodel";
import { resolveRelativePath } from "@mdeo/language-shared";
import type { AstNode, LangiumDocument } from "langium";

/**
 * Model Transformation-specific extension of ExpressionTypedAstConverter.
 * Handles transformation-specific features like patterns, match statements, and lambda expressions.
 * Does not use the base statement types as transformation statements are conceptually different.
 */
export class ModelTransformationTypedAstConverter extends TypedAstConverter {
    /**
     * Expression type identifiers for checking instance types.
     */
    protected expressionTypes = expressionTypes;

    /**
     * Resolver for associations between classes.
     */
    private readonly associationResolver: AssociationResolver;

    /**
     * Creates a new ModelTransformationTypedAstConverter.
     *
     * @param typir The Typir services for type inference
     * @param reflection The AST reflection for type checking
     */
    constructor(
        protected override readonly typir: ModelTransformationTypirServices,
        reflection: AstReflection
    ) {
        super(typir, reflection);
        this.associationResolver = new AssociationResolver(reflection);
    }

    /**
     * Converts a ModelTransformation AST node to a TypedAst.
     * This method is now async to support metamodel resolution.
     *
     * @param document The transformation document
     * @param transformation The ModelTransformation AST node
     * @returns The TypedAst representation
     */
    async convertModelTransformation(
        document: LangiumDocument,
        transformation: ModelTransformationType
    ): Promise<TypedAst> {
        const classes = await this.extractTypedClassesFromMetamodel(document, transformation);

        const statements: TypedTransformationStatement[] = transformation.statements.map((stmt) =>
            this.convertTransformationStatement(stmt)
        );

        return {
            types: this.types,
            metamodelUri: transformation.import.file,
            classes,
            statements
        };
    }

    /**
     * Extracts TypedClasses from the metamodel referenced by a transformation.
     * Resolves the metamodel file, extracts class information, and converts to TypedClasses.
     *
     * @param document The transformation document
     * @param transformation The transformation AST node
     * @returns Array of TypedClasses extracted from the metamodel
     */
    private async extractTypedClassesFromMetamodel(
        document: LangiumDocument,
        transformation: ModelTransformationType
    ): Promise<TypedClass[]> {
        const metamodel = await this.loadMetamodel(document, transformation);
        if (metamodel == undefined) {
            return [];
        }

        const classes = this.extractClasses(metamodel);
        if (classes.length === 0) {
            return [];
        }

        const classInfos = extractMetamodelClasses(classes, this.reflection, DefaultCollectionTypeFactory);

        return this.convertToTypedClasses(classInfos);
    }

    /**
     * Loads the metamodel document referenced by a transformation.
     *
     * @param document The transformation document
     * @param transformation The transformation AST node
     * @returns The metamodel AST or undefined if not found
     */
    private async loadMetamodel(
        document: LangiumDocument,
        transformation: ModelTransformationType
    ): Promise<MetaModelType | undefined> {
        const importFile = transformation.import?.file;
        if (importFile == undefined) {
            return undefined;
        }

        const metamodelUri = resolveRelativePath(document, importFile);
        const documents = (this.typir.langium.LangiumServices as any).workspace.LangiumDocuments;
        const metamodelDoc = await documents.getOrCreateDocument(metamodelUri);

        const metamodelRoot = metamodelDoc.parseResult?.value;
        if (metamodelRoot == undefined || !this.reflection.isInstance(metamodelRoot, MetaModel)) {
            return undefined;
        }

        return metamodelRoot;
    }

    /**
     * Extracts local classes from a metamodel.
     * With the simplified import system, only Class nodes in the metamodel elements are collected.
     *
     * @param metamodel The metamodel AST
     * @returns Array of classes
     */
    private extractClasses(metamodel: MetaModelType): MetamodelClassType[] {
        const result: MetamodelClassType[] = [];

        for (const element of metamodel.elements ?? []) {
            if (this.reflection.isInstance(element, Class)) {
                result.push(element);
            }
        }

        return result;
    }

    /**
     * Converts extracted class information to TypedClasses.
     *
     * @param classInfos The extracted class information
     * @returns Array of TypedClasses
     */
    private convertToTypedClasses(classInfos: MetamodelClassInfo[]): TypedClass[] {
        const typeDefinitions = this.typir.TypeDefinitions;
        const typedClassConverter = new TypedClassConverter(typeDefinitions, this);
        return typedClassConverter.convertToTypedClasses(classInfos);
    }

    /**
     * Converts a transformation statement.
     *
     * @param stmt The statement AST node from the parsed grammar
     * @returns The TypedTransformationStatement representation
     * @throws Error if statement type is unknown or not supported
     */
    private convertTransformationStatement(stmt: AstNode): TypedTransformationStatement {
        if (this.reflection.isInstance(stmt, statementTypes.matchStatementType)) {
            return this.convertMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.ifMatchStatementType)) {
            return this.convertIfMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.whileMatchStatementType)) {
            return this.convertWhileMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.untilMatchStatementType)) {
            return this.convertUntilMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.forMatchStatementType)) {
            return this.convertForMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.ifExpressionStatementType)) {
            return this.convertIfExpressionStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.whileExpressionStatementType)) {
            return this.convertWhileExpressionStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.stopStatementType)) {
            return this.convertStopStatement(stmt);
        }
        throw new Error(`Unknown transformation statement type: ${stmt.$type}`);
    }

    /**
     * Converts a match statement.
     *
     * @param stmt The MatchStatement AST node
     * @returns The TypedMatchStatement representation
     */
    private convertMatchStatement(stmt: MatchStatementType): TypedMatchStatement {
        return {
            kind: "match",
            pattern: this.convertPattern(stmt.pattern)
        };
    }

    /**
     * Converts an if-match statement.
     *
     * @param stmt The IfMatchStatement AST node
     * @returns The TypedIfMatchStatement representation
     */
    private convertIfMatchStatement(stmt: IfMatchStatementType): TypedIfMatchStatement {
        return {
            kind: "ifMatch",
            pattern: this.convertPattern(stmt.ifBlock.pattern),
            thenBlock: stmt.ifBlock.thenBlock.statements.map((s) => this.convertTransformationStatement(s)),
            elseBlock: stmt.elseBlock
                ? stmt.elseBlock.statements.map((s) => this.convertTransformationStatement(s))
                : undefined
        };
    }

    /**
     * Converts a while-match statement.
     *
     * @param stmt The WhileMatchStatement AST node
     * @returns The TypedWhileMatchStatement representation
     */
    private convertWhileMatchStatement(stmt: WhileMatchStatementType): TypedWhileMatchStatement {
        return {
            kind: "whileMatch",
            pattern: this.convertPattern(stmt.pattern),
            doBlock: stmt.doBlock.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts an until-match statement.
     *
     * @param stmt The UntilMatchStatement AST node
     * @returns The TypedUntilMatchStatement representation
     */
    private convertUntilMatchStatement(stmt: UntilMatchStatementType): TypedUntilMatchStatement {
        return {
            kind: "untilMatch",
            pattern: this.convertPattern(stmt.pattern),
            doBlock: stmt.doBlock.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts a for-match statement.
     *
     * @param stmt The ForMatchStatement AST node
     * @returns The TypedForMatchStatement representation
     */
    private convertForMatchStatement(stmt: ForMatchStatementType): TypedForMatchStatement {
        return {
            kind: "forMatch",
            pattern: this.convertPattern(stmt.pattern),
            doBlock: stmt.doBlock.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts an if-expression statement.
     *
     * @param stmt The IfExpressionStatement AST node
     * @returns The TypedIfExpressionStatement representation
     */
    private convertIfExpressionStatement(stmt: IfExpressionStatementType): TypedIfExpressionStatement {
        return {
            kind: "ifExpression",
            condition: this.convertExpression(stmt.condition),
            thenBlock: stmt.thenBlock.statements.map((s) => this.convertTransformationStatement(s)),
            elseIfBranches: stmt.elseIfBranches.map((branch) => this.convertElseIfBranch(branch)),
            elseBlock: stmt.elseBlock
                ? stmt.elseBlock.statements.map((s) => this.convertTransformationStatement(s))
                : undefined
        };
    }

    /**
     * Converts an else-if branch.
     *
     * @param branch The ElseIfBranch AST node
     * @returns The TypedElseIfBranch representation
     */
    private convertElseIfBranch(branch: ElseIfBranchType): TypedElseIfBranch {
        return {
            condition: this.convertExpression(branch.condition),
            block: branch.block.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts a while-expression statement.
     *
     * @param stmt The WhileExpressionStatement AST node
     * @returns The TypedWhileExpressionStatement representation
     */
    private convertWhileExpressionStatement(stmt: WhileExpressionStatementType): TypedWhileExpressionStatement {
        return {
            kind: "whileExpression",
            condition: this.convertExpression(stmt.condition),
            block: stmt.block.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts a stop statement.
     *
     * @param stmt The StopStatement AST node
     * @returns The TypedStopStatement representation
     */
    private convertStopStatement(stmt: StopStatementType): TypedStopStatement {
        return {
            kind: "stop",
            keyword: stmt.keyword
        };
    }

    /**
     * Converts a pattern.
     *
     * @param pattern The Pattern AST node
     * @returns The TypedPattern representation
     */
    private convertPattern(pattern: PatternType): TypedPattern {
        const elements: (
            | TypedPatternVariableElement
            | TypedPatternObjectInstanceElement
            | TypedPatternLinkElement
            | TypedPatternWhereClauseElement
        )[] = [];

        for (const element of pattern.elements) {
            if (this.reflection.isInstance(element, statementTypes.patternVariableType)) {
                elements.push({
                    kind: "variable",
                    variable: this.convertPatternVariable(element)
                });
            } else if (this.reflection.isInstance(element, statementTypes.patternObjectInstanceType)) {
                elements.push({
                    kind: "objectInstance",
                    objectInstance: this.convertPatternObjectInstance(element)
                });
            } else if (this.reflection.isInstance(element, statementTypes.patternLinkType)) {
                elements.push({
                    kind: "link",
                    link: this.convertPatternLink(element)
                });
            } else if (this.reflection.isInstance(element, statementTypes.whereClauseType)) {
                elements.push({
                    kind: "whereClause",
                    whereClause: this.convertWhereClause(element)
                });
            }
        }

        return { elements };
    }

    /**
     * Converts a pattern variable.
     *
     * @param variable The PatternVariable AST node
     * @returns The TypedPatternVariable representation
     */
    private convertPatternVariable(variable: PatternVariableType): TypedPatternVariable {
        return {
            name: variable.name,
            type: variable.type ? this.getTypeIndex(variable.type) : undefined,
            value: this.convertExpression(variable.value)
        };
    }

    /**
     * Converts a pattern object instance.
     *
     * @param obj The PatternObjectInstance AST node
     * @returns The TypedPatternObjectInstance representation
     */
    private convertPatternObjectInstance(obj: PatternObjectInstanceType): TypedPatternObjectInstance {
        return {
            modifier: obj.modifier?.modifier,
            name: obj.name,
            className: obj.class?.$refText ?? "",
            properties: obj.properties.map((prop) => ({
                propertyName: prop.name?.$refText ?? "",
                operator: prop.operator,
                value: this.convertExpression(prop.value)
            }))
        };
    }

    /**
     * Converts a pattern link.
     * Uses AssociationResolver to resolve both property names and determine link direction.
     *
     * @param link The PatternLink AST node
     * @returns The TypedPatternLink representation
     */
    private convertPatternLink(link: PatternLinkType): TypedPatternLink {
        const { sourcePropertyName, targetPropertyName, isOutgoing } = this.resolvePatternLinkProperties(link);

        const start = {
            objectName: link.source.object?.$refText ?? "",
            propertyName: sourcePropertyName
        };
        const end = {
            objectName: link.target.object?.$refText ?? "",
            propertyName: targetPropertyName
        };

        return {
            modifier: link.modifier?.modifier,
            source: isOutgoing ? start : end,
            target: isOutgoing ? end : start
        };
    }

    /**
     * Resolves both property names and direction for a pattern link.
     * Uses AssociationResolver to find the association and determine property names.
     *
     * @param link The PatternLink AST node
     * @returns Object containing source/target property names and isOutgoing flag
     */
    private resolvePatternLinkProperties(link: PatternLinkType): {
        sourcePropertyName?: string;
        targetPropertyName?: string;
        isOutgoing: boolean;
    } {
        const sourceClass = this.getPatternLinkEndClass(link.source);
        const targetClass = this.getPatternLinkEndClass(link.target);

        if (!sourceClass || !targetClass) {
            throw new Error(
                `Unable to resolve classes for pattern link between '${link.source.object?.$refText}' and '${link.target.object?.$refText}'`
            );
        }

        const sourceProperty = link.source.property?.ref as PropertyType | undefined;
        const targetProperty = link.target.property?.ref as PropertyType | undefined;

        const resolved = this.associationResolver.resolveAssociation(
            sourceClass,
            targetClass,
            sourceProperty,
            targetProperty
        );

        if (!resolved) {
            throw new Error(
                `Unable to resolve association for pattern link between '${sourceClass.name}' and '${targetClass.name}' with properties '${sourceProperty?.name}' and '${targetProperty?.name}'.`
            );
        }

        return {
            sourcePropertyName: resolved.sourcePropertyName,
            targetPropertyName: resolved.targetPropertyName,
            isOutgoing: resolved.matchesDirection
        };
    }

    /**
     * Gets the class type for a pattern link end by resolving the object instance reference.
     *
     * @param linkEnd The pattern link end
     * @returns The class type or undefined if not resolvable
     */
    private getPatternLinkEndClass(linkEnd: { object?: { ref?: unknown } }): MetamodelClassType | undefined {
        const objectInstance = linkEnd.object?.ref;
        if (!objectInstance || typeof objectInstance !== "object") {
            return undefined;
        }

        const classRef = (objectInstance as { class?: { ref?: unknown } }).class?.ref;
        if (classRef && this.reflection.isInstance(classRef, Class)) {
            return classRef;
        }
        return undefined;
    }

    /**
     * Converts a where clause.
     *
     * @param clause The WhereClause AST node
     * @returns The TypedWhereClause representation
     */
    private convertWhereClause(clause: WhereClauseType): TypedWhereClause {
        return {
            expression: this.convertExpression(clause.expression)
        };
    }

    /**
     * Handles expression conversion, including lambda expressions.
     *
     * @param expr The expression AST node
     * @returns The TypedExpression representation
     * @throws Error if expression type is unsupported
     */
    protected override convertAdditionalExpression(expr: AstNode): TypedExpression {
        if (this.reflection.isInstance(expr, LambdaExpression)) {
            return this.convertLambdaExpression(expr);
        }
        return super.convertAdditionalExpression(expr);
    }

    /**
     * Converts a lambda expression.
     *
     * @param expr The lambda expression AST node
     * @returns The TypedLambdaExpression representation
     */
    private convertLambdaExpression(expr: LambdaExpressionType): TypedLambdaExpression {
        const parameters = expr.parameterList.parameters.map((param) => param.name);

        return {
            kind: "lambda",
            evalType: this.getTypeIndex(expr),
            parameters,
            body: this.convertExpression(expr.expression)
        };
    }
}
