package com.tc128.giamdinhnative.data.repository

import com.tc128.giamdinhnative.data.model.ItemEOR
import com.tc128.giamdinhnative.data.remote.ApiService
import com.tc128.giamdinhnative.data.remote.JsonPatchOperation
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemEorRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getByContainer(containerId: Int): List<ItemEOR> =
        apiService.getItemEors(containerId)

    suspend fun save(item: ItemEOR) {
        if (item.id == 0) {
            // Giống MAUI: null nav-prop lồng trước khi gửi, chỉ gửi FK id + scalar
            apiService.createItemEor(item.copy(component = null, damageCode = null, repairMethod = null))
        } else {
            val ops = buildList {
                item.componentId?.takeIf { it != 0 }?.let { add(JsonPatchOperation("replace", "/componentID", JsonPrimitive(it))) }
                item.damageCodeId?.takeIf { it != 0 }?.let { add(JsonPatchOperation("replace", "/damageCodeID", JsonPrimitive(it))) }
                item.repairMethodId?.takeIf { it != 0 }?.let { add(JsonPatchOperation("replace", "/repairMethodID", JsonPrimitive(it))) }
                if (item.qty != 0) add(JsonPatchOperation("replace", "/qty", JsonPrimitive(item.qty)))
                item.length?.takeIf { it != 0.0 }?.let { add(JsonPatchOperation("replace", "/length", JsonPrimitive(it))) }
                item.wide?.takeIf { it != 0.0 }?.let { add(JsonPatchOperation("replace", "/wide", JsonPrimitive(it))) }
                item.seq?.let { add(JsonPatchOperation("replace", "/seq", JsonPrimitive(it))) }
                item.location?.takeIf { it.isNotBlank() }?.let { add(JsonPatchOperation("replace", "/location", JsonPrimitive(it))) }
                item.sts?.takeIf { it.isNotBlank() }?.let { add(JsonPatchOperation("replace", "/sts", JsonPrimitive(it))) }
                item.payer?.let { add(JsonPatchOperation("replace", "/payer", JsonPrimitive(it.ordinal))) }
                add(JsonPatchOperation("replace", "/status", JsonPrimitive(item.status.ordinal)))
            }
            if (ops.isNotEmpty()) apiService.updateItemEor(item.id, ops)
        }
    }

    suspend fun delete(id: Int) = apiService.deleteItemEor(id)
}
