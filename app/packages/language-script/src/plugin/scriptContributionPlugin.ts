import type { FunctionSignature, FunctionType, ReturnType } from "@mdeo/language-expression";
import type { ServerContributionPlugin } from "@mdeo/plugin";
import type { TypedCallableBody } from "./typedAst.js";
import type { Interface, ParserRule, SerializedGrammar } from "@mdeo/language-common";
import type { GenericAstNode } from "langium";

/**
 * Plugin for contributing stdlib and syntax extensions for the script language
 */
export interface ScriptContributionPlugin extends ServerContributionPlugin {
    /**
     * Identifies the plugin as a Script language contribution.
     */
    type: typeof ScriptContributionPlugin.TYPE;
    /**
     * Array of all types used in the program.
     * Generics are replaced by Any? due to type erasure.
     * Indexed by typeIndex in expressions.
     * Shared for all functions defined by this plugin
     */
    types: ReturnType[];
    /**
     * If used, the serialized grammar which is used for expression contributions.
     */
    grammar: SerializedGrammar | undefined;
    /**
     * Contributed functions by their names
     */
    functions: Record<string, ContributedFunction>;
    /**
     * Contributed expressions by their names
     * Note: expressions are currently syntactic sugar over functions, and thus are also
     * registered in the global scope as functions.
     */
    expressions: Record<string, ContributedExpression>;
}

export namespace ScriptContributionPlugin {
    /**
     * The type identifier for ScriptContributionPlugin
     */
    export const TYPE = "script-language-contribution";

    /**
     * Type guard for ScriptContributionPlugin
     *
     * @param value The value to check
     * @returns True if the value is a ScriptContributionPlugin, false otherwise
     */
    export function is(value: ServerContributionPlugin): value is ScriptContributionPlugin {
        return "type" in value && value.type === TYPE;
    }
}

/**
 * A contributed function that
 */
export interface ContributedFunction {
    /**
     * The available signatures for the function.
     */
    signatures: Record<string, ContributedFunctionSignature>;
}

/**
 * A contributed function signature with its implementation
 */
export interface ContributedFunctionSignature {
    /**
     * The signature of the function
     */
    signature: FunctionSignature;
    /**
     * The implementation of the function (the body of the function)
     */
    implementation: TypedCallableBody;
}

/**
 * A contributed DSL expression which is implemented by a function
 */
export interface ContributedExpression {
    /**
     * The name of the grammar rule that implements this expression
     */
    ruleName: string;
    /**
     * The name of the grammar type that implements this expression
     */
    interfaceName: string;
    /**
     * The function that implements this expression
     */
    function: ContributedFunctionSignature;
}

/**
 * Resolved plugins containing contributions of all plugins
 */
export interface ResolvedScriptContributionPlugins {
    /**
     * The resolved functions contributed by all the plugins
     * Contains functions from both 'functions' and 'expressions' of the plugins
     */
    functions: Map<string, ResolvedContributedFunction>;
    /**
     * The resolved expressions contributed by all the plugins
     */
    expressions: ResolvedContributedExpression[];
    /**
     * The rules for all the extensions, should be combined to one expression rule
     */
    rules: ParserRule<any>[];
}

/**
 * Resolved variant of a contributed expression
 */
export interface ResolvedContributedExpression {
    /**
     * The signature used by the expression
     */
    signature: FunctionSignature;
    /**
     * The resolved type, can be used for inference/validation rules
     */
    interface: Interface<GenericAstNode>;
    /**
     * The name of the function that implements this expression
     */
    name: string;
}

/**
 * Resolved variant of a contributed function
 */
export interface ResolvedContributedFunction {
    /**
     * The function type
     */
    function: FunctionType;
    /**
     * The actual contributed function
     */
    contributedFunction: ContributedFunction;
    /**
     * The type lookup used by the implementations of the contributed function
     */
    types: ReturnType[];
}
