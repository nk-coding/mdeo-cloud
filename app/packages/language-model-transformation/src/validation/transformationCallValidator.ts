import type { AstNode, LangiumDocument, ValidationAcceptor } from "langium";
import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import { isMetamodelCompatible } from "@mdeo/language-metamodel";
import {
    climbsAboveProjectRoot,
    resolveRelativeDocument,
    resolveRelativePath,
    sharedImport
} from "@mdeo/language-shared";
import {
    CallOutput,
    CallStatement,
    ForMatchStatement,
    IfExpressionStatement,
    IfMatchStatement,
    MatchStatement,
    ModelTransformation,
    PatternVariableReassignment,
    StopStatement,
    TransformationParameter,
    UntilMatchStatement,
    WhileExpressionStatement,
    WhileMatchStatement,
    type BaseTransformationStatementType,
    type CallStatementType,
    type ForMatchStatementType,
    type IfExpressionStatementType,
    type IfMatchStatementType,
    type ModelTransformationType,
    type PatternType,
    type PatternVariableReassignmentType,
    type StatementsScopeType,
    type StopStatementType,
    type TransformationFileImportType,
    type TransformationParameterType,
    type TransformationSignatureType,
    type UntilMatchStatementType,
    type WhileExpressionStatementType,
    type WhileMatchStatementType
} from "../grammar/modelTransformationTypes.js";
import {
    TransformationDeclarationResolver,
    parameterTypeName,
    requiredInputs
} from "../features/transformationDeclarations.js";

const { AstUtils, UriUtils } = sharedImport("langium");

/**
 * Validates callable transformations: signatures, imports of other transformations, calls and
 * the references of patterns to parameters and call outputs.
 *
 * The rules (stage 1 of callable transformations):
 * - a parameter is a single model element (a metamodel class) or a scalar (`int`, `long`,
 *   `float`, `double`, `string`, `boolean`, an enum); an element input is not nullable and has
 *   no default;
 * - an imported file must be a transformation with a signature, whose metamodel is this file's
 *   metamodel or one this file's metamodel imports; imports must not form a cycle;
 * - a call names each input at most once, passes every required input and binds only outputs;
 * - a non-nullable output is assigned on every path on which the transformation succeeds.
 */
export class TransformationCallValidator {
    private readonly reflection: AstReflection;
    private readonly declarations: TransformationDeclarationResolver;

    constructor(private readonly services: ExtendedLangiumServices) {
        this.reflection = services.shared.AstReflection;
        this.declarations = new TransformationDeclarationResolver(
            this.reflection,
            services.shared.workspace.LangiumDocuments
        );
    }

    /**
     * Validates the signature, the imports and the definite assignment of the outputs.
     *
     * @param transformation The transformation root
     * @param accept The validation acceptor
     */
    validateTransformation(transformation: ModelTransformationType, accept: ValidationAcceptor): void {
        if (transformation.signature != undefined) {
            this.validateSignature(transformation.signature, accept);
            this.validateOutputsAssigned(transformation, transformation.signature, accept);
        }
        for (const fileImport of transformation.imports ?? []) {
            this.validateImport(transformation, fileImport, accept);
        }
    }

    /**
     * Validates a call: its arguments and outputs against the callee's signature.
     *
     * @param call The call statement
     * @param accept The validation acceptor
     */
    validateCall(call: CallStatementType, accept: ValidationAcceptor): void {
        const signature = call.callee?.ref;
        if (signature == undefined) {
            return;
        }
        const calleeName = signature.name;

        const passed = new Set<string>();
        for (const argument of call.arguments ?? []) {
            const parameter = argument.parameter?.ref;
            if (parameter == undefined) {
                continue;
            }
            if (parameter.direction !== "in") {
                accept(
                    "error",
                    `'${parameter.name}' is an output of '${calleeName}'; bind it after '->', e.g. '-> (${parameter.name}: name)'.`,
                    { node: argument, property: "parameter" }
                );
                continue;
            }
            if (passed.has(parameter.name)) {
                accept("error", `The input '${parameter.name}' of '${calleeName}' is passed twice.`, {
                    node: argument,
                    property: "parameter"
                });
            }
            passed.add(parameter.name);
        }

        const missing = requiredInputs(signature).filter((p) => !passed.has(p.name));
        if (missing.length > 0) {
            const names = missing.map((p) => `'${p.name}'`).join(", ");
            accept("error", `Missing required input${missing.length > 1 ? "s" : ""} ${names} of '${calleeName}'.`, {
                node: call,
                property: "callee"
            });
        }

        const bound = new Set<string>();
        for (const output of call.outputs ?? []) {
            const parameter = output.parameter?.ref;
            if (parameter == undefined) {
                continue;
            }
            if (parameter.direction !== "out") {
                accept(
                    "error",
                    `'${parameter.name}' is an input of '${calleeName}'; pass it as an argument, e.g. '${parameter.name} = value'.`,
                    { node: output, property: "parameter" }
                );
                continue;
            }
            if (bound.has(parameter.name)) {
                accept("error", `The output '${parameter.name}' of '${calleeName}' is bound twice.`, {
                    node: output,
                    property: "parameter"
                });
            }
            bound.add(parameter.name);
        }
    }

    /**
     * Validates the target of a reassignment: an `out` parameter may be assigned, an `in`
     * parameter may not.
     *
     * @param reassignment The reassignment
     * @param accept The validation acceptor
     */
    validateReassignment(reassignment: PatternVariableReassignmentType, accept: ValidationAcceptor): void {
        const target = reassignment.variable?.ref;
        if (this.reflection.isInstance(target, TransformationParameter) && target.direction === "in") {
            accept(
                "error",
                `Cannot assign the input parameter '${target.name}'; only 'out' parameters and variables can be assigned.`,
                { node: reassignment, property: "variable" }
            );
        }
    }

    /**
     * Validates that a parameter or call output referenced as a node of a pattern (`name { }`,
     * `delete name`, a link end) is a model element that is bound when the pattern is matched.
     *
     * @param referenceNode The reference node, for the diagnostic
     * @param property The property holding the reference
     * @param declaration The referenced declaration
     * @param accept The validation acceptor
     * @returns True if the declaration is a parameter or call output (whether valid or not)
     */
    validateNodeReference(
        referenceNode: AstNode,
        property: string,
        declaration: AstNode,
        accept: ValidationAcceptor
    ): boolean {
        const isParameter = this.reflection.isInstance(declaration, TransformationParameter);
        const isOutput = this.reflection.isInstance(declaration, CallOutput);
        if (!isParameter && !isOutput) {
            return false;
        }
        const name = (declaration as { name?: string }).name ?? "";
        const parameter = isParameter
            ? (declaration as TransformationParameterType)
            : (declaration as { parameter?: { ref?: TransformationParameterType } }).parameter?.ref;
        if (parameter == undefined) {
            return true;
        }
        if (this.declarations.getParameterKind(parameter) !== "element") {
            accept(
                "error",
                `'${name}' is a ${isParameter ? "scalar parameter" : "scalar output"}, not a model element, so it cannot be a node of a pattern. Use it in expressions instead.`,
                { node: referenceNode, property }
            );
            return true;
        }
        if (isParameter && parameter.direction === "out") {
            accept(
                "error",
                `'${name}' is an output parameter: it is not bound when the transformation starts. Assign it inside a pattern with '${name} = node'.`,
                { node: referenceNode, property }
            );
            return true;
        }
        if (parameterTypeName(this.reflection, parameter)?.isNullable === true) {
            accept("error", `'${name}' may be null (its type is nullable), so it cannot be a node of a pattern.`, {
                node: referenceNode,
                property
            });
        }
        return true;
    }

    /**
     * Validates the parameters of a signature.
     *
     * @param signature The signature
     * @param accept The validation acceptor
     */
    private validateSignature(signature: TransformationSignatureType, accept: ValidationAcceptor): void {
        for (const parameter of signature.parameters ?? []) {
            const kind = this.declarations.getParameterKind(parameter);
            const typeName = parameterTypeName(this.reflection, parameter);
            if (kind === "unsupported") {
                accept(
                    "error",
                    typeName?.hasTypeArgs === true
                        ? `Collection parameters are not supported yet; '${parameter.name}' must be a single model element or a scalar.`
                        : `The type of '${parameter.name}' must be a class or enum of the metamodel, or one of int, long, float, double, string and boolean.`,
                    { node: parameter, property: "type" }
                );
                continue;
            }
            if (parameter.direction === "out" && parameter.defaultValue != undefined) {
                accept("error", `Only 'in' parameters can have a default value.`, {
                    node: parameter,
                    property: "defaultValue"
                });
            }
            if (kind === "element" && parameter.direction === "in") {
                if (typeName?.isNullable === true) {
                    accept(
                        "error",
                        `The element input '${parameter.name}' must not be nullable: it is a bound node of the transformation's patterns.`,
                        { node: parameter, property: "type" }
                    );
                }
                if (parameter.defaultValue != undefined) {
                    accept("error", `The element input '${parameter.name}' cannot have a default value.`, {
                        node: parameter,
                        property: "defaultValue"
                    });
                }
            }
        }
    }

    /**
     * Validates one import of other transformations: the file, the metamodel compatibility of
     * the imported transformation and the absence of import cycles.
     *
     * @param transformation The importing transformation
     * @param fileImport The import
     * @param accept The validation acceptor
     */
    private validateImport(
        transformation: ModelTransformationType,
        fileImport: TransformationFileImportType,
        accept: ValidationAcceptor
    ): void {
        const file = fileImport.file;
        if (file == undefined || file.trim() === "") {
            return;
        }
        const document = AstUtils.getDocument(transformation);
        if (climbsAboveProjectRoot(document, file)) {
            accept("error", `Import path '${file}' points outside the project.`, {
                node: fileImport,
                property: "file"
            });
            return;
        }
        const target = resolveRelativeDocument(document, file, this.services.shared.workspace.LangiumDocuments);
        if (target == undefined) {
            accept("error", `Cannot resolve import path '${file}'. The file does not exist or is not loaded.`, {
                node: fileImport,
                property: "file"
            });
            return;
        }
        const targetRoot = target.parseResult?.value;
        if (targetRoot == undefined || !this.reflection.isInstance(targetRoot, ModelTransformation)) {
            accept("error", `The path '${file}' does not point to a model transformation file.`, {
                node: fileImport,
                property: "file"
            });
            return;
        }
        const callee = targetRoot as ModelTransformationType;
        if (callee.signature == undefined) {
            accept("error", `'${file}' declares no signature ('transformation name(…)'), so it cannot be called.`, {
                node: fileImport,
                property: "file"
            });
        } else {
            for (const entityImport of fileImport.imports ?? []) {
                const name = entityImport.entity?.$refText;
                if (entityImport.entity?.ref == undefined && name != undefined && name !== callee.signature.name) {
                    accept(
                        "error",
                        `'${file}' declares the transformation '${callee.signature.name}', not '${name}'.`,
                        {
                            node: entityImport,
                            property: "entity"
                        }
                    );
                }
            }
        }

        if (UriUtils.equals(target.uri, document.uri)) {
            accept("error", `A transformation cannot import itself; recursive calls are not supported.`, {
                node: fileImport,
                property: "file"
            });
            return;
        }
        const cycle = this.findImportCycle(document, target);
        if (cycle != undefined) {
            accept("error", `Import cycle: ${cycle.join(" -> ")}. Recursive calls are not supported.`, {
                node: fileImport,
                property: "file"
            });
        }

        this.validateCalleeMetamodel(transformation, document, callee, target, file, fileImport, accept);
    }

    /**
     * Validates that the imported transformation's metamodel is this transformation's
     * metamodel or one that this transformation's metamodel (transitively) imports.
     */
    private validateCalleeMetamodel(
        transformation: ModelTransformationType,
        document: LangiumDocument,
        callee: ModelTransformationType,
        calleeDocument: LangiumDocument,
        file: string,
        fileImport: TransformationFileImportType,
        accept: ValidationAcceptor
    ): void {
        const documents = this.services.shared.workspace.LangiumDocuments;
        const callerMetamodel = resolveRelativeDocument(document, transformation.import?.file, documents);
        const calleeMetamodel = resolveRelativeDocument(calleeDocument, callee.import?.file, documents);
        if (callerMetamodel == undefined || calleeMetamodel == undefined) {
            return;
        }
        if (!isMetamodelCompatible(calleeMetamodel, callerMetamodel, documents)) {
            accept(
                "error",
                `'${file}' uses the metamodel '${callee.import.file}', which is neither this transformation's metamodel nor imported by it. A transformation can only call transformations of its own metamodel or of a metamodel its metamodel imports.`,
                { node: fileImport, property: "file" }
            );
        }
    }

    /**
     * Searches the imports reachable from [start] for a path back to [origin].
     *
     * @param origin The importing document
     * @param start The imported document
     * @returns The file names of the cycle, from origin back to origin, or undefined
     */
    private findImportCycle(origin: LangiumDocument, start: LangiumDocument): string[] | undefined {
        const documents = this.services.shared.workspace.LangiumDocuments;
        const visited = new Set<string>();
        const fileName = (doc: LangiumDocument) => UriUtils.basename(doc.uri);
        const search = (doc: LangiumDocument, path: string[]): string[] | undefined => {
            if (UriUtils.equals(doc.uri, origin.uri)) {
                return path;
            }
            const key = doc.uri.toString();
            if (visited.has(key)) {
                return undefined;
            }
            visited.add(key);
            const root = doc.parseResult?.value;
            if (!this.reflection.isInstance(root, ModelTransformation)) {
                return undefined;
            }
            for (const fileImport of (root as ModelTransformationType).imports ?? []) {
                if (fileImport.file == undefined) {
                    continue;
                }
                const next = documents.getDocument(resolveRelativePath(doc, fileImport.file));
                if (next != undefined) {
                    const found = search(next, [...path, fileName(next)]);
                    if (found != undefined) {
                        return found;
                    }
                }
            }
            return undefined;
        };
        return search(start, [fileName(origin), fileName(start)]);
    }

    /**
     * Validates that every non-nullable output is assigned on every path on which the
     * transformation succeeds: at its end and at every `stop`. A path that ends in `kill` or in
     * a failing match is not a successful path.
     *
     * @param transformation The transformation root
     * @param signature The signature
     * @param accept The validation acceptor
     */
    private validateOutputsAssigned(
        transformation: ModelTransformationType,
        signature: TransformationSignatureType,
        accept: ValidationAcceptor
    ): void {
        const required = (signature.parameters ?? []).filter(
            (p) =>
                p.direction === "out" &&
                parameterTypeName(this.reflection, p)?.isNullable !== true &&
                this.declarations.getParameterKind(p) !== "unsupported"
        );
        if (required.length === 0) {
            return;
        }
        const stops: { stop: StopStatementType; assigned: Set<string> }[] = [];
        const atEnd = this.analyzeStatements(transformation.statements ?? [], new Set(), stops);

        const missingAt = (assigned: Set<string>) => required.filter((p) => !assigned.has(p.name));
        for (const { stop, assigned } of stops) {
            const missing = missingAt(assigned);
            if (missing.length > 0) {
                accept(
                    "error",
                    `The transformation returns here without assigning ${this.describe(missing)}. Assign ${missing.length > 1 ? "them" : "it"} before 'stop', or declare ${missing.length > 1 ? "them" : "it"} nullable (e.g. '${missing[0].name}: ${parameterTypeName(this.reflection, missing[0])?.name ?? "T"}?').`,
                    { node: stop }
                );
            }
        }
        if (atEnd != undefined) {
            for (const parameter of missingAt(atEnd)) {
                accept(
                    "error",
                    `The output '${parameter.name}' is not assigned on every path on which the transformation succeeds. Assign it with '${parameter.name} = …' in a pattern that always runs, or declare it nullable ('${parameter.name}: ${parameterTypeName(this.reflection, parameter)?.name ?? "T"}?').`,
                    { node: parameter, property: "name" }
                );
            }
        }
    }

    /**
     * Describes a list of outputs for a message.
     */
    private describe(parameters: TransformationParameterType[]): string {
        const names = parameters.map((p) => `'${p.name}'`).join(", ");
        return parameters.length > 1 ? `the outputs ${names}` : `the output ${names}`;
    }

    /**
     * Computes which outputs are definitely assigned when a block of statements completes.
     *
     * @param statements The statements, in order
     * @param assigned The outputs assigned before the block
     * @param stops Collects every `stop` with the outputs assigned when it is reached
     * @returns The outputs assigned after the block, or undefined if it never completes normally
     */
    private analyzeStatements(
        statements: BaseTransformationStatementType[],
        assigned: Set<string>,
        stops: { stop: StopStatementType; assigned: Set<string> }[]
    ): Set<string> | undefined {
        let current: Set<string> | undefined = new Set(assigned);
        for (const statement of statements) {
            if (current == undefined) {
                return undefined;
            }
            current = this.analyzeStatement(statement, current, stops);
        }
        return current;
    }

    /**
     * Computes the outputs definitely assigned after one statement.
     */
    private analyzeStatement(
        statement: BaseTransformationStatementType,
        assigned: Set<string>,
        stops: { stop: StopStatementType; assigned: Set<string> }[]
    ): Set<string> | undefined {
        const block = (scope: StatementsScopeType | undefined, before: Set<string>) =>
            this.analyzeStatements(scope?.statements ?? [], before, stops);

        if (this.reflection.isInstance(statement, MatchStatement)) {
            return this.withPatternAssignments(assigned, statement.pattern);
        }
        if (this.reflection.isInstance(statement, IfMatchStatement)) {
            const ifMatch = statement as IfMatchStatementType;
            const thenResult = block(
                ifMatch.ifBlock?.thenBlock,
                this.withPatternAssignments(assigned, ifMatch.ifBlock?.pattern)
            );
            const elseResult = ifMatch.elseBlock != undefined ? block(ifMatch.elseBlock, assigned) : assigned;
            return this.intersect([thenResult, elseResult]);
        }
        if (
            this.reflection.isInstance(statement, WhileMatchStatement) ||
            this.reflection.isInstance(statement, ForMatchStatement)
        ) {
            const loop = statement as WhileMatchStatementType | ForMatchStatementType;
            block(loop.doBlock, this.withPatternAssignments(assigned, loop.pattern));
            return assigned;
        }
        if (this.reflection.isInstance(statement, UntilMatchStatement)) {
            const loop = statement as UntilMatchStatementType;
            block(loop.doBlock, assigned);
            return this.withPatternAssignments(assigned, loop.pattern);
        }
        if (this.reflection.isInstance(statement, IfExpressionStatement)) {
            const ifExpression = statement as IfExpressionStatementType;
            const results = [
                block(ifExpression.thenBlock, assigned),
                ...(ifExpression.elseIfBranches ?? []).map((branch) => block(branch.block, assigned)),
                ifExpression.elseBlock != undefined ? block(ifExpression.elseBlock, assigned) : assigned
            ];
            return this.intersect(results);
        }
        if (this.reflection.isInstance(statement, WhileExpressionStatement)) {
            block((statement as WhileExpressionStatementType).block, assigned);
            return assigned;
        }
        if (this.reflection.isInstance(statement, StopStatement)) {
            const stop = statement as StopStatementType;
            if (stop.keyword === "stop") {
                stops.push({ stop, assigned });
            }
            return undefined;
        }
        if (this.reflection.isInstance(statement, CallStatement)) {
            return assigned;
        }
        return assigned;
    }

    /**
     * Adds the outputs a pattern assigns (`out = expression`).
     */
    private withPatternAssignments(assigned: Set<string>, pattern: PatternType | undefined): Set<string> {
        const result = new Set(assigned);
        for (const element of pattern?.elements ?? []) {
            if (this.reflection.isInstance(element, PatternVariableReassignment)) {
                const target = (element as PatternVariableReassignmentType).variable?.ref;
                if (this.reflection.isInstance(target, TransformationParameter)) {
                    result.add(target.name);
                }
            }
        }
        return result;
    }

    /**
     * Intersects the outputs assigned on several paths that join; paths that do not complete
     * normally (undefined) do not take part.
     */
    private intersect(paths: (Set<string> | undefined)[]): Set<string> | undefined {
        const completing = paths.filter((p): p is Set<string> => p != undefined);
        if (completing.length === 0) {
            return undefined;
        }
        return new Set([...completing[0]].filter((name) => completing.every((p) => p.has(name))));
    }
}
