package com.arigato.app.domain.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RootStatusSerializer::class)
enum class RootStatus {
    NOT_REQUIRED,
    OPTIONAL_ROOT,
    REQUIRES_ROOT
}

object RootStatusSerializer : KSerializer<RootStatus> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RootStatus", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RootStatus) {
        encoder.encodeString(value.name.lowercase())
    }

    override fun deserialize(decoder: Decoder): RootStatus {
        val value = decoder.decodeString()
        return RootStatus.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: RootStatus.NOT_REQUIRED
    }
}
