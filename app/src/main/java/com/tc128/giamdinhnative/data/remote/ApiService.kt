package com.tc128.giamdinhnative.data.remote

import com.tc128.giamdinhnative.data.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.*

interface ApiService {

    @POST("api/token")
    suspend fun login(@Body body: LoginRequest): String

    @GET("api/containerv2")
    suspend fun getContainers(): List<Container>

    @GET("api/containerv2/GetSoLuong")
    suspend fun getMoreContainers(@Query("cursor") cursor: Int): List<Container>

    @GET("api/containerv2")
    suspend fun searchContainers(@Query("ContainerNo") query: String): List<Container>

    @GET("api/containerv2/{id}")
    suspend fun getContainer(@Path("id") containerId: Int): Container

    @POST("api/containerv2")
    suspend fun createContainer(@Body container: Container): Container

    @PATCH("api/containerv2/giamdinh/{id}")
    suspend fun updateContainer(
        @Path("id") containerId: Int,
        @Body patch: List<JsonPatchOperation>
    )

    @DELETE("api/containerv2/{id}/")
    suspend fun deleteContainer(@Path("id") containerId: Int)

    @GET("api/containerv2/GetDirtyContainersNotYetClean")
    suspend fun getDirtyContainersNotYetClean(
        @Query("containerNo") containerNo: String? = null,
        @Query("cleanMethodId") cleanMethodId: Int? = null,
        @Query("isFilterJustClean") isFilterJustClean: Boolean? = null
    ): List<Container>

    @PUT("api/containerv2/UploadCleanDateTime")
    suspend fun uploadCleanDateTime(@Body body: UploadCleanDateTimeRequest)

    @GET("api/grade")
    suspend fun getGrades(): List<Grade>

    @GET("api/size")
    suspend fun getSizes(): List<Size>

    @GET("api/opt")
    suspend fun getOpts(): List<Opt>

    @GET("api/cleanmethod")
    suspend fun getCleanMethods(): List<CleanMethod>

    @GET("api/component")
    suspend fun getComponents(): List<Component>

    @GET("api/damagecode")
    suspend fun getDamageCodes(): List<DamageCode>

    @GET("api/repairmethod")
    suspend fun getRepairMethods(): List<RepairMethod>

    @GET("api/depot")
    suspend fun getDepots(): List<Depot>

    @GET("api/fastfill")
    suspend fun getFastFills(): List<FastFill>

    @GET("api/ChamDiem")
    suspend fun getChamDiems(): List<ChamDiem>

    @GET("api/FrequencyComponent")
    suspend fun getFrequencyComponents(): List<FrequencyComponent>

    @GET("api/FrequencyDamageCode")
    suspend fun getFrequencyDamageCodes(): List<FrequencyDamageCode>

    @GET("api/FrequencyRepairMethod")
    suspend fun getFrequencyRepairMethods(): List<FrequencyRepairMethod>

    @POST("api/FrequencyComponent/Creates")
    suspend fun createFrequencyComponents(@Body items: List<FrequencyComponent>)

    @POST("api/FrequencyDamageCode/Creates")
    suspend fun createFrequencyDamageCodes(@Body items: List<FrequencyDamageCode>)

    @POST("api/FrequencyRepairMethod/Creates")
    suspend fun createFrequencyRepairMethods(@Body items: List<FrequencyRepairMethod>)

    @GET("api/itemrepair")
    suspend fun getItemRepairs(): List<ItemRepair>

    @GET("api/ItemEor")
    suspend fun getItemEors(@Query("ContainerId") containerId: Int): List<ItemEOR>

    @POST("api/itemeor")
    suspend fun createItemEor(@Body item: ItemEOR): ItemEOR

    @PATCH("api/itemeor/giamdinh/{id}")
    suspend fun updateItemEor(@Path("id") id: Int, @Body patch: List<JsonPatchOperation>)

    @DELETE("api/itemeor/{id}/")
    suspend fun deleteItemEor(@Path("id") id: Int)

    @GET("api/Photo")
    suspend fun getPhotosByContainer(@Query("ContainerId") containerId: Int): List<com.tc128.giamdinhnative.data.model.Photo>

    @Multipart
    @POST("api/photov2")
    suspend fun uploadPhoto(
        @Part("IdContainerOnServer") containerId: okhttp3.RequestBody,
        @Part("DateCreate") dateCreate: okhttp3.RequestBody,
        @Part("Status") status: okhttp3.RequestBody,
        @Part("ItemEORId") itemEorId: okhttp3.RequestBody?,
        @Part image: okhttp3.MultipartBody.Part   // field name "Image" set in Part
    ): okhttp3.ResponseBody

    // Endpoint riêng cho app native — tách khỏi "latest" của app Xamarin vì 2 app khác
    // package name/version code/file APK, không thể dùng chung 1 bản ghi "latest"
    @GET("api/updatev2/latest/native")
    suspend fun getLatestAppUpdate(): AppUpdateInfo
}

@Serializable
data class LoginRequest(
    @kotlinx.serialization.SerialName("Email")    val email: String,
    @kotlinx.serialization.SerialName("Password") val password: String,
)

@Serializable
data class UploadCleanDateTimeRequest(
    @kotlinx.serialization.SerialName("ContainerId") val containerId: Int,
    @kotlinx.serialization.SerialName("DateTimeClean") val dateTimeClean: String,
)

@Serializable
data class AppUpdateInfo(
    @kotlinx.serialization.SerialName("versionCode") val versionCode: Int = 0,
    @kotlinx.serialization.SerialName("versionName") val versionName: String? = null,
    @kotlinx.serialization.SerialName("apkUrl") val apkUrl: String? = null,
    @kotlinx.serialization.SerialName("releaseNotes") val releaseNotes: String? = null,
)

@Serializable
data class JsonPatchOperation(
    val op: String,
    val path: String,
    val value: JsonElement   // JsonElement tự serialize được với kotlinx-serialization
)
