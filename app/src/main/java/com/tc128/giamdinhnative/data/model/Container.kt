package com.tc128.giamdinhnative.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Khớp với enum StatusOfContainer của backend GiamDinh (Xamarin): thứ tự khai báo quyết định giá trị ordinal
enum class StatusOfContainer(val displayName: String) {
    Survey("Đang giám định"),
    Estimate("Chờ báo giá"),
    Approval("Đã duyệt"),
    Reject("Từ chối"),
    Complete("Hoàn thành"),
    Working("Đang sửa chữa"),
    Draft("Bản nháp")
}

object StatusOfContainerSerializer : KSerializer<StatusOfContainer> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StatusOfContainer", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: StatusOfContainer) = encoder.encodeInt(value.ordinal)
    override fun deserialize(decoder: Decoder): StatusOfContainer =
        StatusOfContainer.entries.getOrElse(decoder.decodeInt()) { StatusOfContainer.Survey }
}

@Serializable
data class Container(
    @SerialName("id") val id: Int = 0,
    @SerialName("containerNo") val containerNumber: String = "",
    @SerialName("optID") val optId: Int? = null,
    @SerialName("sizeID") val sizeId: Int? = null,
    @SerialName("depotID") val depotId: Int? = null,
    @SerialName("gradeID") val gradeId: Int? = null,
    @SerialName("cleanMethodId") val cleanMethodId: Int? = null,
    @SerialName("billNoGateIn") val billNoGateIn: String? = null,
    @SerialName("yearManuacture") val yearManufacture: Int? = null,
    @SerialName("pointGrade") val pointGrade: Int? = null,
    @SerialName("isDamage") val isDamage: Boolean? = null,
    @SerialName("isDirty") val isDirty: Boolean? = null,
    @SerialName("exitsImage") val existsImage: Boolean? = null,
    @SerialName("collectOpt") val collectOpt: Double? = null,
    @SerialName("collectCustomer") val collectCustomer: Double? = null,
    @SerialName("remark") val remark: String? = null,
    @SerialName("tinhTrang") val tinhTrang: String? = null,
    @SerialName("seal") val seal: String? = null,
    @SerialName("dateTimeGatein") val dateTimeGatein: String? = null,
    @SerialName("dateTimeClean") val dateTimeClean: String? = null,
    @SerialName("dateTimeEOR") val dateTimeEor: String? = null,
    @SerialName("dateTimeApproval") val dateTimeApproval: String? = null,
    @SerialName("dateTimeRepair") val dateTimeRepair: String? = null,
    @SerialName("opt") val opt: Opt? = null,
    @SerialName("size") val size: Size? = null,
    @SerialName("depot") val depot: Depot? = null,
    @SerialName("grade") val grade: Grade? = null,
    @SerialName("cleanMethod") val cleanMethod: CleanMethod? = null,
    @Serializable(with = StatusOfContainerSerializer::class)
    @SerialName("statusOfContainer") val statusOfContainer: StatusOfContainer? = null,
    @SerialName("nguoiTao") val nguoiTao: AppUser? = null,
    @SerialName("nguoiGiamDinh") val nguoiGiamDinh: AppUser? = null,
) {
    val sizeName: String? get() = size?.codeName
    val gradeName: String? get() = grade?.codeName
    val depotName: String? get() = depot?.codeName
    val optName: String? get() = opt?.codeName
    val cleanMethodName: String? get() = cleanMethod?.codeName
}

@Serializable
data class AppUser(
    @SerialName("id") val id: String? = null,
    @SerialName("hoVaTen") val hoVaTen: String? = null,
)
