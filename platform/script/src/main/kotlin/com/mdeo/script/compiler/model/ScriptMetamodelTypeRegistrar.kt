package com.mdeo.script.compiler.model

import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.EnumData
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.PropertyData
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.script.compiler.registry.property.GlobalPropertyRegistry
import com.mdeo.script.compiler.registry.property.StaticGlobalPropertyDefinition
import com.mdeo.script.compiler.registry.type.InstancePropertyDefinition
import com.mdeo.script.compiler.registry.type.MethodDefinition
import com.mdeo.script.compiler.registry.type.PropertyDefinition
import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.TypeDefinitionImpl
import com.mdeo.script.compiler.registry.type.TypeRegistry
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Registers metamodel types into a TypeRegistry for script compilation.
 *
 * This registrar creates type definitions for:
 * - Enum containers (e.g., "enum-container/path.Color") with properties for each entry
 * - Enum value types (e.g., "enum/path.Color") as type markers
 * - Class instance types (e.g., "class/path.Foo") with property getters
 * - A global "Model" type with typed allInstances() overloads
 *
 * This mirrors the TransformationEngine.createTypeRegistry pattern.
 */
object ScriptMetamodelTypeRegistrar {

    /** Package prefix for enum container types. */
    const val ENUM_CONTAINER_PACKAGE = "enum-container"

    /** Package prefix for enum value types. */
    const val ENUM_PACKAGE = "enum"

    /** Package prefix for class types. */
    const val CLASS_PACKAGE = "class"

    /** Package prefix for class container types. */
    const val CLASS_CONTAINER_PACKAGE = "class-container"

    /** Package for the model global. */
    const val MODEL_PACKAGE = "model"

    /** Type name for the model global. */
    const val MODEL_TYPE_NAME = "Model"

    /** Internal name of ExecutionContext class (formerly ModelContext). */
    private const val MODEL_CONTEXT_CLASS = "com/mdeo/script/runtime/ExecutionContext"

    /** Internal name of ScriptModel class. */
    private const val SCRIPT_MODEL_CLASS = "com/mdeo/script/runtime/model/ScriptModel"

    /**
     * Combined result of metamodel compilation: a TypeRegistry enriched with metamodel types
     * and a file-scope property registry mapping class/enum container names to their singletons.
     */
    data class MetamodelCompilationResult(
        val typeRegistry: TypeRegistry,
        val fileScopeRegistry: GlobalPropertyRegistry
    )

    /**
     * Creates a type registry and file-scope property registry enriched with metamodel types.
     *
     * @param metamodelData The metamodel containing class and enum definitions.
     * @param metamodelPath The absolute path of the metamodel file.
     * @return A MetamodelCompilationResult with TypeRegistry and file-scope GlobalPropertyRegistry.
     */
    fun createRegistry(
        metamodelData: MetamodelData,
        metamodelPath: String
    ): MetamodelCompilationResult {
        val registry = TypeRegistry(parent = TypeRegistry.GLOBAL)

        for (enumData in metamodelData.enums) {
            registerEnumContainer(registry, enumData, metamodelPath)
            registerEnumValueType(registry, enumData, metamodelPath)
        }

        val classMap = metamodelData.classes.associateBy { it.name }
        for (classData in metamodelData.classes) {
            registerClassType(registry, classData, metamodelPath, classMap, metamodelData)
            registerClassContainerType(registry, classData, metamodelPath)
        }

        registerModelType(registry, metamodelData, metamodelPath)

        val fileScopeRegistry = buildFileScopeRegistry(metamodelData, metamodelPath)

        return MetamodelCompilationResult(registry, fileScopeRegistry)
    }

    /**
     * Builds a GlobalPropertyRegistry for file-scope identifiers (scope level 1).
     *
     * Registers:
     * - Each class name → GETSTATIC <ClassContainer>.INSTANCE
     * - Each enum name  → GETSTATIC <EnumContainer>.INSTANCE
     */
    private fun buildFileScopeRegistry(
        metamodelData: MetamodelData,
        metamodelPath: String
    ): GlobalPropertyRegistry {
        val registry = GlobalPropertyRegistry()

        for (classData in metamodelData.classes) {
            val containerClass = ScriptClassBytecodeGenerator.getClassContainerClassName(classData.name)
            registry.registerProperty(
                StaticGlobalPropertyDefinition(
                    name = classData.name,
                    descriptor = "L$containerClass;",
                    ownerClass = containerClass,
                    getterName = "INSTANCE"
                )
            )
        }

        for (enumData in metamodelData.enums) {
            val containerClass = ScriptEnumBytecodeGenerator.getEnumContainerClassName(enumData.name)
            registry.registerProperty(
                StaticGlobalPropertyDefinition(
                    name = enumData.name,
                    descriptor = "L$containerClass;",
                    ownerClass = containerClass,
                    getterName = "INSTANCE"
                )
            )
        }

        return registry
    }

    /**
     * Registers a class container type with an `all()` method.
     *
     * The class container is accessed like: `House.all()` returning `Collection<House>`.
     */
    private fun registerClassContainerType(
        registry: TypeRegistry,
        classData: ClassData,
        metamodelPath: String
    ) {
        val typePackage = "$CLASS_CONTAINER_PACKAGE$metamodelPath"
        val containerClassName = ScriptClassBytecodeGenerator.getClassContainerClassName(classData.name)
        val instanceClassName = ScriptClassBytecodeGenerator.getInstanceClassName(classData.name)

        val typeDef = TypeDefinitionImpl(
            typePackage = typePackage,
            typeName = classData.name,
            jvmClassName = containerClassName
        )

        typeDef.addMethod(ClassContainerAllMethodDefinition(
            className = classData.name,
            containerClassName = containerClassName,
            instanceClassName = instanceClassName,
            metamodelPath = metamodelPath
        ))

        registry.register(typeDef)
    }

    /**
     * Registers an enum container type with properties for each entry.
     *
     * The enum container is accessed like: `Color.RED`
     * Each entry property returns the corresponding EnumValue singleton.
     */
    private fun registerEnumContainer(
        registry: TypeRegistry,
        enumData: EnumData,
        metamodelPath: String
    ) {
        val typePackage = "$ENUM_CONTAINER_PACKAGE$metamodelPath"
        val typeDef = TypeDefinitionImpl(
            typePackage = typePackage,
            typeName = enumData.name,
            jvmClassName = ScriptEnumBytecodeGenerator.getEnumContainerClassName(enumData.name)
        )

        val containerClass = ScriptEnumBytecodeGenerator.getEnumContainerClassName(enumData.name)
        val valueClass = ScriptEnumBytecodeGenerator.getEnumValueClassName(enumData.name)

        for (entry in enumData.entries) {
            typeDef.addProperty(EnumEntryPropertyDefinition(
                name = entry,
                containerClass = containerClass,
                valueClass = valueClass
            ))
        }

        registry.register(typeDef)
    }

    /**
     * Registers an enum value type (just a type marker).
     *
     * This type is used for property types that reference enums.
     */
    private fun registerEnumValueType(
        registry: TypeRegistry,
        enumData: EnumData,
        metamodelPath: String
    ) {
        val typePackage = "$ENUM_PACKAGE$metamodelPath"
        val valueClassName = ScriptEnumBytecodeGenerator.getEnumValueClassName(enumData.name)
        val typeDef = TypeDefinitionImpl(
            typePackage = typePackage,
            typeName = enumData.name,
            extends = listOf(ClassTypeRef(`package` = "builtin", type = "Any", isNullable = false)),
            jvmClassName = valueClassName
        )
        typeDef.addMethod(EnumValueGetEntryMethodDefinition(valueClassName))
        registry.register(typeDef)
    }

    /**
     * Registers a class type with property getters.
     */
    private fun registerClassType(
        registry: TypeRegistry,
        classData: ClassData,
        metamodelPath: String,
        classMap: Map<String, ClassData>,
        metamodelData: MetamodelData
    ) {
        val typePackage = "$CLASS_PACKAGE$metamodelPath"
        val instanceClassName = ScriptClassBytecodeGenerator.getInstanceClassName(classData.name)

        val extendsRefs = classData.extends.map { superName ->
            ClassTypeRef(`package` = typePackage, type = superName, isNullable = false)
        }.let { refs ->
            if (refs.isEmpty()) listOf(ClassTypeRef(`package` = "builtin", type = "Any", isNullable = false))
            else refs
        }

        val typeDef = TypeDefinitionImpl(
            typePackage = typePackage,
            typeName = classData.name,
            extends = extendsRefs,
            jvmClassName = instanceClassName
        )

        for (property in classData.properties) {
            val propDef = createPropertyDefinition(property, instanceClassName, metamodelPath, metamodelData)
            typeDef.addProperty(propDef)
        }

        val associations = findAssociationsForClass(classData.name, metamodelData)
        for ((propertyName, targetClassName, isMultiple) in associations) {
            val assocPropDef = createAssociationPropertyDefinition(
                propertyName, targetClassName, instanceClassName, metamodelPath, isMultiple
            )
            typeDef.addProperty(assocPropDef)
        }

        registry.register(typeDef)
    }

    /**
     * Creates a property definition for a metamodel property.
     */
    private fun createPropertyDefinition(
        property: PropertyData,
        instanceClassName: String,
        metamodelPath: String,
        metamodelData: MetamodelData
    ): PropertyDefinition {
        val isMultiple = property.multiplicity.isMultiple()
        val descriptor = getPropertyDescriptor(property, metamodelPath, isMultiple)
        val methodName = "get${property.name.replaceFirstChar { it.uppercase() }}"

        return InstancePropertyDefinition(
            name = property.name,
            descriptor = descriptor,
            ownerClass = instanceClassName,
            isInterface = false,
            getterName = methodName
        )
    }

    /**
     * Creates a property definition for an association.
     */
    private fun createAssociationPropertyDefinition(
        propertyName: String,
        targetClassName: String,
        instanceClassName: String,
        metamodelPath: String,
        isMultiple: Boolean
    ): PropertyDefinition {
        val targetInstanceClass = ScriptClassBytecodeGenerator.getInstanceClassName(targetClassName)
        val descriptor = if (isMultiple) {
            "Ljava/util/List;"
        } else {
            "L$targetInstanceClass;"
        }
        val methodName = "get${propertyName.replaceFirstChar { it.uppercase() }}"

        return InstancePropertyDefinition(
            name = propertyName,
            descriptor = descriptor,
            ownerClass = instanceClassName,
            isInterface = false,
            getterName = methodName
        )
    }

    /**
     * Gets the JVM descriptor for a property.
     */
    private fun getPropertyDescriptor(
        property: PropertyData,
        metamodelPath: String,
        isMultiple: Boolean
    ): String {
        if (isMultiple) {
            return "Ljava/util/List;"
        }

        return when {
            property.primitiveType != null -> when (property.primitiveType?.lowercase()) {
                "string" -> "Ljava/lang/String;"
                "int", "integer" -> "Ljava/lang/Integer;"
                "long" -> "Ljava/lang/Long;"
                "float" -> "Ljava/lang/Float;"
                "double" -> "Ljava/lang/Double;"
                "boolean" -> "Ljava/lang/Boolean;"
                else -> "Ljava/lang/Object;"
            }
            property.enumType != null -> {
                val enumValueClass = ScriptEnumBytecodeGenerator.getEnumValueClassName(property.enumType!!)
                "L$enumValueClass;"
            }
            else -> "Ljava/lang/Object;"
        }
    }

    /**
     * Finds associations where the given class is one of the ends.
     */
    private fun findAssociationsForClass(
        className: String,
        metamodelData: MetamodelData
    ): List<Triple<String, String, Boolean>> {
        val result = mutableListOf<Triple<String, String, Boolean>>()

        for (assoc in metamodelData.associations) {
            if (assoc.source.className == className && assoc.source.name != null) {
                result.add(
                    Triple(
                        assoc.source.name!!,
                        assoc.target.className,
                        assoc.source.multiplicity.isMultiple()
                    )
                )
            }
            if (assoc.target.className == className && assoc.target.name != null) {
                result.add(
                    Triple(
                        assoc.target.name!!,
                        assoc.source.className,
                        assoc.target.multiplicity.isMultiple()
                    )
                )
            }
        }

        return result
    }

    /**
     * Registers the Model type with allInstances method overloads.
     */
    private fun registerModelType(
        registry: TypeRegistry,
        metamodelData: MetamodelData,
        metamodelPath: String
    ) {
        val typeDef = TypeDefinitionImpl(
            typePackage = MODEL_PACKAGE,
            typeName = MODEL_TYPE_NAME,
            jvmClassName = SCRIPT_MODEL_CLASS
        )

        for (classData in metamodelData.classes) {
            val instanceClassName = ScriptClassBytecodeGenerator.getInstanceClassName(classData.name)
            typeDef.addMethod(AllInstancesMethodDefinition(
                overloadKey = classData.name,
                targetClassName = classData.name,
                instanceClassName = instanceClassName
            ))
        }

        registry.register(typeDef)
    }
}

/**
 * Property definition for enum entry access (static field).
 *
 * Emits GETSTATIC to load the enum singleton.
 */
private class EnumEntryPropertyDefinition(
    override val name: String,
    private val containerClass: String,
    private val valueClass: String
) : PropertyDefinition {

    override val descriptor: String = "L$valueClass;"
    override val isStatic: Boolean = false
    override val ownerClass: String = containerClass
    override val isInterface: Boolean = false
    override val getterName: String = name

    override fun emitAccess(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.POP)
        mv.visitFieldInsn(
            Opcodes.GETSTATIC,
            containerClass,
            name,
            "L$valueClass;"
        )
    }

    override fun emitSet(mv: MethodVisitor, valueDescriptor: String) {
        throw UnsupportedOperationException("Enum entry '$name' is read-only")
    }
}

/**
 * Method definition for model.allInstances(className).
 *
 * Emits a call to ExecutionContext.requireModel().getAllInstances(className).
 */
private class AllInstancesMethodDefinition(
    override val overloadKey: String,
    private val targetClassName: String,
    private val instanceClassName: String
) : MethodDefinition {

    override val name: String = "allInstances"
    override val descriptor: String = "()Ljava/util/List;"
    override val isStatic: Boolean = false
    override val ownerClass: String = "com/mdeo/script/runtime/model/ScriptModel"
    override val isInterface: Boolean = false
    override val jvmMethodName: String = "getAllInstances"
    override val isVarArgs: Boolean = false
    override val parameterTypes: List<ValueType> = emptyList()
    override val returnType: ReturnType = 
        ClassTypeRef(`package` = "builtin", type = "List", isNullable = false)

    override fun emitInvocation(mv: MethodVisitor) {
        mv.visitLdcInsn(targetClassName)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            ownerClass,
            jvmMethodName,
            "(Ljava/lang/String;)Ljava/util/List;",
            false
        )
    }
}
/**
 * Method definition for class container `all()` calls (e.g. `House.all()`).
 *
 * Pops the class container receiver, then calls
 * `ExecutionContext.requireModel().getAllInstances(className)`.
 */
private class ClassContainerAllMethodDefinition(
    private val className: String,
    private val containerClassName: String,
    private val instanceClassName: String,
    private val metamodelPath: String
) : MethodDefinition {

    override val overloadKey: String = ""
    override val name: String = "all"
    override val descriptor: String = "()Ljava/util/List;"
    override val isStatic: Boolean = false
    override val ownerClass: String = containerClassName
    override val isInterface: Boolean = false
    override val jvmMethodName: String = "all"
    override val isVarArgs: Boolean = false
    override val parameterTypes: List<ValueType> = emptyList()
    override val returnType: ReturnType =
        ClassTypeRef(`package` = "builtin", type = "Collection", isNullable = false)

    override fun emitInvocation(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.POP)
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "com/mdeo/script/runtime/ExecutionContext",
            "requireModel",
            "()Lcom/mdeo/script/runtime/model/ScriptModel;",
            false
        )
        mv.visitLdcInsn(className)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "com/mdeo/script/runtime/model/ScriptModel",
            "getAllInstances",
            "(Ljava/lang/String;)Ljava/util/List;",
            false
        )
        // Wrap the returned List in a BagImpl so the result implements
        // ReadonlyCollection (required for .size(), .isEmpty(), etc. calls
        // that the script compiler emits as INVOKEINTERFACE on ReadonlyCollection).
        mv.visitTypeInsn(Opcodes.NEW, "com/mdeo/script/stdlib/impl/collections/BagImpl")
        mv.visitInsn(Opcodes.DUP_X1)
        mv.visitInsn(Opcodes.SWAP)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "com/mdeo/script/stdlib/impl/collections/BagImpl",
            "<init>",
            "(Ljava/util/Collection;)V",
            false
        )
    }
}

/**
 * Method definition for `getEntry()` on enum value types.
 *
 * Emits INVOKEVIRTUAL on the enum value class to call its `getEntry()` method,
 * which returns the String name of the enum entry (e.g. "ACTIVE").
 */
private class EnumValueGetEntryMethodDefinition(
    private val valueClassName: String
) : MethodDefinition {

    override val overloadKey: String = ""
    override val name: String = "getEntry"
    override val descriptor: String = "()Ljava/lang/String;"
    override val isStatic: Boolean = false
    override val ownerClass: String = valueClassName
    override val isInterface: Boolean = false
    override val jvmMethodName: String = "getEntry"
    override val isVarArgs: Boolean = false
    override val parameterTypes: List<ValueType> = emptyList()
    override val returnType: ReturnType =
        ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)

    override fun emitInvocation(mv: MethodVisitor) {
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            valueClassName,
            "getEntry",
            "()Ljava/lang/String;",
            false
        )
    }
}