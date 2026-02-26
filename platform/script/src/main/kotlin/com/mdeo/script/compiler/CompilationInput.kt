package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedAst

/**
 * Input for the script compiler.
 * Contains the TypedAST for each file to be compiled.
 */
data class CompilationInput(
    /**
     * Map of file path to the TypedAST for that file.
     * The file path is used to generate unique class names.
     */
    val files: Map<String, TypedAst>
)
