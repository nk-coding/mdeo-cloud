package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedAst
import com.mdeo.script.ast.TypedPluginAst

/**
 * Input for the script compiler.
 * Contains the TypedAST for each file to be compiled together with an optional
 * plugin contribution AST.
 */
data class CompilationInput(
    /**
     * Map of file path to the TypedAST for that file.
     * The file path is used to generate unique class names.
     */
    val files: Map<String, TypedAst>,

    /**
     * Optional TypedAST for all plugin-contributed functions.
     * When present, the plugin functions are compiled into the same
     * [CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME] class as the regular script
     * functions and become callable via [extension call][com.mdeo.expression.ast.expressions.TypedExtensionCallExpression]
     * expressions.
     */
    val pluginAst: TypedPluginAst? = null
)
