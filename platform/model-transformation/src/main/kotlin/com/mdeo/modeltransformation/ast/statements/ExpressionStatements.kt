package com.mdeo.modeltransformation.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Else-if branch in an if-expression statement.
 *
 * Represents one else-if condition and its associated block of statements.
 *
 * @param condition The boolean condition expression.
 * @param block Statements to execute if the condition is true.
 */
@Serializable
data class TypedElseIfBranch(
    @Contextual val condition: TypedExpression,
    val block: List<@Contextual TypedTransformationStatement>
)

/**
 * If-expression statement for conditional execution based on expressions.
 *
 * Unlike if-match which uses pattern matching, this statement evaluates
 * boolean expressions to determine control flow.
 *
 * @param kind Always "ifExpression" for this statement type.
 * @param condition The boolean condition expression.
 * @param thenBlock Statements to execute if the condition is true.
 * @param elseIfBranches Array of else-if branches to check in order.
 * @param elseBlock Optional statements to execute if no conditions are true.
 */
@Serializable
data class TypedIfExpressionStatement(
    override val kind: String = "ifExpression",
    @Contextual val condition: TypedExpression,
    val thenBlock: List<@Contextual TypedTransformationStatement>,
    val elseIfBranches: List<TypedElseIfBranch>,
    val elseBlock: List<@Contextual TypedTransformationStatement>? = null
) : TypedTransformationStatement

/**
 * While-expression statement for repeated execution based on an expression.
 *
 * Repeatedly evaluates the condition and executes the block as long as
 * the condition remains true.
 *
 * @param kind Always "whileExpression" for this statement type.
 * @param condition The boolean condition expression.
 * @param block Statements to execute while the condition is true.
 */
@Serializable
data class TypedWhileExpressionStatement(
    override val kind: String = "whileExpression",
    @Contextual val condition: TypedExpression,
    val block: List<@Contextual TypedTransformationStatement>
) : TypedTransformationStatement
