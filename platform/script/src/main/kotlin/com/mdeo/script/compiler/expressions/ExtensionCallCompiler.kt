package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedExtensionCallExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.registry.function.PluginFunctionParameter
import com.mdeo.script.compiler.registry.function.PluginFunctionSignatureDefinition
import com.mdeo.script.compiler.util.ASMUtil
import com.mdeo.script.compiler.util.CoercionUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Holds the information needed to load a single argument value that was temporarily
 * spilled to a JVM local-variable slot.
 *
 * @param paramName Name of the plugin-function parameter this argument belongs to.
 * @param slotIndex JVM local-variable slot where the value was stored.
 * @param type Resolved [ReturnType] of the stored value (determines load opcode and size).
 */
private data class TempLocal(
    val paramName: String,
    val slotIndex: Int,
    val type: ReturnType
)

/**
 * Compiles [TypedExtensionCallExpression] (kind `"extensionCall"`) to bytecode.
 *
 * Extension calls arise from plugin-contributed DSL expressions whose syntactic
 * arguments are mapped to the parameters of the implementing function. Two invariants
 * must be maintained:
 *
 * **Source-order evaluation** – argument expressions must be evaluated in the order they
 * appear in the source text. The [TypedExtensionCallExpression.arguments] list is already
 * sorted by source position by the frontend converter, so iterating it in order is
 * sufficient. Because the JVM requires arguments on the stack in *parameter-declaration*
 * order, all evaluated values are first spilled to temporary local-variable slots; they
 * are then loaded in the correct parameter order.
 *
 * **Cardinality** – a parameter whose type is a collection (e.g. `List<String>`) may
 * receive zero, one, or many named arguments. All matching arguments are assembled into
 * a fresh [java.util.ArrayList] before the function is invoked. Non-collection parameters
 * expect exactly one argument; if none is present a JVM `null` is pushed instead.
 *
 * The compiled call emits `INVOKEVIRTUAL` (matching [PluginFunctionSignatureDefinition])
 * so a `this` receiver is pushed before the assembled arguments.
 */
class ExtensionCallCompiler : AbstractCallCompiler() {

    /**
     * Accepts only extension-call expressions.
     *
     * @param expression The expression to test.
     * @return `true` when the expression kind is `"extensionCall"`.
     */
    override fun canCompile(expression: TypedExpression): Boolean =
        expression.kind == "extensionCall"

    /**
     * Emits bytecode for one extension-call expression.
     *
     * Steps:
     * 1. Resolve the [PluginFunctionSignatureDefinition] from the function registry.
     * 2. Evaluate every argument expression in source order and spill each result to a
     *    dedicated temporary local-variable slot.
     * 3. Push the `this` receiver.
     * 4. For each declared parameter (in declaration order):
     *    - Collect all temporary locals whose [TempLocal.paramName] matches.
     *    - If the parameter type is a collection type: build an [java.util.ArrayList]
     *      from those locals, boxing primitives as required.
     *    - Otherwise: load the single matching temporary local, applying coercion to
     *      the declared parameter type; push `null` when no matching argument is present.
     * 5. Invoke the method via [PluginFunctionSignatureDefinition.emitInvocation].
     * 6. Apply return-type coercion if needed.
     *
     * @param expression The extension-call expression to compile.
     * @param context The current compilation context.
     * @param mv The ASM [MethodVisitor] for bytecode emission.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val extCall = expression as TypedExtensionCallExpression

        val funcDef = context.functionRegistry.lookupFunction(extCall.name)
            ?: error("Extension function '${extCall.name}' not found in registry")

        val signature = (funcDef.getOverload(extCall.overload) ?: funcDef.getOverloads().firstOrNull())
            as? PluginFunctionSignatureDefinition
            ?: error(
                "'${extCall.name}' (overload '${extCall.overload}') is not a plugin function or was not found"
            )

        val tempLocals = spillArgsToTemps(extCall, context, mv)

        mv.visitVarInsn(Opcodes.ALOAD, 0)

        for (param in signature.namedParameters) {
            val matching = tempLocals.filter { it.paramName == param.name }
            if (isCollectionType(param.type)) {
                emitListFromTemps(matching, getCollectionElementType(param.type), mv)
            } else if (matching.isEmpty()) {
                mv.visitInsn(Opcodes.ACONST_NULL)
            } else {
                val temp = matching.first()
                mv.visitVarInsn(ASMUtil.getLoadOpcode(temp.type), temp.slotIndex)
                CoercionUtil.emitCoercion(temp.type, param.type, mv, context)
            }
        }

        signature.emitInvocation(mv)

        val expectedReturnType = context.getType(extCall.evalType)
        emitReturnTypeCoercion(expectedReturnType, signature.returnType, mv)
    }

    /**
     * Evaluates every argument expression in source order and stores each result in a
     * freshly allocated temporary local-variable slot.
     *
     * The [TypedExtensionCallExpression.arguments] list is already sorted by source
     * position, so iterating it maintains the source-order evaluation guarantee.
     *
     * @param extCall The extension-call expression whose arguments are to be spilled.
     * @param context The compilation context (provides type lookup and slot allocation).
     * @param mv The method visitor for bytecode emission.
     * @return Ordered list of [TempLocal] descriptors in the same order as the input
     *         arguments.
     */
    private fun spillArgsToTemps(
        extCall: TypedExtensionCallExpression,
        context: CompilationContext,
        mv: MethodVisitor
    ): List<TempLocal> {
        val temps = mutableListOf<TempLocal>()
        for (arg in extCall.arguments) {
            val argType = context.getType(arg.value.evalType)
            context.compileExpression(arg.value, mv, argType)
            val slotsNeeded = ASMUtil.getSlotsForType(argType)
            val slot = context.allocateTempSlot(slotsNeeded)
            mv.visitVarInsn(ASMUtil.getStoreOpcode(argType), slot)
            temps += TempLocal(paramName = arg.name, slotIndex = slot, type = argType)
        }
        return temps
    }

    /**
     * Emits bytecode that creates a `java.util.ArrayList`, populates it with the values
     * from [temps] (boxing primitives as required), and leaves the list on the stack.
     *
     * @param temps The temporary locals that correspond to the list elements, in source order.
     * @param elementType The declared element type of the collection parameter (used for boxing).
     * @param mv The method visitor for bytecode emission.
     */
    private fun emitListFromTemps(
        temps: List<TempLocal>,
        @Suppress("UNUSED_PARAMETER") elementType: ReturnType,
        mv: MethodVisitor
    ) {
        mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList")
        mv.visitInsn(Opcodes.DUP)
        val size = temps.size
        when {
            size <= 5 -> mv.visitInsn(Opcodes.ICONST_0 + size)
            size <= Byte.MAX_VALUE -> mv.visitIntInsn(Opcodes.BIPUSH, size)
            else -> mv.visitLdcInsn(size)
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false)

        for (temp in temps) {
            mv.visitInsn(Opcodes.DUP)
            mv.visitVarInsn(ASMUtil.getLoadOpcode(temp.type), temp.slotIndex)
            if (temp.type is ClassTypeRef && !temp.type.isNullable && CoercionUtil.isPrimitiveType(temp.type)) {
                CoercionUtil.emitBoxing(temp.type, mv)
            }
            mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "java/util/List",
                "add",
                "(Ljava/lang/Object;)Z",
                true
            )
            mv.visitInsn(Opcodes.POP)
        }
    }

    /**
     * Returns `true` when [type] represents a built-in collection type whose
     * parameter cardinality requires assembling multiple arguments into a list.
     *
     * @param type The parameter type to test.
     * @return `true` for any built-in collection (List, Set, Bag, etc.).
     */
    private fun isCollectionType(type: ReturnType): Boolean {
        if (type !is ClassTypeRef) return false
        return type.`package` == "builtin" && type.type in COLLECTION_TYPES
    }

    /**
     * Extracts the element type from a collection [ClassTypeRef] (via the `T` type argument).
     * Returns `Any?` when the type argument is absent or the type is not a [ClassTypeRef].
     *
     * @param collectionType The collection type reference.
     * @return The element type or `Any?` as a fallback.
     */
    private fun getCollectionElementType(collectionType: ReturnType): ReturnType {
        if (collectionType is ClassTypeRef) {
            val firstArg = collectionType.typeArgs?.values?.firstOrNull()
            if (firstArg != null) return firstArg
        }
        return ClassTypeRef(`package` = "builtin", type = "Any", isNullable = true)
    }

    private companion object {
        /** All built-in type names that represent a collection and therefore
         *  require cardinality-based argument assembly. */
        val COLLECTION_TYPES: Set<String> = setOf(
            "List", "ReadonlyList",
            "Collection", "ReadonlyCollection",
            "Set", "ReadonlySet",
            "Bag", "ReadonlyBag",
            "OrderedSet", "ReadonlyOrderedSet",
            "OrderedCollection", "ReadonlyOrderedCollection"
        )
    }
}
