package com.tc128.giamdinhnative.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Server (.NET) serialize enum dưới dạng số nguyên theo thứ tự khai báo (ordinal), không phải tên chuỗi
enum class EorStatus { Approval, Cancel, Pending, Complete }
enum class Payer { U, O, D }

object EorStatusSerializer : KSerializer<EorStatus> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EorStatus", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: EorStatus) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): EorStatus =
        EorStatus.entries.getOrElse(decoder.decodeInt()) { EorStatus.Pending }
}

object PayerSerializer : KSerializer<Payer> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Payer", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Payer) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): Payer =
        Payer.entries.getOrElse(decoder.decodeInt()) { Payer.U }
}

@Serializable
data class ItemEorRef(
    @SerialName("id") val id: Int = 0,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("descriptionVietnamese") val descriptionVietnamese: String? = null,
)

@Serializable
data class ItemEOR(
    @SerialName("id") val id: Int = 0,
    @SerialName("containerID") val containerId: Int = 0,
    @SerialName("componentID") val componentId: Int? = null,
    @SerialName("component") val component: ItemEorRef? = null,
    @SerialName("damageCodeID") val damageCodeId: Int? = null,
    @SerialName("damageCode") val damageCode: ItemEorRef? = null,
    @SerialName("repairMethodID") val repairMethodId: Int? = null,
    @SerialName("repairMethod") val repairMethod: ItemEorRef? = null,
    @SerialName("itemRepairID") val itemRepairId: Int? = null,
    @SerialName("qty") val qty: Int = 1,
    @SerialName("seq") val seq: Int? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("length") val length: Double? = null,
    @SerialName("wide") val wide: Double? = null,
    @SerialName("sts") val sts: String? = null,
    @Serializable(with = PayerSerializer::class) @SerialName("payer") val payer: Payer? = null,
    @Serializable(with = EorStatusSerializer::class) @SerialName("status") val status: EorStatus = EorStatus.Pending,
    @SerialName("labourHour") val labourHour: Double? = null,
    @SerialName("labourCost") val labourCost: Double? = null,
    @SerialName("materialCost") val materialCost: Double? = null,
    @SerialName("total") val total: Double? = null,
    @SerialName("dateCreate") val dateCreate: String? = null,
) {
    val componentName: String? get() = component?.descriptionVietnamese?.ifBlank { null } ?: component?.codeName
    val damageCodeName: String? get() = damageCode?.codeName
    val repairMethodName: String? get() = repairMethod?.codeName
}
