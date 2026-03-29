package com.mdeo.metamodel

import com.mdeo.metamodel.data.ModelData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Polymorphic container for a serialized model graph, supporting multiple
 * backing formats optimized for different [com.mdeo.modeltransformation.graph.ModelGraph]
 * implementations.
 *
 * [AsModelData] wraps the existing JSON-friendly [ModelData] structure.
 * [AsBinary] holds a compact byte array produced by [ModelBinarySerializer],
 * which is significantly faster for model graphs backed by generated
 * [ModelInstance] objects (e.g. MdeoModelGraph).
 */
@Serializable
sealed class SerializedModel {

    /**
     * Extracts the contained model data, decoding from binary if necessary.
     *
     * @param metamodel The compiled metamodel, required when decoding binary format.
     * @return The [ModelData] representation of the serialized model.
     */
    abstract fun toModelData(metamodel: Metamodel): ModelData

    /**
     * Backing format that wraps a pre-built [ModelData] instance.
     *
     * This is the natural format for graph implementations that already have
     * efficient [ModelData] conversion (e.g. TinkerModelGraph).
     *
     * @param modelData The model data payload.
     */
    @Serializable
    @SerialName("model_data")
    data class AsModelData(val modelData: ModelData) : SerializedModel() {

        override fun toModelData(metamodel: Metamodel): ModelData = modelData
    }

    /**
     * Backing format that holds a compact binary representation produced by
     * [ModelBinarySerializer].
     *
     * This format avoids the overhead of constructing intermediate [ModelData]
     * objects and is optimized for model graphs that use generated [ModelInstance]
     * subclasses directly (e.g. MdeoModelGraph).
     *
     * @param data The raw byte array containing the serialized model.
     */
    @Serializable
    @SerialName("binary")
    data class AsBinary(val data: ByteArray) : SerializedModel() {

        override fun toModelData(metamodel: Metamodel): ModelData {
            val (model, _) = ModelBinarySerializer(metamodel).deserialize(data)
            return model.toModelData()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AsBinary) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
    }
}
