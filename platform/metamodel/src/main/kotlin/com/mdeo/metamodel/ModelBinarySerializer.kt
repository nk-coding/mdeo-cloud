package com.mdeo.metamodel

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.IdentityHashMap

/**
 * High-performance binary serializer and deserializer for [Model] instances.
 *
 * Uses generated [ModelInstance.writeProperties] / [ModelInstance.readProperties] methods
 * for scalar fields and handles link (association) fields via generated
 * [ModelInstance.writeLinkFields] / [ModelInstance.readLinkFields] methods.
 *
 * The binary format is designed for speed rather than portability: it relies on both
 * sides sharing the same compiled [Metamodel] (identical field layouts and class ordering).
 *
 * **Binary layout:**
 * 1. Vertex count (int)
 * 2. For each vertex: class type ID (short), then scalar property bytes
 * 3. For each vertex: link fields (per Set: int count + int indices)
 * 4. For each vertex: instance name (UTF string)
 *
 * @param metamodel The compiled metamodel used for class creation and enum resolution.
 */
class ModelBinarySerializer(private val metamodel: Metamodel) {

    /**
     * Sorted list of all class names in the metamodel, used as the mapping from
     * class type ID (position) to class name.
     */
    private val orderedClassNames: List<String> = metamodel.metadata.classes.keys.sorted()

    /**
     * Reverse mapping from class name to its type ID (index in [orderedClassNames]).
     */
    private val classNameToTypeId: Map<String, Int> =
        orderedClassNames.withIndex().associate { (index, name) -> name to index }

    /**
     * Serializes a [Model] and its [instanceNames] into a compact byte array.
     *
     * The model's instances are written in iteration order. Each instance is assigned
     * a sequential index (0, 1, …) used for link field serialization.
     *
     * @param model The model to serialize.
     * @param instanceNames Provides the logical name for each instance, keyed by
     *        instance identity. These are typically sourced from an
     *        [com.mdeo.modeltransformation.runtime.InstanceNameRegistry].
     * @return The serialized byte array.
     */
    fun serialize(model: Model, instanceNames: IdentityHashMap<ModelInstance, String>): ByteArray {
        val baos = ByteArrayOutputStream(4096)
        val out = DataOutputStream(baos)

        val instances = model.instancesByName.values.toList()
        val instanceIndex = IdentityHashMap<ModelInstance, Int>(instances.size)
        for ((i, instance) in instances.withIndex()) {
            instanceIndex[instance] = i
        }

        out.writeInt(instances.size)

        for (instance in instances) {
            val className = metamodel.classNameOf(instance)
            val typeId = classNameToTypeId[className]
                ?: error("Unknown class name '$className' not in metamodel")
            out.writeShort(typeId)
            instance.writeProperties(out)
        }

        for (instance in instances) {
            instance.writeLinkFields(out, instanceIndex)
        }

        for (instance in instances) {
            val name = instanceNames[instance] ?: instance.toString()
            out.writeUTF(name)
        }

        out.flush()
        return baos.toByteArray()
    }

    /**
     * Deserializes a byte array produced by [serialize] back into a [Model] and
     * instance-name map.
     *
     * Deserialization is two-phase, mirroring the deep-copy pattern:
     * 1. **Phase 1** – create all instances and read scalar properties.
     * 2. **Phase 2** – read link fields, populating Sets with references
     *    resolved through the instance array built in phase 1.
     *
     * @param data The byte array to deserialize.
     * @return A pair of the deserialized [Model] and a map from [ModelInstance] to
     *         its logical instance name.
     */
    fun deserialize(data: ByteArray): Pair<Model, IdentityHashMap<ModelInstance, String>> {
        val input = DataInputStream(ByteArrayInputStream(data))

        val vertexCount = input.readInt()
        val instances = arrayOfNulls<ModelInstance>(vertexCount)
        val classNames = arrayOfNulls<String>(vertexCount)

        for (i in 0 until vertexCount) {
            val typeId = input.readShort().toInt()
            val className = orderedClassNames[typeId]
            classNames[i] = className
            val instance = metamodel.createInstance(className)
            instance.readProperties(input, metamodel)
            instances[i] = instance
        }

        @Suppress("UNCHECKED_CAST")
        val typedInstances = instances as Array<ModelInstance>
        for (i in 0 until vertexCount) {
            typedInstances[i].readLinkFields(input, typedInstances)
        }

        val instancesByName = LinkedHashMap<String, ModelInstance>(vertexCount * 2)
        val nameByInstance = IdentityHashMap<ModelInstance, String>(vertexCount * 2)
        for (i in 0 until vertexCount) {
            val name = input.readUTF()
            instancesByName[name] = typedInstances[i]
            nameByInstance[typedInstances[i]] = name
        }

        val model = Model(metamodel, metamodel.path, instancesByName)
        return Pair(model, nameByInstance)
    }
}
