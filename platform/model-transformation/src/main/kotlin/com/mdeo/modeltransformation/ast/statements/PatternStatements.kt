package com.mdeo.modeltransformation.ast.statements

import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/**
 * Match statement for simple pattern matching.
 *
 * Matches a pattern once against the model. If the pattern matches, the matched
 * bindings become available for subsequent statements.
 *
 * @param kind Always "match" for this statement type.
 * @param pattern The pattern to match against the model.
 */
@Serializable
data class TypedMatchStatement(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val kind: String = "match",
    val pattern: TypedPattern
) : TypedTransformationStatement

/**
 * If-match statement for conditional execution based on pattern matching.
 *
 * Attempts to match a pattern. If successful, executes the then block with the
 * matched bindings. Otherwise, executes the optional else block.
 *
 * @param kind Always "ifMatch" for this statement type.
 * @param pattern The pattern to match.
 * @param thenBlock Statements to execute if the pattern matches.
 * @param elseBlock Optional statements to execute if the pattern does not match.
 */
@Serializable
data class TypedIfMatchStatement(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val kind: String = "ifMatch",
    val pattern: TypedPattern,
    val thenBlock: List<@Contextual TypedTransformationStatement>,
    val elseBlock: List<@Contextual TypedTransformationStatement>? = null
) : TypedTransformationStatement

/**
 * While-match statement for repeated execution while a pattern matches.
 *
 * Repeatedly matches the pattern and executes the do block as long as
 * the pattern continues to match.
 *
 * @param kind Always "whileMatch" for this statement type.
 * @param pattern The pattern to match repeatedly.
 * @param doBlock Statements to execute for each successful match.
 */
@Serializable
data class TypedWhileMatchStatement(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val kind: String = "whileMatch",
    val pattern: TypedPattern,
    val doBlock: List<@Contextual TypedTransformationStatement>
) : TypedTransformationStatement

/**
 * Until-match statement for repeated execution until a pattern matches.
 *
 * Repeatedly executes the do block until the pattern matches.
 * This is the inverse of while-match.
 *
 * @param kind Always "untilMatch" for this statement type.
 * @param pattern The pattern that, when matched, terminates the loop.
 * @param doBlock Statements to execute until the pattern matches.
 */
@Serializable
data class TypedUntilMatchStatement(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val kind: String = "untilMatch",
    val pattern: TypedPattern,
    val doBlock: List<@Contextual TypedTransformationStatement>
) : TypedTransformationStatement

/**
 * For-match statement for iterating over all pattern matches.
 *
 * Finds all matches of the pattern and executes the do block once for each match.
 *
 * @param kind Always "forMatch" for this statement type.
 * @param pattern The pattern to match repeatedly.
 * @param doBlock Statements to execute for each match found.
 */
@Serializable
data class TypedForMatchStatement(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val kind: String = "forMatch",
    val pattern: TypedPattern,
    val doBlock: List<@Contextual TypedTransformationStatement>
) : TypedTransformationStatement
