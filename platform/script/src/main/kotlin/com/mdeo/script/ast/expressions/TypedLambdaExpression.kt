package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedCallableBody
import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Lambda expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param parameters Parameters of the lambda (names only, types are in the evalType).
 * @param body Body of the lambda as statements.
 */
@Serializable
data class TypedLambdaExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.Lambda,
    override val evalType: Int,
    val parameters: List<String>,
    val body: TypedCallableBody
) : TypedExpression
