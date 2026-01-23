package com.mdeo.script.ast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enum for all statement types in the TypedAST.
 */
@Serializable
enum class TypedStatementKind(val value: String) {
    @SerialName("if")
    If("if"),
    
    @SerialName("while")
    While("while"),
    
    @SerialName("for")
    For("for"),
    
    @SerialName("variableDeclaration")
    VariableDeclaration("variableDeclaration"),
    
    @SerialName("assignment")
    Assignment("assignment"),
    
    @SerialName("expression")
    Expression("expression"),
    
    @SerialName("break")
    Break("break"),
    
    @SerialName("continue")
    Continue("continue"),
    
    @SerialName("return")
    Return("return")
}
