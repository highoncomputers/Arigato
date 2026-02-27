package com.arigato.app.domain.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ExecutionModeSerializer::class)
enum class ExecutionMode {
    GUI,
    TERMINAL,
    LAUNCH
}

object ExecutionModeSerializer : KSerializer<ExecutionMode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ExecutionMode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ExecutionMode) {
        encoder.encodeString(value.name.lowercase())
    }

    override fun deserialize(decoder: Decoder): ExecutionMode {
        val value = decoder.decodeString()
        return ExecutionMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: ExecutionMode.GUI
    }
}
