package com.mdeo.script.compiler

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.metamodel.data.AssociationData
import com.mdeo.metamodel.data.AssociationEndData
import com.mdeo.metamodel.data.ClassData
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.MultiplicityData
import com.mdeo.metamodel.data.PropertyData
import com.mdeo.script.compiler.model.ScriptMetamodelTypeRegistrar
import org.junit.jupiter.api.Test

/**
 * Regression test for IllegalStateException thrown during compilation when a
 * type cast expression (`as`) wraps a chain that contains a lambda.
 *
 * ## Bug description
 *
 * The `ScopeBuilder.collectFromExpression()` `when` block handles various expression
 * types and registers lambda scopes by recursing into sub-expressions.  However, it
 * had no case for `TypedTypeCastExpression`, so it fell through to the `else -> {}`
 * branch and silently skipped the inner expression.  When the inner expression
 * contained a lambda, the scope was never registered in `statementScopes`, causing
 * the compiler to throw:
 *
 *   java.lang.IllegalStateException: Lambda scope not found in scope tree.
 *   This indicates a bug in ScopeBuilder – all lambda expressions should have
 *   their scopes registered during scope building phase.
 *
 * ## Equivalent script
 *
 * ```
 * fun test(): int {
 *     return Knapsack.all().first().items.map((item) => item.gain).sum() as int
 * }
 * ```
 *
 * The workaround (no cast) compiled fine:
 * ```
 *     return Knapsack.all().first().items.map((item) => item.gain).sum().asInteger()
 * ```
 *
 * ## Fix
 *
 * Added a `is TypedTypeCastExpression` case in `ScopeBuilder.collectFromExpression()`
 * that recurses into `expression.expression` so lambdas nested inside a cast are found.
 */
class TypeCastWithLambdaTest {

    private val metamodelPath = "/test-typecast-lambda.mm"
    private val classPackage = "${ScriptMetamodelTypeRegistrar.CLASS_PACKAGE}$metamodelPath"
    private val classContainerPackage = "${ScriptMetamodelTypeRegistrar.CLASS_CONTAINER_PACKAGE}$metamodelPath"
    private val compiler = ScriptCompiler()
    private val testFilePath = "test://typecast-lambda.script"

    @Test
    fun `type cast wrapping lambda chain does not throw Lambda scope not found`() {
        // Metamodel: Knapsack --items--> Item (many), Item.gain: int
        val metamodelData = MetamodelData(
            path = metamodelPath,
            classes = listOf(
                ClassData(
                    name = "Item",
                    isAbstract = false,
                    properties = listOf(
                        PropertyData(
                            name = "gain",
                            primitiveType = "int",
                            multiplicity = MultiplicityData.single()
                        )
                    )
                ),
                ClassData(name = "Knapsack", isAbstract = false)
            ),
            associations = listOf(
                AssociationData(
                    source = AssociationEndData(
                        className = "Knapsack",
                        name = "items",
                        multiplicity = MultiplicityData.many()
                    ),
                    operator = "--",
                    target = AssociationEndData(
                        className = "Item",
                        name = "knapsack",
                        multiplicity = MultiplicityData.optional()
                    )
                )
            )
        )

        val ast = buildTypedAst {
            val intIdx          = intType()    // 0
            val doubleIdx       = doubleType() // 1
            val itemIdx         = addType(ClassTypeRef(classPackage, "Item", false))     // 2
            val knapsackIdx     = addType(ClassTypeRef(classPackage, "Knapsack", false)) // 3
            val knapsackContIdx = addType(ClassTypeRef(classContainerPackage, "Knapsack", false)) // 4
            val collIdx         = addType(ClassTypeRef("builtin", "Collection", false))  // 5
            // Lambda type: (Item) => int
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "int", false),
                "item" to ClassTypeRef(classPackage, "Item", false)
            ) // 6

            // Lambda body: return item.gain
            // `item` is the lambda parameter declared in the lambda params scope (level 4).
            val lambda = lambdaExpr(
                parameters = listOf("item"),
                body = listOf(
                    returnStmt(
                        memberAccess(
                            expression = identifier("item", itemIdx, 4),
                            member = "gain",
                            resultTypeIndex = intIdx
                        )
                    )
                ),
                lambdaTypeIndex = lambdaTypeIdx
            )

            // fun test(): int {
            //     return Knapsack.all().first().items.map((item) => item.gain).sum() as int
            // }
            function(
                name = "test",
                returnType = intIdx,
                body = listOf(
                    returnStmt(
                        typeCast(
                            expression = memberCall(
                                // .sum() on the mapped collection → double
                                expression = memberCall(
                                    // .map((item) => item.gain) on items collection
                                    expression = memberAccess(
                                        // .items on the Knapsack instance
                                        expression = memberCall(
                                            // .first() on the Knapsack collection
                                            expression = memberCall(
                                                // Knapsack.all()
                                                expression = identifier("Knapsack", knapsackContIdx, 1),
                                                member = "all",
                                                overload = "",
                                                arguments = emptyList(),
                                                resultTypeIndex = collIdx
                                            ),
                                            member = "first",
                                            overload = "",
                                            arguments = emptyList(),
                                            resultTypeIndex = knapsackIdx
                                        ),
                                        member = "items",
                                        resultTypeIndex = collIdx
                                    ),
                                    member = "map",
                                    overload = "",
                                    arguments = listOf(lambda),
                                    resultTypeIndex = collIdx
                                ),
                                member = "sum",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleIdx
                            ),
                            targetTypeIndex = intIdx,
                            resultTypeIndex = intIdx
                        )
                    )
                )
            )
        }

        val input = CompilationInput(mapOf(testFilePath to ast))
        // Before the fix this throws:
        //   java.lang.IllegalStateException: Lambda scope not found in scope tree.
        //   This indicates a bug in ScopeBuilder – all lambda expressions should have
        //   their scopes registered during scope building phase.
        // After the fix compilation succeeds without exception.
        compiler.compile(input, metamodelData)
    }
}
