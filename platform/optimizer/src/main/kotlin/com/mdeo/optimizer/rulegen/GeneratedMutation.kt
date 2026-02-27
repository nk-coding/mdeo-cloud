package com.mdeo.optimizer.rulegen

import com.mdeo.modeltransformation.ast.TypedAst

/**
 * A generated mutation operator, produced by [MutationRuleGenerator].
 *
 * Each instance pairs a unique rule name with the [TypedAst] that implements the
 * mutation operation.  The [TypedAst] is built programmatically in Kotlin by
 * [MutationAstBuilder] and can be passed directly to [OptimizationOrchestrator]
 * as a pre-compiled transformation.
 *
 * @param name      Unique rule name (used as the map key in the transformations map).
 * @param typedAst  The ready-to-execute typed AST for the mutation.
 */
data class GeneratedMutation(
    val name: String,
    val typedAst: TypedAst
)
