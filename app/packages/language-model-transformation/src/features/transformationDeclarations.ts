import type { AstReflection } from "@mdeo/language-common";
import { getExportedEntitiesByPath, type ClassType, type EnumType } from "@mdeo/language-metamodel";
import { resolveRelativePath, sharedImport } from "@mdeo/language-shared";
import type { AstNode, LangiumDocuments } from "langium";
import {
    CallOutput,
    ForInVariable,
    ForMatchStatement,
    IfMatchStatement,
    MatchStatement,
    ModelTransformation,
    PatternObjectInstance,
    TransformationParameter,
    WhileMatchStatement,
    typeTypes,
    type CallOutputType,
    type ForInStatementType,
    type ForInVariableType,
    type IfMatchStatementType,
    type ModelTransformationType,
    type PatternObjectInstanceType,
    type PatternType,
    type TransformationParameterType,
    type TransformationSignatureType
} from "../grammar/modelTransformationTypes.js";

const { AstUtils } = sharedImport("langium");

/**
 * What the type of a transformation parameter denotes.
 *
 * - `element`: a class of the metamodel, so the parameter is a model element (a node of the
 *   model graph) that patterns can reference as `name { }`;
 * - `scalar`: a primitive type or an enum of the metamodel, so the parameter is a value;
 * - `collection`: a `List<T>` or `Set<T>` of model elements or of scalars;
 * - `unsupported`: anything else (other generic types, lambdas, `Any`).
 */
export type ParameterKind = "element" | "scalar" | "collection" | "unsupported";

/**
 * The collection types a collection parameter may have.
 */
export const COLLECTION_PARAMETER_TYPES: ReadonlySet<string> = new Set(["List", "Set"]);

/**
 * What a collection parameter holds.
 */
export interface CollectionShape {
    /**
     * `List` (ordered, duplicates allowed) or `Set` (no duplicates).
     */
    collection: "List" | "Set";
    /**
     * Whether the elements are model elements or scalars.
     */
    elementKind: "element" | "scalar";
    /**
     * The class of model elements.
     */
    elementClass?: ClassType;
}

/**
 * The part of the Typir services the resolver uses to find the element type of the collection
 * a `for … in` loop iterates over.
 */
export interface TypeInference {
    inferType(node: AstNode): unknown;
}

/**
 * The primitive types a scalar parameter may have.
 */
export const SCALAR_PRIMITIVE_TYPES: ReadonlySet<string> = new Set([
    "int",
    "long",
    "float",
    "double",
    "string",
    "boolean"
]);

/**
 * A parameter's type, as written: its name and whether it is nullable.
 */
export interface ParameterTypeName {
    name: string;
    isNullable: boolean;
    hasTypeArgs: boolean;
    /**
     * The type arguments as written, e.g. `Component` of `List<Component>`.
     */
    typeArgs: ParameterTypeName[];
}

/**
 * Reads the name of a parameter's type, if it is a plain class type (`Name`, `Name?`,
 * `pkg.Name`). Lambda types yield `undefined`.
 *
 * @param reflection The AST reflection
 * @param parameter The parameter
 * @returns The type name, or undefined for other types
 */
export function parameterTypeName(
    reflection: AstReflection,
    parameter: TransformationParameterType
): ParameterTypeName | undefined {
    return typeNameOf(reflection, parameter.type as AstNode | undefined);
}

/**
 * Reads the name of a written type (`Name`, `Name?`, `List<Name>`).
 *
 * @param reflection The AST reflection
 * @param type The type node
 * @returns The type name, or undefined for lambda types
 */
function typeNameOf(reflection: AstReflection, type: AstNode | undefined): ParameterTypeName | undefined {
    if (type == undefined || !reflection.isInstance(type, typeTypes.classTypeType)) {
        return undefined;
    }
    const classType = type as unknown as { name?: string; isNullable?: boolean; typeArgs?: AstNode[] };
    if (classType.name == undefined) {
        return undefined;
    }
    const typeArgs = (classType.typeArgs ?? [])
        .map((arg) => typeNameOf(reflection, arg))
        .filter((arg): arg is ParameterTypeName => arg != undefined);
    return {
        name: classType.name,
        isNullable: classType.isNullable === true,
        hasTypeArgs: (classType.typeArgs?.length ?? 0) > 0,
        typeArgs
    };
}

/**
 * Resolves what the instances, parameters and call outputs of a transformation denote: which
 * metamodel class an object declaration has, and whether a parameter is a model element or a
 * scalar.
 *
 * Parameter types are written with the expression language's type syntax (`AllocationContext`,
 * `int`, `Kind?`); a type names a model element when its name is a class of the transformation's
 * metamodel (including the metamodels it imports), exactly as the class of a pattern object is
 * resolved.
 */
export class TransformationDeclarationResolver {
    /**
     * @param reflection The AST reflection
     * @param documents The Langium documents, for the metamodels of the transformations
     * @param inference Returns the Typir inference, used for the element class of `for … in`
     *   loops; without it a loop variable has no class
     */
    constructor(
        private readonly reflection: AstReflection,
        private readonly documents: LangiumDocuments,
        private readonly inference?: () => TypeInference | undefined
    ) {}

    /**
     * Returns the class of an object declaration: the class of a pattern object, the class an
     * element parameter is typed with, the class of the callee parameter a call output binds, or
     * the element class of the collection a `for … in` loop variable iterates over.
     *
     * @param declaration The declaration (may be undefined)
     * @returns The class, or undefined when there is none
     */
    getDeclaredClass(declaration: AstNode | undefined): ClassType | undefined {
        if (declaration == undefined) {
            return undefined;
        }
        if (this.reflection.isInstance(declaration, PatternObjectInstance)) {
            return (declaration as PatternObjectInstanceType).class?.ref as ClassType | undefined;
        }
        if (this.reflection.isInstance(declaration, TransformationParameter)) {
            return this.getParameterClass(declaration as TransformationParameterType);
        }
        if (this.reflection.isInstance(declaration, CallOutput)) {
            const parameter = (declaration as CallOutputType).parameter?.ref;
            return parameter != undefined ? this.getParameterClass(parameter) : undefined;
        }
        if (this.reflection.isInstance(declaration, ForInVariable)) {
            return this.getLoopElementClass(declaration as ForInVariableType);
        }
        return undefined;
    }

    /**
     * Returns the class of the elements a `for … in` loop iterates over, from the type of its
     * collection expression (`List<T>`, `Set<T>` or any other collection of a class `T`).
     *
     * @param variable The loop variable
     * @returns The class, or undefined for collections of scalars and untyped collections
     */
    getLoopElementClass(variable: ForInVariableType): ClassType | undefined {
        const statement = variable.$container as ForInStatementType | undefined;
        const inference = this.inference?.();
        if (statement?.collection == undefined || inference == undefined) {
            return undefined;
        }
        let type: unknown;
        try {
            type = inference.inferType(statement.collection);
        } catch {
            return undefined;
        }
        if (Array.isArray(type)) {
            return undefined;
        }
        const definition = (type as { definition?: { typeArgs?: Record<string, unknown> } } | undefined)?.definition;
        const element = definition?.typeArgs?.["T"] as { package?: string; type?: string } | undefined;
        if (element?.package == undefined || element.type == undefined || !element.package.startsWith("class")) {
            return undefined;
        }
        return this.findClass(variable, element.type, element.package.slice("class".length));
    }

    /**
     * Whether the loop variable of a `for … in` loop is a model element.
     *
     * @param variable The loop variable
     * @returns True when the loop iterates over model elements
     */
    isElementLoopVariable(variable: ForInVariableType): boolean {
        return this.getLoopElementClass(variable) != undefined;
    }

    /**
     * Returns the shape of a collection parameter: `List` or `Set`, and whether it holds model
     * elements or scalars.
     *
     * @param parameter The parameter
     * @returns The shape, or undefined when the parameter is not a supported collection
     */
    getCollectionShape(parameter: TransformationParameterType): CollectionShape | undefined {
        const typeName = parameterTypeName(this.reflection, parameter);
        if (
            typeName == undefined ||
            !COLLECTION_PARAMETER_TYPES.has(typeName.name) ||
            typeName.typeArgs.length !== 1 ||
            typeName.isNullable
        ) {
            return undefined;
        }
        const element = typeName.typeArgs[0];
        if (element.hasTypeArgs || element.isNullable) {
            return undefined;
        }
        const collection = typeName.name as "List" | "Set";
        const entity = this.resolveTypeName(parameter, element.name);
        if (entity?.kind === "class") {
            return { collection, elementKind: "element", elementClass: entity.node };
        }
        if (entity?.kind === "enum" || SCALAR_PRIMITIVE_TYPES.has(element.name)) {
            return { collection, elementKind: "scalar" };
        }
        return undefined;
    }

    /**
     * Returns the pattern object an optional element input pins (proposal C, story diagrams'
     * maybe-bound objects): a nullable element input whose name is that of an object of the
     * transformation's first pattern. When the caller passes the input, the object is bound
     * to it; when not (or when it passes null), the object is matched freely.
     *
     * The first pattern is the pattern of the first statement when that is a `match`,
     * `if match`, `for match` or `while match`.
     *
     * @param parameter The parameter
     * @returns The pinned object, or undefined when the parameter pins none
     */
    getPinnedObject(parameter: TransformationParameterType): PatternObjectInstanceType | undefined {
        if (parameter.direction !== "in" || parameterTypeName(this.reflection, parameter)?.isNullable !== true) {
            return undefined;
        }
        if (this.getParameterKind(parameter) !== "element") {
            return undefined;
        }
        return this.firstPatternObjects(parameter).find((object) => object.name === parameter.name);
    }

    /**
     * Returns the objects declared by the first pattern of the transformation containing a node
     * (outside its `forbid` / `require` blocks).
     *
     * @param node Any node of the transformation
     * @returns The objects of the first pattern
     */
    firstPatternObjects(node: AstNode): PatternObjectInstanceType[] {
        const pattern = this.firstPattern(node);
        return (pattern?.elements ?? []).filter((element): element is PatternObjectInstanceType =>
            this.reflection.isInstance(element, PatternObjectInstance)
        );
    }

    /**
     * Returns the first pattern of the transformation containing a node: the pattern of its first
     * statement, when that is a `match`, `if match`, `for match` or `while match`.
     *
     * @param node Any node of the transformation
     * @returns The pattern, or undefined
     */
    firstPattern(node: AstNode): PatternType | undefined {
        const root = getTransformationRoot(this.reflection, node);
        const first = root?.statements?.[0];
        if (first == undefined) {
            return undefined;
        }
        if (this.reflection.isInstance(first, IfMatchStatement)) {
            return (first as IfMatchStatementType).ifBlock?.pattern;
        }
        if (
            this.reflection.isInstance(first, MatchStatement) ||
            this.reflection.isInstance(first, ForMatchStatement) ||
            this.reflection.isInstance(first, WhileMatchStatement)
        ) {
            return (first as { pattern?: PatternType }).pattern;
        }
        return undefined;
    }

    /**
     * Returns the class an element parameter is typed with.
     *
     * @param parameter The parameter
     * @returns The class, or undefined when the parameter is not typed with a class
     */
    getParameterClass(parameter: TransformationParameterType): ClassType | undefined {
        const entity = this.resolveParameterType(parameter);
        return entity?.kind === "class" ? entity.node : undefined;
    }

    /**
     * Classifies a parameter by its type.
     *
     * @param parameter The parameter
     * @returns Whether it is an element, a scalar, or not supported
     */
    getParameterKind(parameter: TransformationParameterType): ParameterKind {
        const typeName = parameterTypeName(this.reflection, parameter);
        if (typeName == undefined) {
            return "unsupported";
        }
        if (typeName.hasTypeArgs) {
            return this.getCollectionShape(parameter) != undefined ? "collection" : "unsupported";
        }
        const entity = this.resolveParameterType(parameter);
        if (entity?.kind === "class") {
            return "element";
        }
        if (entity?.kind === "enum" || SCALAR_PRIMITIVE_TYPES.has(typeName.name)) {
            return "scalar";
        }
        return "unsupported";
    }

    /**
     * Whether a declaration is an element whose value may be null: an element parameter or call
     * output typed `T?`. Such a name cannot be a node of a pattern.
     *
     * @param declaration The declaration
     * @returns True for nullable element parameters and outputs
     */
    isNullableElement(declaration: AstNode | undefined): boolean {
        let parameter: TransformationParameterType | undefined;
        if (this.reflection.isInstance(declaration, TransformationParameter)) {
            parameter = declaration as TransformationParameterType;
        } else if (this.reflection.isInstance(declaration, CallOutput)) {
            parameter = (declaration as CallOutputType).parameter?.ref;
        }
        if (parameter == undefined) {
            return false;
        }
        return parameterTypeName(this.reflection, parameter)?.isNullable === true;
    }

    /**
     * Returns the metamodel document of the transformation containing a node.
     *
     * @param node Any node of a transformation
     * @returns The metamodel document, if it is loaded
     */
    getMetamodelDocument(node: AstNode) {
        const root = AstUtils.getContainerOfType(node, (n) => this.reflection.isInstance(n, ModelTransformation)) as
            ModelTransformationType | undefined;
        const file = root?.import?.file;
        if (root == undefined || file == undefined) {
            return undefined;
        }
        const document = AstUtils.getDocument(root);
        return this.documents.getDocument(resolveRelativePath(document, file));
    }

    /**
     * Resolves the type name of a parameter against the classes and enums of the
     * transformation's metamodel.
     *
     * @param parameter The parameter
     * @returns The class or enum, if the name denotes one
     */
    private resolveParameterType(
        parameter: TransformationParameterType
    ): { kind: "class"; node: ClassType } | { kind: "enum"; node: EnumType } | undefined {
        const typeName = parameterTypeName(this.reflection, parameter);
        if (typeName == undefined) {
            return undefined;
        }
        return this.resolveTypeName(parameter, typeName.name);
    }

    /**
     * Resolves a type name against the classes and enums of the metamodel of the transformation
     * containing a node.
     *
     * @param node Any node of the transformation
     * @param name The type name
     * @returns The class or enum, if the name denotes one
     */
    private resolveTypeName(
        node: AstNode,
        name: string
    ): { kind: "class"; node: ClassType } | { kind: "enum"; node: EnumType } | undefined {
        const metamodelDocument = this.getMetamodelDocument(node);
        if (metamodelDocument == undefined) {
            return undefined;
        }
        for (const entities of getExportedEntitiesByPath(metamodelDocument, this.documents)) {
            const cls = entities.classes.find((c) => c.name === name);
            if (cls != undefined) {
                return { kind: "class", node: cls };
            }
            const enm = entities.enums.find((e) => e.name === name);
            if (enm != undefined) {
                return { kind: "enum", node: enm };
            }
        }
        return undefined;
    }

    /**
     * Finds a class of the metamodel of the transformation containing a node, by its name and
     * the path of the metamodel file declaring it.
     *
     * @param node Any node of the transformation
     * @param name The class name
     * @param path The absolute path of the declaring metamodel file
     * @returns The class, if there is one
     */
    private findClass(node: AstNode, name: string, path: string): ClassType | undefined {
        const metamodelDocument = this.getMetamodelDocument(node);
        if (metamodelDocument == undefined) {
            return undefined;
        }
        const byPath = getExportedEntitiesByPath(metamodelDocument, this.documents);
        const exact = byPath.find((entities) => entities.absolutePath === path)?.classes.find((c) => c.name === name);
        return exact ?? byPath.flatMap((entities) => entities.classes).find((c) => c.name === name);
    }
}

/**
 * Returns a declaration as a pattern object instance, if it is one.
 *
 * @param reflection The AST reflection
 * @param declaration The declaration
 * @returns The pattern object instance, or undefined for parameters and call outputs
 */
export function asPatternObjectInstance(
    reflection: AstReflection,
    declaration: AstNode | undefined
): PatternObjectInstanceType | undefined {
    return reflection.isInstance(declaration, PatternObjectInstance)
        ? (declaration as PatternObjectInstanceType)
        : undefined;
}

/**
 * Returns the transformation root of a node.
 *
 * @param reflection The AST reflection
 * @param node Any node
 * @returns The root, if the node belongs to a transformation
 */
export function getTransformationRoot(reflection: AstReflection, node: AstNode): ModelTransformationType | undefined {
    return AstUtils.getContainerOfType(node, (n) => reflection.isInstance(n, ModelTransformation)) as
        ModelTransformationType | undefined;
}

/**
 * Lists the parameters of a signature that callers must pass: the `in` parameters without a
 * default value whose type is not nullable. A nullable input without a default is optional: it
 * is null when not passed (for an element input that pins an object of the first pattern, the
 * object is then matched freely).
 *
 * @param signature The signature (may be undefined)
 * @returns The required input parameters
 */
export function requiredInputs(signature: TransformationSignatureType | undefined): TransformationParameterType[] {
    return (signature?.parameters ?? []).filter(
        (p) => p.direction === "in" && p.defaultValue == undefined && !isNullableType(p)
    );
}

/**
 * Whether a parameter's type is written nullable (`T?`).
 *
 * @param parameter The parameter
 * @returns True for a nullable type
 */
function isNullableType(parameter: TransformationParameterType): boolean {
    return (parameter.type as { isNullable?: boolean } | undefined)?.isNullable === true;
}

/**
 * Renders a signature as source text, for hovers and messages.
 *
 * @param signature The signature
 * @returns e.g. `transformation moveComponent(in maxCandidates: int = …, out moved: AllocationContext)`
 */
export function renderSignature(signature: TransformationSignatureType): string {
    const parameters = (signature.parameters ?? []).map((p) => renderParameter(p)).join(", ");
    return `transformation ${signature.name}(${parameters})`;
}

/**
 * Renders a parameter as source text.
 *
 * @param parameter The parameter
 * @returns e.g. `in maxCandidates: int = …`
 */
export function renderParameter(parameter: TransformationParameterType): string {
    const typeText = parameter.type?.$cstNode?.text ?? "?";
    const defaultText = parameter.defaultValue != undefined ? ` = ${parameter.defaultValue.$cstNode?.text ?? "…"}` : "";
    return `${parameter.direction} ${parameter.name}: ${typeText}${defaultText}`;
}

/**
 * The services {@link declaredClassOf} needs.
 */
export interface DeclarationServices {
    shared: {
        AstReflection: AstReflection;
        workspace: { LangiumDocuments: LangiumDocuments };
    };
    typir?: { Inference: TypeInference };
}

/**
 * Returns the class of an object declaration (pattern object, element parameter, call output or
 * loop variable), for code that has the language services at hand but no resolver of its own.
 *
 * @param services The language services
 * @param declaration The declaration
 * @returns The class, or undefined
 */
export function declaredClassOf(
    services: DeclarationServices,
    declaration: AstNode | undefined
): ClassType | undefined {
    return new TransformationDeclarationResolver(
        services.shared.AstReflection,
        services.shared.workspace.LangiumDocuments,
        () => services.typir?.Inference
    ).getDeclaredClass(declaration);
}
