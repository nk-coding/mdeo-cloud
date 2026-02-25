import {
    Script,
    type ResolvedScriptContributionPlugins,
    type ScriptServices,
    type TypedAst,
    type TypedFunction
} from "@mdeo/language-script";
import { hasErrors, type FileDataHandler } from "@mdeo/service-common";
import { ScriptTypedAstConverter } from "./scriptTypedAstConverter.js";
import type { ReturnType } from "@mdeo/language-expression";
import { ScriptTypedAstMerger } from "./typedAstMerger.js";

/**
 * Key for the typed AST handler.
 */
export const TYPED_AST_HANDLER_KEY = "typed-ast";

/**
 * The root AST provides access to all functions contributed by plugins
 */
interface TypedRootAst {
    /**
     * Array of all types used in the plugin functions.
     * Indexed by typeIndex in expressions.
     */
    types: ReturnType[];

    /**
     * All the functions contributed by plugins
     */
    functions: TypedPluginFunction[];
}

/**
 * A plugin function with all its overloaded signatures
 */
interface TypedPluginFunction {
    /**
     * The name of the function
     */
    name: string;
    /**
     * The signatures of the function, keyed by overload identifier
     */
    signatures: Record<string, TypedPluginFunctionSignature>;
}

/**
 * A function signature without the name
 */
type TypedPluginFunctionSignature = Omit<TypedFunction, "name">;

/**
 * Handler for computing the typed AST of a script file.
 * Converts the language AST into a typed representation suitable for code generation.
 *
 * @param context The file data context with path, content, and services
 * @returns Promise resolving to the file data result with typed AST
 */
export const typedAstHandler: FileDataHandler<TypedAst | TypedRootAst | null, ScriptServices> = async (context) => {
    const { instance, fileInfo, serverApi } = context;

    if (fileInfo == undefined) {
        const typedRootAst = createTypedRootAst(instance.services.typir.ResolvedContributionPlugins);
        return {
            data: typedRootAst,
            ...serverApi.getTrackedRequests()
        };
    }

    const document = await instance.buildDocument(fileInfo.uri);

    if (hasErrors(document)) {
        return {
            data: null,
            ...serverApi.getTrackedRequests()
        };
    }

    const script = document.parseResult.value;
    const reflection = instance.services.shared.AstReflection;
    if (!reflection.isInstance(script, Script)) {
        throw new Error("Document root is not a Script");
    }

    const converter = new ScriptTypedAstConverter(instance.services.typir, reflection);
    const typedRoot = converter.convertScript(script, document);

    return {
        data: typedRoot,
        ...serverApi.getTrackedRequests()
    };
};

/**
 * Creates the typed root AST containing all contributed functions from plugins
 *
 * @param resolvedPlugins The resolved script contribution plugins
 * @returns The typed root AST
 */
function createTypedRootAst(resolvedPlugins: ResolvedScriptContributionPlugins): TypedRootAst {
    const functions: TypedPluginFunction[] = [];
    const merger = new ScriptTypedAstMerger();

    for (const [functionName, resolvedFunction] of resolvedPlugins.functions.entries()) {
        const signatures: Record<string, TypedPluginFunctionSignature> = {};

        for (const [overloadId, contributedSignature] of Object.entries(
            resolvedFunction.contributedFunction.signatures
        )) {
            const typedFunction: TypedFunction = {
                name: functionName,
                parameters: contributedSignature.signature.parameters.map((param) => ({
                    name: param.name,
                    type: merger.addTypeToGlobal(param.type)
                })),
                returnType: merger.addTypeToGlobal(contributedSignature.signature.returnType),
                body: contributedSignature.implementation
            };

            const remappedFunction = merger.remapFunction(typedFunction, resolvedFunction.types);

            signatures[overloadId] = {
                parameters: remappedFunction.parameters,
                returnType: remappedFunction.returnType,
                body: remappedFunction.body
            };
        }

        functions.push({
            name: functionName,
            signatures
        });
    }

    return {
        types: merger.getGlobalTypes(),
        functions
    };
}
