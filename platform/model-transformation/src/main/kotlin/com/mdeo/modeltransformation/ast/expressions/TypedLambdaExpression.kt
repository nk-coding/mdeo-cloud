package com.mdeo.modeltransformation.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Lambda expression in the model transformation language.
 *
 * Lambda expressions allow defining anonymous functions that can be passed
 * to higher-order functions such as collection operations (filter, map, etc.).
 *
 * @param kind Always "lambda" for this expression type.
 * @param evalType Index into the types array for the type this expression evaluates to.
 *                 This will be a function type representing the lambda's signature.
 * @param parameters Names of the lambda parameters. The types are encoded in evalType.
 * @param body Body of the lambda as a single expression to evaluate.
 */
@Serializable
data class TypedLambdaExpression(
    override val kind: String = "lambda",
    override val evalType: Int,
    val parameters: List<String>,
    @Contextual val body: TypedExpression
) : TypedExpression
