import { ExpressionHoverProvider } from "@mdeo/language-expression";
import { sharedImport } from "@mdeo/language-shared";
import type { AstNode, LangiumDocument, MaybePromise } from "langium";
import type { Hover, HoverParams } from "vscode-languageserver-protocol";
import {
    CallArgument,
    CallOutput,
    CallStatement,
    PatternLinkEnd,
    PatternObjectInstanceDelete,
    PatternObjectInstanceReference,
    TransformationImport,
    TransformationParameter,
    TransformationSignature,
    type CallOutputType,
    type TransformationParameterType,
    type TransformationSignatureType
} from "../grammar/modelTransformationTypes.js";
import { renderParameter, renderSignature } from "./transformationDeclarations.js";

const { CstUtils, AstUtils, GrammarAST } = sharedImport("langium");

/**
 * Hover provider of the Model Transformation language: the expression hovers, plus the
 * signatures of called and imported transformations and the parameters and outputs of calls.
 */
export class ModelTransformationHoverProvider extends ExpressionHoverProvider {
    override getHoverContent(document: LangiumDocument, params: HoverParams): MaybePromise<Hover | undefined> {
        const hover = this.callHover(document, params);
        return hover ?? super.getHoverContent(document, params);
    }

    /**
     * Returns the hover for the parts of signatures and calls, if the position is on one.
     */
    private callHover(document: LangiumDocument, params: HoverParams): Hover | undefined {
        const rootCstNode = document.parseResult?.value?.$cstNode;
        if (rootCstNode == undefined) {
            return undefined;
        }
        const offset = document.textDocument.offsetAt(params.position);
        const cstNode = CstUtils.findDeclarationNodeAtOffset(rootCstNode, offset);
        if (cstNode == undefined) {
            return undefined;
        }
        const astNode = cstNode.astNode;
        const feature = AstUtils.getContainerOfType(cstNode.grammarSource, GrammarAST.isAssignment)?.feature;

        if (this.reflection.isInstance(astNode, CallStatement) && feature === "callee") {
            return this.signatureHover(astNode.callee?.ref);
        }
        if (this.reflection.isInstance(astNode, TransformationImport) && feature === "entity") {
            return this.signatureHover(astNode.entity?.ref as TransformationSignatureType | undefined);
        }
        if (this.reflection.isInstance(astNode, TransformationSignature) && feature === "name") {
            return this.signatureHover(astNode);
        }
        if (this.reflection.isInstance(astNode, TransformationParameter) && feature === "name") {
            return this.parameterHover(astNode);
        }
        if (
            (this.reflection.isInstance(astNode, CallArgument) || this.reflection.isInstance(astNode, CallOutput)) &&
            feature === "parameter"
        ) {
            return this.parameterHover(astNode.parameter?.ref);
        }
        if (this.reflection.isInstance(astNode, CallOutput) && feature === "name") {
            return this.outputHover(astNode);
        }
        if (
            (this.reflection.isInstance(astNode, PatternObjectInstanceReference) ||
                this.reflection.isInstance(astNode, PatternObjectInstanceDelete)) &&
            feature === "instance"
        ) {
            return this.declarationHover(astNode.instance?.ref);
        }
        if (this.reflection.isInstance(astNode, PatternLinkEnd) && feature === "object") {
            return this.declarationHover(astNode.object?.ref);
        }
        return undefined;
    }

    /**
     * Hover for a referenced declaration that is a parameter or call output.
     */
    private declarationHover(declaration: AstNode | undefined): Hover | undefined {
        if (this.reflection.isInstance(declaration, TransformationParameter)) {
            return this.parameterHover(declaration);
        }
        if (this.reflection.isInstance(declaration, CallOutput)) {
            return this.outputHover(declaration);
        }
        return undefined;
    }

    /**
     * Hover showing a whole signature.
     */
    private signatureHover(signature: TransformationSignatureType | undefined): Hover | undefined {
        if (signature == undefined) {
            return undefined;
        }
        const file = AstUtils.getDocument(signature).uri.path;
        return markdownHover(renderSignature(signature), `Defined in \`${file}\``);
    }

    /**
     * Hover showing one parameter and the transformation it belongs to.
     */
    private parameterHover(parameter: TransformationParameterType | undefined): Hover | undefined {
        if (parameter == undefined) {
            return undefined;
        }
        const signature = parameter.$container as TransformationSignatureType | undefined;
        const role =
            parameter.direction === "in"
                ? parameter.defaultValue != undefined
                    ? "optional input"
                    : "required input"
                : "output";
        return markdownHover(renderParameter(parameter), `${role} of \`${signature?.name ?? "?"}\``);
    }

    /**
     * Hover showing the name a call output binds and the callee parameter it comes from.
     */
    private outputHover(output: CallOutputType): Hover | undefined {
        const parameter = output.parameter?.ref;
        const signature = parameter?.$container as TransformationSignatureType | undefined;
        const typeText = parameter?.type?.$cstNode?.text ?? "?";
        return markdownHover(
            `${output.name}: ${typeText}`,
            `bound by the call: output \`${parameter?.name ?? output.parameter?.$refText}\` of \`${signature?.name ?? "?"}\``
        );
    }
}

/**
 * Creates a markdown hover with a code line and an explanation.
 *
 * @param code The code line
 * @param text The explanation below it
 * @returns The hover
 */
function markdownHover(code: string, text: string): Hover {
    return {
        contents: {
            kind: "markdown",
            value: "```\n" + code + "\n```\n" + text
        }
    };
}
