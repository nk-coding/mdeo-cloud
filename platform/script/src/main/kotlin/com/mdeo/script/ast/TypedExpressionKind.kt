package com.mdeo.script.ast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enum for all expression types in the TypedAST.
 */
@Serializable
enum class TypedExpressionKind(val value: String) {
    @SerialName("unary")
    Unary("unary"),
    
    @SerialName("binary")
    Binary("binary"),
    
    @SerialName("ternary")
    Ternary("ternary"),
    
    @SerialName("call")
    ExpressionCall("call"),
    
    @SerialName("functionCall")
    FunctionCall("functionCall"),
    
    @SerialName("memberCall")
    MemberCall("memberCall"),
    
    @SerialName("extensionCall")
    ExtensionCall("extensionCall"),
    
    @SerialName("memberAccess")
    MemberAccess("memberAccess"),
    
    @SerialName("identifier")
    Identifier("identifier"),
    
    @SerialName("stringLiteral")
    StringLiteral("stringLiteral"),
    
    @SerialName("intLiteral")
    IntLiteral("intLiteral"),
    
    @SerialName("longLiteral")
    LongLiteral("longLiteral"),
    
    @SerialName("floatLiteral")
    FloatLiteral("floatLiteral"),
    
    @SerialName("doubleLiteral")
    DoubleLiteral("doubleLiteral"),
    
    @SerialName("booleanLiteral")
    BooleanLiteral("booleanLiteral"),
    
    @SerialName("nullLiteral")
    NullLiteral("nullLiteral"),
    
    @SerialName("lambda")
    Lambda("lambda")
}
