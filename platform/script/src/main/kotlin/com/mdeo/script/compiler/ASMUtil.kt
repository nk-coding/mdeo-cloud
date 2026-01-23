package com.mdeo.script.compiler

import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.VoidType
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Centralized utility for ASM/JVM bytecode operations.
 * 
 * This object consolidates commonly duplicated functions across the compiler:
 * - Type descriptor generation
 * - Load/Store opcode selection
 * - Stack size calculation
 * - Unboxing and casting operations
 * 
 * All compilers should use these utilities instead of implementing their own versions.
 */
object ASMUtil {
    
    /**
     * Gets the JVM type descriptor for a return type.
     * 
     * Delegates to CompilationContext.getTypeDescriptor for consistency.
     * This is the canonical method for getting type descriptors.
     * 
     * @param returnType The return type to get the descriptor for.
     * @return The JVM type descriptor string (e.g., "I" for int, "Ljava/lang/String;" for String).
     */
    fun getTypeDescriptor(returnType: ReturnType): String {
        return when (returnType) {
            is VoidType -> "V"
            is ClassTypeRef -> getClassTypeDescriptor(returnType)
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Gets the JVM type descriptor for a class type reference.
     * 
     * @param classType The class type reference.
     * @return The JVM type descriptor string.
     */
    private fun getClassTypeDescriptor(classType: ClassTypeRef): String {
        return when (classType.type) {
            "builtin.int" -> if (classType.isNullable) "Ljava/lang/Integer;" else "I"
            "builtin.long" -> if (classType.isNullable) "Ljava/lang/Long;" else "J"
            "builtin.float" -> if (classType.isNullable) "Ljava/lang/Float;" else "F"
            "builtin.double" -> if (classType.isNullable) "Ljava/lang/Double;" else "D"
            "builtin.boolean" -> if (classType.isNullable) "Ljava/lang/Boolean;" else "Z"
            "builtin.string" -> "Ljava/lang/String;"
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Gets the primitive type descriptor for JVM.
     * 
     * @param primitiveTypeName The primitive type name (e.g., "builtin.int").
     * @return The JVM type descriptor (e.g., "I" for int), or null if not a primitive type.
     */
    fun getPrimitiveDescriptor(primitiveTypeName: String): String? {
        return when (primitiveTypeName) {
            "builtin.int" -> "I"
            "builtin.long" -> "J"
            "builtin.float" -> "F"
            "builtin.double" -> "D"
            "builtin.boolean" -> "Z"
            else -> null
        }
    }
    
    /**
     * Gets the appropriate LOAD opcode for a type.
     * 
     * Returns the JVM load instruction opcode based on the type:
     * - ILOAD for int and boolean primitives
     * - LLOAD for long primitives
     * - FLOAD for float primitives
     * - DLOAD for double primitives
     * - ALOAD for all reference types (including nullable primitives)
     * 
     * @param type The type being loaded.
     * @return The JVM LOAD opcode (ILOAD, LLOAD, FLOAD, DLOAD, or ALOAD).
     */
    fun getLoadOpcode(type: ReturnType): Int {
        if (type is ClassTypeRef) {
            if (type.isNullable) {
                return Opcodes.ALOAD
            }
            return when (type.type) {
                "builtin.int", "builtin.boolean" -> Opcodes.ILOAD
                "builtin.long" -> Opcodes.LLOAD
                "builtin.float" -> Opcodes.FLOAD
                "builtin.double" -> Opcodes.DLOAD
                else -> Opcodes.ALOAD
            }
        }
        return Opcodes.ALOAD
    }
    
    /**
     * Gets the appropriate STORE opcode for a type.
     * 
     * Returns the JVM store instruction opcode based on the type:
     * - ISTORE for int and boolean primitives
     * - LSTORE for long primitives
     * - FSTORE for float primitives
     * - DSTORE for double primitives
     * - ASTORE for all reference types (including nullable primitives)
     * 
     * @param type The type to get the store opcode for.
     * @return The appropriate JVM STORE opcode.
     */
    fun getStoreOpcode(type: ReturnType): Int {
        if (type is ClassTypeRef) {
            if (type.isNullable) {
                return Opcodes.ASTORE
            }
            return when (type.type) {
                "builtin.int", "builtin.boolean" -> Opcodes.ISTORE
                "builtin.long" -> Opcodes.LSTORE
                "builtin.float" -> Opcodes.FSTORE
                "builtin.double" -> Opcodes.DSTORE
                else -> Opcodes.ASTORE
            }
        }
        return Opcodes.ASTORE
    }
    
    /**
     * Gets the stack size for a type (1 for most types, 2 for long/double).
     * 
     * @param typeName The type name to check (e.g., "builtin.long").
     * @return The number of stack slots (1 for most types, 2 for long/double).
     */
    fun getStackSize(typeName: String): Int {
        return when (typeName) {
            "builtin.long", "builtin.double" -> 2
            else -> 1
        }
    }
    
    /**
     * Gets the stack size for a ReturnType.
     * Objects (nullable types) are always 1 slot.
     * 
     * @param type The return type to check.
     * @return The number of stack slots (1 for most types, 2 for long/double primitives).
     */
    fun getStackSize(type: ReturnType): Int {
        if (type !is ClassTypeRef) {
            return 1
        }
        if (type.isNullable) {
            return 1
        }
        return getStackSize(type.type)
    }
    
    /**
     * Gets the number of local variable slots required for a type.
     * 
     * @param type The return type to check.
     * @param isWrapped Whether the variable is wrapped in a Ref (which always takes 1 slot).
     * @return The number of JVM local variable slots required (1 or 2).
     */
    fun getSlotsForType(type: ReturnType, isWrapped: Boolean = false): Int {
        if (isWrapped) {
            return 1
        }

        if (type is ClassTypeRef && !type.isNullable) {
            return when (type.type) {
                "builtin.long", "builtin.double" -> 2
                else -> 1
            }
        }
        return 1
    }
    
    /**
     * Emits unboxing or casting bytecode for Object to target type conversion.
     * 
     * This is primarily used by ForStatementCompiler to convert Iterator.next() results
     * (which return Object) to the loop variable type.
     * 
     * For primitive types, casts to the wrapper type and unboxes (unless nullable).
     * For String, casts to String. For other object types, keeps as Object.
     * 
     * @param targetType The target type to convert to.
     * @param mv The method visitor for emitting bytecode.
     */
    fun emitUnboxOrCast(targetType: ReturnType, mv: MethodVisitor) {
        if (targetType !is ClassTypeRef) {
            return
        }
        
        when (targetType.type) {
            "builtin.int" -> {
                if (targetType.isNullable) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
                } else {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false)
                }
            }
            "builtin.long" -> {
                if (targetType.isNullable) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long")
                } else {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long")
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false)
                }
            }
            "builtin.float" -> {
                if (targetType.isNullable) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float")
                } else {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float")
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false)
                }
            }
            "builtin.double" -> {
                if (targetType.isNullable) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double")
                } else {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double")
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false)
                }
            }
            "builtin.boolean" -> {
                if (targetType.isNullable) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean")
                } else {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean")
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false)
                }
            }
            "builtin.string" -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
            }
            else -> {
                // For other object types, no cast needed (already Object)
            }
        }
    }
}
