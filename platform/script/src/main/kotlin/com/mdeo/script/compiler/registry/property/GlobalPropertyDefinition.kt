package com.mdeo.script.compiler.registry.property

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Represents a global property definition in the registry.
 *
 * Global properties are accessible at scope level 0 without a receiver.
 */
interface GlobalPropertyDefinition {
    /**
     * The name of the global property.
     */
    val name: String

    /**
     * The JVM type descriptor of the property value.
     */
    val descriptor: String

    /**
     * The JVM internal name of the owner class.
     */
    val ownerClass: String

    /**
     * The JVM field or getter name.
     */
    val getterName: String

    /**
     * Emits the property access bytecode.
     *
     * @param mv The method visitor to emit bytecode to.
     */
    fun emitAccess(mv: MethodVisitor)
}

/**
 * Implementation of GlobalPropertyDefinition for static field access.
 */
class StaticGlobalPropertyDefinition(
    override val name: String,
    override val descriptor: String,
    override val ownerClass: String,
    override val getterName: String
) : GlobalPropertyDefinition {

    override fun emitAccess(mv: MethodVisitor) {
        mv.visitFieldInsn(
            Opcodes.GETSTATIC,
            ownerClass,
            getterName,
            descriptor
        )
    }
}
