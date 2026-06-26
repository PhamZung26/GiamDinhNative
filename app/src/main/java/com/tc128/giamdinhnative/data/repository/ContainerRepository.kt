package com.tc128.giamdinhnative.data.repository

import com.tc128.giamdinhnative.data.model.Container
import com.tc128.giamdinhnative.data.model.StatusOfContainer
import com.tc128.giamdinhnative.data.remote.ApiService
import com.tc128.giamdinhnative.data.remote.JsonPatchOperation
import com.tc128.giamdinhnative.data.remote.UploadCleanDateTimeRequest
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContainerRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getContainers(): List<Container> = apiService.getContainers()

    suspend fun getMoreContainers(lastId: Int): List<Container> =
        apiService.getMoreContainers(lastId)

    suspend fun searchContainers(query: String): List<Container> =
        apiService.searchContainers(query)

    suspend fun getContainer(containerId: Int): Container =
        apiService.getContainer(containerId)

    suspend fun createContainer(
        containerNumber: String,
        sizeId: Int?,
        optId: Int?
    ): Container = apiService.createContainer(
        Container(
            containerNumber = containerNumber,
            sizeId = sizeId,
            optId = optId,
            depotId = 1,
            dateTimeGatein = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            statusOfContainer = StatusOfContainer.Survey
        )
    )

    suspend fun updateContainer(
        containerId: Int,
        containerNumber: String? = null,
        gradeId: Int? = null,
        sizeId: Int? = null,
        optId: Int? = null,
        cleanMethodId: Int? = null,
        remark: String? = null,
        tinhTrang: String? = null,
        seal: String? = null,
        yearManufacture: Int? = null,
        isDamage: Boolean? = null,
        isDirty: Boolean? = null,
        statusOfContainer: Int? = null
    ) {
        val ops = buildList {
            // Path names khớp với C# property names (camelCase của ASP.NET JsonPatch)
            containerNumber?.let { add(JsonPatchOperation("replace", "/containerNo",        JsonPrimitive(it))) }
            gradeId?.let         { add(JsonPatchOperation("replace", "/gradeID",            JsonPrimitive(it))) }
            sizeId?.let          { add(JsonPatchOperation("replace", "/sizeID",             JsonPrimitive(it))) }
            optId?.let           { add(JsonPatchOperation("replace", "/optID",              JsonPrimitive(it))) }
            cleanMethodId?.let   { add(JsonPatchOperation("replace", "/cleanMethodId",      JsonPrimitive(it))) }
            remark?.let          { add(JsonPatchOperation("replace", "/remark",             JsonPrimitive(it))) }
            tinhTrang?.let       { add(JsonPatchOperation("replace", "/tinhTrang",          JsonPrimitive(it))) }
            seal?.let            { add(JsonPatchOperation("replace", "/seal",               JsonPrimitive(it))) }
            yearManufacture?.let { add(JsonPatchOperation("replace", "/yearManuacture",     JsonPrimitive(it))) }
            isDamage?.let        { add(JsonPatchOperation("replace", "/isDamage",           JsonPrimitive(it))) }
            isDirty?.let         { add(JsonPatchOperation("replace", "/isDirty",            JsonPrimitive(it))) }
            statusOfContainer?.let { add(JsonPatchOperation("replace", "/statusOfContainer", JsonPrimitive(it))) }
        }
        if (ops.isNotEmpty()) apiService.updateContainer(containerId, ops)
    }

    suspend fun deleteContainer(containerId: Int) = apiService.deleteContainer(containerId)

    suspend fun getDirtyContainersNotYetClean(
        containerNo: String? = null,
        cleanMethodId: Int? = null,
        isFilterJustClean: Boolean? = null
    ): List<Container> =
        apiService.getDirtyContainersNotYetClean(containerNo, cleanMethodId, isFilterJustClean)

    suspend fun uploadCleanDateTime(containerId: Int) =
        apiService.uploadCleanDateTime(
            UploadCleanDateTimeRequest(
                containerId = containerId,
                dateTimeClean = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        )
}
