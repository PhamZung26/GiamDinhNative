package com.tc128.giamdinhnative.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

enum class PhotoStatus { Available, PreRepair, PostRepair }

// Server (.NET) serialize enum dưới dạng số nguyên ordinal, không phải tên chuỗi — giống EorStatus/Payer
object PhotoStatusSerializer : KSerializer<PhotoStatus> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PhotoStatus", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: PhotoStatus) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): PhotoStatus =
        PhotoStatus.entries.getOrElse(decoder.decodeInt()) { PhotoStatus.Available }
}

@Serializable
data class Photo(
    @SerialName("id") val id: Int = 0,
    @SerialName("idLocal") val idLocal: Long = 0,
    @SerialName("containerID") val containerId: Int = 0,
    @SerialName("containerNumber") val containerNumber: String? = null,
    @SerialName("itemEorId") val itemEorId: Int? = null,
    @SerialName("pathLocal") val pathLocal: String? = null,
    @SerialName("path") val path: String? = null,
    @SerialName("pathOfBackup") val pathOfBackup: String? = null,
    @Serializable(with = PhotoStatusSerializer::class) @SerialName("status") val status: PhotoStatus = PhotoStatus.Available,
    @SerialName("isUploaded") val isUploaded: Boolean = false,
    @SerialName("isBackedUp") val isBackedUp: Boolean = false,
    @SerialName("isDeletedLocal") val isDeletedLocal: Boolean = false,
)
