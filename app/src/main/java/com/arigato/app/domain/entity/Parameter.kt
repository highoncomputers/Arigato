package com.arigato.app.domain.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class ParameterValidation(
    val pattern: String? = null,
    val hint: String? = null,
    val message: String? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null
)

@Serializable
data class ParameterOption(
    val value: String,
    val label: String = value
)

object ParameterOptionsSerializer : KSerializer<List<ParameterOption>> {
    override val descriptor: SerialDescriptor = ListSerializer(ParameterOption.serializer()).descriptor

    override fun serialize(encoder: Encoder, value: List<ParameterOption>) {
        val jsonEncoder = encoder as? JsonEncoder ?: return
        val jsonArray = JsonArray(
            value.map {
                buildJsonObject {
                    put("value", it.value)
                    put("label", it.label)
                }
            }
        )
        jsonEncoder.encodeJsonElement(jsonArray)
    }

    override fun deserialize(decoder: Decoder): List<ParameterOption> {
        val jsonDecoder = decoder as? JsonDecoder ?: return emptyList()
        val element = jsonDecoder.decodeJsonElement()
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> ParameterOption(item.content)
                is JsonObject -> {
                    val value = item["value"]?.jsonPrimitive?.content
                        ?: item["label"]?.jsonPrimitive?.content
                        ?: return@mapNotNull null
                    val label = item["label"]?.jsonPrimitive?.content ?: value
                    ParameterOption(value = value, label = label)
                }
                else -> null
            }
        }
    }
}

@Serializable
data class Parameter(
    val name: String,
    val label: String? = null,
    val hint: String? = null,
    val flag: String? = null,
    val description: String? = null,
    val type: String = "TEXT",
    val isRequired: Boolean = false,
    val defaultValue: String? = null,
    @Serializable(with = ParameterOptionsSerializer::class)
    val options: List<ParameterOption>? = null,
    val validation: ParameterValidation? = null,
    val isPositional: Boolean = false
) {
    val parameterType: ParameterType
        get() = ParameterType.fromString(type)
}
