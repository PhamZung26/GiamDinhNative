package com.tc128.giamdinhnative.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Grade(
    @SerialName("id") val id: Int,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("name") val name: String? = null,
)

@Serializable
data class Size(
    @SerialName("id") val id: Int,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("name") val name: String? = null,
)

@Serializable
data class Opt(
    @SerialName("id") val id: Int,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("name") val name: String? = null,
)

@Serializable
data class CleanMethod(
    @SerialName("id") val id: Int,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("name") val name: String? = null,
)

@Serializable
data class Depot(
    @SerialName("id") val id: Int,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("name") val name: String? = null,
)

@Serializable
data class Component(
    @SerialName("id") val id: Int,
    @SerialName("codeName") val codeName: String,
    @SerialName("nameVn") val nameVn: String? = null,
    @SerialName("nameEn") val nameEn: String? = null,
)

@Serializable
data class DamageCode(
    @SerialName("id") val id: Int,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("name") val name: String? = null,
)

@Serializable
data class RepairMethod(
    @SerialName("id") val id: Int,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("name") val name: String? = null,
)

// Mẫu sửa chữa có sẵn (catalog) — chọn 1 mẫu tự điền Component/RepairMethod/Length/Wide/STS
@Serializable
data class ItemRepair(
    @SerialName("id") val id: Int = 0,
    @SerialName("componentID") val componentId: Int = 0,
    @SerialName("repairMethodID") val repairMethodId: Int = 0,
    @SerialName("location") val location: String? = null,
    @SerialName("length") val length: Int? = 0,
    @SerialName("wide") val wide: Int? = 0,
    @SerialName("sts") val sts: String? = null,
    @SerialName("qty") val qty: Int = 1,
)

// Mẫu điền nhanh (quick-fill) cho NhapCont — danh sách giá trị gợi ý sẵn theo CodeName
@Serializable
data class FastFill(
    @SerialName("id") val id: Int = 0,
    @SerialName("codeName") val codeName: String = "",
    @SerialName("name") val name: String? = null,
)

// Hạng mục chấm điểm (ChamDiem) — khớp đúng model Xamarin GiamDinh.Models.ChamDiem.
// ChiTiet: các mục cùng ChiTiet loại trừ nhau (chọn 1 thì ẩn các mục còn lại cùng ChiTiet).
// DiemSo == 0: mục "không có lỗi" — chọn nó sẽ ẩn toàn bộ mục có điểm khác 0 trong cùng Nhom.
@Serializable
data class ChamDiem(
    @SerialName("id") val id: Int = 0,
    @SerialName("nhom") val nhom: String? = null,
    @SerialName("chiTiet") val chiTiet: String? = null,
    @SerialName("dienGiai") val dienGiai: String = "",
    @SerialName("diemSo") val diemSo: Int = 0,
    @SerialName("dinhNghia") val dinhNghia: String? = null,
)

// Tần suất sử dụng Component/DamageCode/RepairMethod — phục vụ gợi ý "hay dùng" trên client
@Serializable
data class FrequencyComponent(
    @SerialName("id") val id: Int = 0,
    @SerialName("componentID") val componentId: Int = 0,
    @SerialName("count") val count: Int = 0,
)

@Serializable
data class FrequencyDamageCode(
    @SerialName("id") val id: Int = 0,
    @SerialName("damageCodeID") val damageCodeId: Int = 0,
    @SerialName("count") val count: Int = 0,
)

@Serializable
data class FrequencyRepairMethod(
    @SerialName("id") val id: Int = 0,
    @SerialName("repairMethodID") val repairMethodId: Int = 0,
    @SerialName("count") val count: Int = 0,
)
