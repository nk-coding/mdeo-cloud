package com.mdeo.script.compiler

import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.EnumData
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.MultiplicityData
import com.mdeo.expression.ast.types.PropertyData
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
import com.mdeo.script.ast.TypedAst
import com.mdeo.script.compiler.model.ScriptMetamodelTypeRegistrar
import com.mdeo.script.runtime.ExecutionContext
import com.mdeo.script.runtime.ExecutionEnvironment
import com.mdeo.script.runtime.model.ModelDataScriptModel
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests verifying that class container types (e.g. `House.all()`)
 * and enum container types (e.g. `Color.RED`) are correctly accessible at
 * file scope (scope level 1) during script compilation.
 *
 * Regression test for: "Variable not found: House at scope level 1"
 */
class ClassContainerAccessTest {

    // Metamodel path as an absolute path (starting with /), mirroring what the
    // TypeScript frontend sends. The TypeScript package functions use
    // `"class-container${absolutePath}"` — no extra slash — so absolute path "/foo.mm"
    // produces "class-container/foo.mm".
    private val metamodelPath = "/test-metamodel.mm"

    // Derived package prefixes (mirrors ScriptMetamodelTypeRegistrar constants + absolutePath)
    private val classPackage = "${ScriptMetamodelTypeRegistrar.CLASS_PACKAGE}$metamodelPath"
    private val classContainerPackage = "${ScriptMetamodelTypeRegistrar.CLASS_CONTAINER_PACKAGE}$metamodelPath"
    private val enumContainerPackage = "${ScriptMetamodelTypeRegistrar.ENUM_CONTAINER_PACKAGE}$metamodelPath"
    private val enumPackage = "${ScriptMetamodelTypeRegistrar.ENUM_PACKAGE}$metamodelPath"

    private val compiler = ScriptCompiler()
    private val testFilePath = "test://class-container-test.script"

    // -------------------------------------------------------------------------
    // Class container – House.all()
    // -------------------------------------------------------------------------

    /**
     * Regression test: `House.all()` must not throw
     * "Variable not found: House at scope level 1".
     *
     * The script:
     * ```
     * for (house in House.all()) {
     *     println(house.address)
     * }
     * ```
     * executed against a model with two House instances must print both addresses.
     */
    @Test
    fun `House dot all() iterates over all instances in the model`() {
        val metamodelData = MetamodelData(
            path = metamodelPath,
            classes = listOf(
                ClassData(
                    name = "House",
                    isAbstract = false,
                    properties = listOf(
                        PropertyData(
                            name = "address",
                            primitiveType = "string",
                            multiplicity = MultiplicityData.single()
                        )
                    )
                )
            )
        )

        val ast = buildHouseAllAst()
        val input = CompilationInput(mapOf(testFilePath to ast))
        val program = compiler.compile(input, metamodelData)

        val modelData = ModelData(
            metamodelUri = metamodelPath,
            instances = listOf(
                ModelDataInstance(
                    name = "house1",
                    className = "House",
                    properties = mapOf("address" to ModelDataPropertyValue.StringValue("123 Main St"))
                ),
                ModelDataInstance(
                    name = "house2",
                    className = "House",
                    properties = mapOf("address" to ModelDataPropertyValue.StringValue("456 Oak Ave"))
                )
            ),
            links = emptyList()
        )

        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos, true, Charsets.UTF_8)
        val env = ExecutionEnvironment(program)
        val model = ModelDataScriptModel(modelData, metamodelData, env.classLoader, program)

        ExecutionContext.withContext(ps, model) {
            env.invoke(testFilePath, "test")
        }
        val output = baos.toString(Charsets.UTF_8)

        val lines = output.lines().filter { it.isNotBlank() }
        assertEquals(2, lines.size, "Expected two printed addresses, got: $lines")
        assertTrue(lines.containsAll(listOf("123 Main St", "456 Oak Ave")),
            "Expected both addresses to be printed, got: $lines")
    }

    /**
     * Builds the TypedAST for:
     * ```
     * fun test() {
     *     for (house in House.all()) {
     *         println(house.address)
     *     }
     * }
     * ```
     * with types matching the compiler's expected format.
     */
    private fun buildHouseAllAst(): TypedAst = buildTypedAst {
        // Type indices
        val voidIdx = addType(VoidType())
        val stringIdx = addType(ClassTypeRef("builtin", "string", false))
        val houseIdx = addType(ClassTypeRef(classPackage, "House", false))
        val collectionHouseIdx = addType(ClassTypeRef("builtin", "Collection", false))
        val houseContainerIdx = addType(ClassTypeRef(classContainerPackage, "House", false))

        function(
            name = "test",
            returnType = voidIdx,
            body = listOf(
                // for (house in House.all()) { println(house.address) }
                forStmt(
                    variableName = "house",
                    variableType = houseIdx,
                    iterable = memberCall(
                        expression = identifier("House", houseContainerIdx, scope = 1),
                        member = "all",
                        overload = "",
                        arguments = emptyList(),
                        resultTypeIndex = collectionHouseIdx
                    ),
                    body = listOf(
                        exprStmt(
                            functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(
                                    memberAccess(
                                        expression = identifier("house", houseIdx, scope = 4),
                                        member = "address",
                                        resultTypeIndex = stringIdx
                                    )
                                ),
                                resultTypeIndex = voidIdx
                            )
                        )
                    )
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Enum container – Color.RED
    // -------------------------------------------------------------------------

    /**
     * Verifies that enum container entries are accessible at file scope (scope level 1).
     *
     * The script:
     * ```
     * fun getActiveEntry(): String {
     *     return Status.ACTIVE.getEntry()
     * }
     * ```
     * must return the string `"ACTIVE"`.
     */
    @Test
    fun `Enum container entry access returns correct entry name`() {
        val metamodelData = MetamodelData(
            path = metamodelPath,
            enums = listOf(
                EnumData(name = "Status", entries = listOf("ACTIVE", "INACTIVE"))
            )
        )

        val ast = buildEnumEntryAst()
        val input = CompilationInput(mapOf(testFilePath to ast))
        val program = compiler.compile(input, metamodelData)

        val result = ExecutionEnvironment(program).invoke(testFilePath, "getActiveEntry")

        assertEquals("ACTIVE", result,
            "Expected Status.ACTIVE.getEntry() to return \"ACTIVE\", but got: $result")
    }

    /**
     * Builds the TypedAST for:
     * ```
     * fun getActiveEntry(): String {
     *     return Status.ACTIVE.getEntry()
     * }
     * ```
     *
     * The enum container (Status) is at scope level 1. Accessing `.ACTIVE` pops
     * the container receiver and loads the static field. Then `.getEntry()` is
     * called on the resulting EnumValue instance.
     */
    private fun buildEnumEntryAst(): TypedAst = buildTypedAst {
        val stringIdx     = addType(ClassTypeRef("builtin", "string", false))
        val enumValueIdx  = addType(ClassTypeRef(enumPackage, "Status", false))
        val containerIdx  = addType(ClassTypeRef(enumContainerPackage, "Status", false))

        function(
            name = "getActiveEntry",
            returnType = stringIdx,
            body = listOf(
                returnStmt(
                    // Status.ACTIVE.getEntry()
                    memberCall(
                        expression = memberAccess(
                            // Status  (enum container, scope 1)
                            expression = identifier("Status", containerIdx, scope = 1),
                            member = "ACTIVE",
                            resultTypeIndex = enumValueIdx
                        ),
                        member = "getEntry",
                        overload = "",
                        arguments = emptyList(),
                        resultTypeIndex = stringIdx
                    )
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // Enum container – Color.RED in a for-loop with classes
    // -------------------------------------------------------------------------

    /**
     * Verifies a combined scenario with both a class container and an enum container
     * at scope level 1. The script filters rooms by category enum value and prints
     * house addresses.
     *
     * (Simplified: just verifies enum entry name comparison logic compiles and runs.)
     */
    @Test
    fun `Both class container and enum container are accessible in the same script`() {
        val metamodelData = MetamodelData(
            path = metamodelPath,
            classes = listOf(
                ClassData(
                    name = "Tag",
                    isAbstract = false,
                    properties = listOf(
                        PropertyData(
                            name = "label",
                            primitiveType = "string",
                            multiplicity = MultiplicityData.single()
                        )
                    )
                )
            ),
            enums = listOf(
                EnumData(name = "Priority", entries = listOf("HIGH", "LOW"))
            )
        )

        // Script: two functions share the same file scope
        val ast = buildMixedAst()
        val input = CompilationInput(mapOf(testFilePath to ast))
        val program = compiler.compile(input, metamodelData)

        val modelData = ModelData(
            metamodelUri = metamodelPath,
            instances = listOf(
                ModelDataInstance(
                    name = "t1",
                    className = "Tag",
                    properties = mapOf("label" to ModelDataPropertyValue.StringValue("urgent"))
                )
            ),
            links = emptyList()
        )

        // Use a single ExecutionEnvironment with captured console so model instances and
        // script classes share the same ScriptClassLoader (avoids classloader identity crisis).
        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos, true, Charsets.UTF_8)
        val env = ExecutionEnvironment(program)
        val model = ModelDataScriptModel(modelData, metamodelData, env.classLoader, program)

        // Verify class container
        ExecutionContext.withContext(ps, model) {
            env.invoke(testFilePath, "printTagLabels")
        }
        val tagLabels = baos.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
        assertEquals(listOf("urgent"), tagLabels)

        // Verify enum container
        val priorityEntry = env.invoke(testFilePath, "getHighEntry")
        assertEquals("HIGH", priorityEntry)
    }

    private fun buildMixedAst(): TypedAst = buildTypedAst {
        val voidIdx      = addType(VoidType())
        val stringIdx    = addType(ClassTypeRef("builtin", "string", false))
        val tagIdx       = addType(ClassTypeRef(classPackage, "Tag", false))
        val collTagIdx   = addType(ClassTypeRef("builtin", "Collection", false))
        val tagContIdx   = addType(ClassTypeRef(classContainerPackage, "Tag", false))
        val enumValIdx   = addType(ClassTypeRef(enumPackage, "Priority", false))
        val enumContIdx  = addType(ClassTypeRef(enumContainerPackage, "Priority", false))

        // fun printTagLabels(): void { for (t in Tag.all()) { println(t.label) } }
        function(
            name = "printTagLabels",
            returnType = voidIdx,
            body = listOf(
                forStmt(
                    variableName = "t",
                    variableType = tagIdx,
                    iterable = memberCall(
                        expression = identifier("Tag", tagContIdx, scope = 1),
                        member = "all",
                        overload = "",
                        arguments = emptyList(),
                        resultTypeIndex = collTagIdx
                    ),
                    body = listOf(
                        exprStmt(
                            functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(
                                    memberAccess(
                                        expression = identifier("t", tagIdx, scope = 4),
                                        member = "label",
                                        resultTypeIndex = stringIdx
                                    )
                                ),
                                resultTypeIndex = voidIdx
                            )
                        )
                    )
                )
            )
        )

        // fun getHighEntry(): String { return Priority.HIGH.getEntry() }
        function(
            name = "getHighEntry",
            returnType = stringIdx,
            body = listOf(
                returnStmt(
                    memberCall(
                        expression = memberAccess(
                            expression = identifier("Priority", enumContIdx, scope = 1),
                            member = "HIGH",
                            resultTypeIndex = enumValIdx
                        ),
                        member = "getEntry",
                        overload = "",
                        arguments = emptyList(),
                        resultTypeIndex = stringIdx
                    )
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // toString() on generated types
    // -------------------------------------------------------------------------

    /**
     * Verifies that calling `.toString()` on an enum value returns the entry's name.
     *
     * The script:
     * ```
     * fun test(): String {
     *     return Status.ACTIVE.toString()
     * }
     * ```
     * must return the string `"ACTIVE"`.
     */
    @Test
    fun `toString on enum value returns entry name`() {
        val metamodelData = MetamodelData(
            path = metamodelPath,
            enums = listOf(EnumData(name = "Status", entries = listOf("ACTIVE", "INACTIVE")))
        )

        val ast = buildTypedAst {
            val stringIdx    = addType(ClassTypeRef("builtin", "string", false))
            val enumValueIdx = addType(ClassTypeRef(enumPackage, "Status", false))
            val containerIdx = addType(ClassTypeRef(enumContainerPackage, "Status", false))

            function(
                name = "test",
                returnType = stringIdx,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = memberAccess(
                                expression = identifier("Status", containerIdx, scope = 1),
                                member = "ACTIVE",
                                resultTypeIndex = enumValueIdx
                            ),
                            member = "toString",
                            overload = "",
                            arguments = emptyList(),
                            resultTypeIndex = stringIdx
                        )
                    )
                )
            )
        }

        val input = CompilationInput(mapOf(testFilePath to ast))
        val program = compiler.compile(input, metamodelData)

        val result = ExecutionEnvironment(program).invoke(testFilePath, "test")

        assertEquals("ACTIVE", result,
            "Expected Status.ACTIVE.toString() to return \"ACTIVE\", but got: $result")
    }

    /**
     * Verifies that `.toString()` on a model class instance returns `"ClassName:instanceName"`.
     *
     * The script:
     * ```
     * fun test(): String {
     *     for (house in House.all()) {
     *         return house.toString()
     *     }
     *     return ""
     * }
     * ```
     * executed against a model with one House instance named `"house1"` must return
     * `"House:house1"`.
     */
    @Test
    fun `toString on class instance returns class name and instance name`() {
        val metamodelData = MetamodelData(
            path = metamodelPath,
            classes = listOf(ClassData(name = "House", isAbstract = false, properties = emptyList()))
        )

        val ast = buildTypedAst {
            val stringIdx          = addType(ClassTypeRef("builtin", "string", false))
            val houseIdx           = addType(ClassTypeRef(classPackage, "House", false))
            val collectionHouseIdx = addType(ClassTypeRef("builtin", "Collection", false))
            val houseContainerIdx  = addType(ClassTypeRef(classContainerPackage, "House", false))

            function(
                name = "test",
                returnType = stringIdx,
                body = listOf(
                    forStmt(
                        variableName = "house",
                        variableType = houseIdx,
                        iterable = memberCall(
                            expression = identifier("House", houseContainerIdx, scope = 1),
                            member = "all",
                            overload = "",
                            arguments = emptyList(),
                            resultTypeIndex = collectionHouseIdx
                        ),
                        body = listOf(
                            returnStmt(
                                memberCall(
                                    expression = identifier("house", houseIdx, scope = 4),
                                    member = "toString",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = stringIdx
                                )
                            )
                        )
                    ),
                    returnStmt(stringLiteral("", stringIdx))
                )
            )
        }

        val input = CompilationInput(mapOf(testFilePath to ast))
        val program = compiler.compile(input, metamodelData)

        val modelData = ModelData(
            metamodelUri = metamodelPath,
            instances = listOf(
                ModelDataInstance(
                    name = "house1",
                    className = "House",
                    properties = emptyMap()
                )
            ),
            links = emptyList()
        )

        val env = ExecutionEnvironment(program)
        val model = ModelDataScriptModel(modelData, metamodelData, env.classLoader, program)

        val result = ExecutionContext.withContext(System.out, model) {
            env.invoke(testFilePath, "test")
        }

        assertEquals("House:house1", result,
            "Expected house.toString() to return \"House:house1\", but got: $result")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Captures console output produced by [block].
     */
    private fun captureOutput(block: () -> Unit): String {
        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos, true, Charsets.UTF_8)
        ExecutionContext.withContext(ps, null) { block() }
        return baos.toString(Charsets.UTF_8)
    }

}
