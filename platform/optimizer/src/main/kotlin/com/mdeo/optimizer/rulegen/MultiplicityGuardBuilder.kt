package com.mdeo.optimizer.rulegen

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.expressions.TypedMemberCallExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.modeltransformation.ast.patterns.TypedPatternWhereClauseElement
import com.mdeo.modeltransformation.ast.patterns.TypedWhereClause

/**
 * Builds multiplicity-guard `where` clauses and manages the companion `types` array
 * required by [com.mdeo.modeltransformation.ast.TypedAst] expressions.
 *
 * Each generated mutation rule may need `.size()` checks to prevent mutations that
 * would violate metamodel multiplicity constraints.  This builder:
 *
 * - Maintains a deduplication-safe list of [ReturnType] entries (builtin + metamodel types).
 * - Provides factory methods ([buildUpperBoundGuard] / [buildLowerBoundGuard]) that produce
 *   ready-to-use [TypedPatternWhereClauseElement] instances referencing the correct type indices.
 *
 * Standard layout of the `types` array (indices 0–5 are reserved builtins):
 *
 * | Index | Type             |
 * |-------|------------------|
 * | 0     | void             |
 * | 1     | builtin.string   |
 * | 2     | builtin.double   |
 * | 3     | builtin.boolean  |
 * | 4     | builtin.Any?     |
 * | 5     | builtin.int      |
 *
 * Metamodel class types and `List<Class>` types are appended lazily via
 * [getOrAddClassType] and [getOrAddListType].
 *
 * @param metamodelPath The metamodel path stored in the TypedAst, used to construct
 *                      `class/<path>` package prefixes for metamodel type entries.
 */
class MultiplicityGuardBuilder(private val metamodelPath: String) {

    companion object {
        /**
         * Type-array index for `void`. 
         */
        const val VOID_INDEX = 0

        /**
         * Type-array index for `builtin.string`. 
         */
        const val STRING_INDEX = 1

        /**
         * Type-array index for `builtin.double`. 
         */
        const val DOUBLE_INDEX = 2

        /**
         * Type-array index for `builtin.boolean`. 
         */
        const val BOOLEAN_INDEX = 3

        /**
         * Type-array index for `builtin.Any?`. 
         */
        const val ANY_INDEX = 4

        /**
         * Type-array index for `builtin.int`. 
         */
        const val INT_INDEX = 5
    }

    private val types = mutableListOf<ReturnType>(
        VoidType(),
        ClassTypeRef(`package` = "builtin", type = "string", isNullable = false),
        ClassTypeRef(`package` = "builtin", type = "double", isNullable = false),
        ClassTypeRef(`package` = "builtin", type = "boolean", isNullable = false),
        ClassTypeRef(`package` = "builtin", type = "Any", isNullable = true),
        ClassTypeRef(`package` = "builtin", type = "int", isNullable = false),
    )

    private val classTypeIndices = mutableMapOf<String, Int>()
    private val listTypeIndices = mutableMapOf<String, Int>()

    /**
     * Returns the type-array index for a metamodel class, adding a new entry if it
     * has not been registered yet.
     *
     * The resulting [ClassTypeRef] uses `package = "class" + metamodelPath` and
     * `type = className`.
     *
     * @param className Simple name of the metamodel class (e.g. `"Node"`).
     * @return Index into the types array.
     */
    fun getOrAddClassType(className: String): Int =
        classTypeIndices.getOrPut(className) {
            val idx = types.size
            types.add(
                ClassTypeRef(
                    `package` = "class" + metamodelPath,
                    type = className,
                    isNullable = false,
                    typeArgs = emptyMap()
                )
            )
            idx
        }

    /**
     * Returns the type-array index for `List<TargetClass>`, adding new entries as needed.
     *
     * If the target class type has not been registered yet it is added first via
     * [getOrAddClassType].
     *
     * @param targetClassName Simple name of the list element type (e.g. `"Node"`).
     * @return Index of the `List<TargetClass>` entry in the types array.
     */
    fun getOrAddListType(targetClassName: String): Int {
        getOrAddClassType(targetClassName)
        return listTypeIndices.getOrPut(targetClassName) {
            val idx = types.size
            types.add(
                ClassTypeRef(
                    `package` = "builtin",
                    type = "List",
                    isNullable = false,
                    typeArgs = mapOf(
                        "T" to ClassTypeRef(
                            `package` = "class" + metamodelPath,
                            type = targetClassName,
                            isNullable = false,
                            typeArgs = emptyMap()
                        )
                    )
                )
            )
            idx
        }
    }

    /**
     * Returns a snapshot of the current types array, suitable for
     * [com.mdeo.modeltransformation.ast.TypedAst.types].
     */
    fun getTypes(): List<ReturnType> = types.toList()

    /**
     * Builds an upper-bound guard: `where <varName>.<refName>.size() < <upperBound>`.
     *
     * Used by ADD and CREATE rules to prevent exceeding the maximum multiplicity.
     *
     * @param varName         Pattern variable name (e.g. `"source"`, `"container"`).
     * @param varClassName    Metamodel class of [varName] (for type-index resolution).
     * @param refName         Reference name on the variable (e.g. `"rooms"`).
     * @param targetClassName Target class of the reference (for `List` type resolution).
     * @param upperBound      Maximum allowed cardinality (must be > 0).
     * @return A [TypedPatternWhereClauseElement] encoding `varName.refName.size() < upperBound`.
     */
    fun buildUpperBoundGuard(
        varName: String,
        varClassName: String,
        refName: String,
        targetClassName: String,
        upperBound: Int
    ): TypedPatternWhereClauseElement {
        require(upperBound > 0) { "upperBound must be positive, was $upperBound" }
        return buildSizeGuard(varName, varClassName, refName, targetClassName, "<", upperBound)
    }

    /**
     * Builds a lower-bound guard: `where <varName>.<refName>.size() > <lowerBound>`.
     *
     * Used by REMOVE rules to prevent dropping below the minimum multiplicity.
     *
     * @param varName         Pattern variable name (e.g. `"source"`).
     * @param varClassName    Metamodel class of [varName] (for type-index resolution).
     * @param refName         Reference name on the variable (e.g. `"rooms"`).
     * @param targetClassName Target class of the reference (for `List` type resolution).
     * @param lowerBound      Minimum required cardinality (must be > 0).
     * @return A [TypedPatternWhereClauseElement] encoding `varName.refName.size() > lowerBound`.
     */
    fun buildLowerBoundGuard(
        varName: String,
        varClassName: String,
        refName: String,
        targetClassName: String,
        lowerBound: Int
    ): TypedPatternWhereClauseElement {
        require(lowerBound > 0) { "lowerBound must be positive, was $lowerBound" }
        return buildSizeGuard(varName, varClassName, refName, targetClassName, ">", lowerBound)
    }

    /**
     * Core helper that constructs the expression tree for
     * `where <varName>.<refName>.size() <operator> <bound>`.
     *
     * Expression tree structure:
     * ```
     * TypedBinaryExpression(boolean, operator,
     *   left  = TypedMemberCallExpression(int, "size", args=[],
     *             expression = TypedMemberAccessExpression(List<Target>, refName,
     *               expression = TypedIdentifierExpression(OwnerClass, varName, scope=1)
     *             )
     *           ),
     *   right = TypedIntLiteralExpression(int, bound)
     * )
     * ```
     */
    private fun buildSizeGuard(
        varName: String,
        varClassName: String,
        refName: String,
        targetClassName: String,
        operator: String,
        bound: Int
    ): TypedPatternWhereClauseElement {
        val objectTypeIndex = getOrAddClassType(varClassName)
        val listTypeIndex = getOrAddListType(targetClassName)

        val identifierExpr = TypedIdentifierExpression(
            evalType = objectTypeIndex,
            name = varName,
            scope = 1
        )

        val memberAccessExpr = TypedMemberAccessExpression(
            evalType = listTypeIndex,
            expression = identifierExpr,
            member = refName,
            isNullChaining = false
        )

        val sizeCallExpr = TypedMemberCallExpression(
            evalType = INT_INDEX,
            expression = memberAccessExpr,
            member = "size",
            isNullChaining = false,
            overload = "",
            arguments = emptyList()
        )

        val boundExpr = TypedIntLiteralExpression(
            evalType = INT_INDEX,
            value = bound.toString()
        )

        val binaryExpr = TypedBinaryExpression(
            evalType = BOOLEAN_INDEX,
            operator = operator,
            left = sizeCallExpr,
            right = boundExpr
        )

        return TypedPatternWhereClauseElement(
            whereClause = TypedWhereClause(expression = binaryExpr)
        )
    }
}
