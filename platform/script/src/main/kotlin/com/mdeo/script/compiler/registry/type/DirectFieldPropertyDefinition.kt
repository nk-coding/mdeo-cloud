package com.mdeo.script.compiler.registry.type

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

private const val HELPER_CLASS = "com/mdeo/metamodel/MultiplicityAccessHelper"
private const val SET_IMPL_CLASS = "com/mdeo/script/stdlib/impl/collections/SetImpl"
private const val LIST_IMPL_CLASS = "com/mdeo/script/stdlib/impl/collections/ListImpl"

/**
 * Property definition that emits bytecode to access a generated `prop_X` field
 * on a metamodel-generated instance class.
 *
 * For regular scalar properties the field holds the value directly (or null when unset).
 * For multi-valued properties the field holds a `java.util.List`, which is wrapped in a
 * [ListImpl] so that it implements [ReadonlyCollection].
 * For link/association-end fields (which always use `java.util.Set` backing) a single-valued
 * association end additionally calls [MultiplicityAccessHelper.extractFirst] to unwrap the
 * value, while a multi-valued association end is wrapped in a [SetImpl] so that it
 * implements [ReadonlyCollection].
 *
 * @param name The property name in the script language.
 * @param descriptor The script-facing JVM type descriptor (always a reference type).
 * @param ownerClass The JVM internal name of the generated instance class.
 * @param fieldName The field name (e.g. "prop_0", "prop_1").
 * @param fieldDescriptor The actual JVM field descriptor (`Ljava/util/Set;` for links,
 *                        `Ljava/util/List;` for multi-valued properties, or the boxed
 *                        element type for single-valued properties).
 * @param upper The upper multiplicity bound (used only for `Ljava/util/Set;` link fields).
 */
class DirectFieldPropertyDefinition(
    override val name: String,
    override val descriptor: String,
    override val ownerClass: String,
    private val fieldName: String,
    private val fieldDescriptor: String,
    private val upper: Int = -1
) : PropertyDefinition {

    override val isStatic: Boolean = false
    override val isInterface: Boolean = false
    override val getterName: String = fieldName

    override fun emitAccess(mv: MethodVisitor) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, ownerClass)
        mv.visitFieldInsn(Opcodes.GETFIELD, ownerClass, fieldName, fieldDescriptor)
        if (fieldDescriptor == "Ljava/util/Set;") {
            if (upper == 1) {
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    HELPER_CLASS,
                    "extractFirst",
                    "(Ljava/util/Set;)Ljava/lang/Object;",
                    false
                )
                if (descriptor != "Ljava/lang/Object;") {
                    val targetType = descriptor.substring(1, descriptor.length - 1)
                    mv.visitTypeInsn(Opcodes.CHECKCAST, targetType)
                }
            } else {
                mv.visitTypeInsn(Opcodes.NEW, SET_IMPL_CLASS)
                mv.visitInsn(Opcodes.DUP_X1)
                mv.visitInsn(Opcodes.SWAP)
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, SET_IMPL_CLASS, "<init>",
                    "(Ljava/util/Collection;)V", false)
            }
        } else if (fieldDescriptor == "Ljava/util/List;") {
            mv.visitTypeInsn(Opcodes.NEW, LIST_IMPL_CLASS)
            mv.visitInsn(Opcodes.DUP_X1)
            mv.visitInsn(Opcodes.SWAP)
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, LIST_IMPL_CLASS, "<init>",
                "(Ljava/util/Collection;)V", false)
        }
    }

    override fun emitSet(mv: MethodVisitor, valueDescriptor: String) {
        throw UnsupportedOperationException("Property '$name' is read-only")
    }
}
