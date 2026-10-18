import type { AstReflection } from "@mdeo/language-common";
import { getExportedEntitiesByPath, type ClassType, type EnumType } from "@mdeo/language-metamodel";
import { resolveRelativePath, sharedImport } from "@mdeo/language-shared";
import type { AstNode, LangiumDocuments } from "langium";
import {
    CallOutput,
    ModelTransformation,
    PatternObjectInstance,
    TransformationParameter,
    typeTypes,
    type CallOutputType,
    type ModelTransformationType,
    type PatternObjectInstanceType,
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
 * - `unsupported`: anything else (collections, lambdas, `Any`), which stage 1 does not support.
 */
export type ParameterKind = "element" | "scalar" | "unsupported";

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
    const type = parameter.type as AstNode | undefined;
    if (type == undefined || !reflection.isInstance(type, typeTypes.classTypeType)) {
        return undefined;
    }
    const classType = type as unknown as { name?: string; isNullable?: boolean; typeArgs?: unknown[] };
    if (classType.name == undefined) {
        return undefined;
    }
    return {
        name: classType.name,
        isNullable: classType.isNullable === true,
        hasTypeArgs: (classType.typeArgs?.length ?? 0) > 0
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
     */
    constructor(
        private readonly reflection: AstReflection,
        private readonly documents: LangiumDocuments
    ) {}

    /**
     * Returns the class of an object declaration: the class of a pattern object, the class an
     * element parameter is typed with, or the class of the callee parameter a call output binds.
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
        if (typeName == undefined || typeName.hasTypeArgs) {
            return "unsupported";
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
        const metamodelDocument = this.getMetamodelDocument(parameter);
        if (metamodelDocument == undefined) {
            return undefined;
        }
        for (const entities of getExportedEntitiesByPath(metamodelDocument, this.documents)) {
            const cls = entities.classes.find((c) => c.name === typeName.name);
            if (cls != undefined) {
                return { kind: "class", node: cls };
            }
            const enm = entities.enums.find((e) => e.name === typeName.name);
            if (enm != undefined) {
                return { kind: "enum", node: enm };
            }
        }
        return undefined;
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
 * default value.
 *
 * @param signature The signature (may be undefined)
 * @returns The required input parameters
 */
export function requiredInputs(signature: TransformationSignatureType | undefined): TransformationParameterType[] {
    return (signature?.parameters ?? []).filter((p) => p.direction === "in" && p.defaultValue == undefined);
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
}

/**
 * Returns the class of an object declaration (pattern object, element parameter or call
 * output), for code that has the language services at hand but no resolver of its own.
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
        services.shared.workspace.LangiumDocuments
    ).getDeclaredClass(declaration);
}
