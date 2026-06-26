package com.tc128.giamdinhnative.data.repository

import com.tc128.giamdinhnative.data.model.ChamDiem
import javax.inject.Inject
import javax.inject.Singleton

// Danh mục hạng mục chấm điểm (giống Xamarin App.chamDiemService.GetItems() — đọc từ cache local,
// đồng bộ /api/ChamDiem qua LookupRepository). Màn ChamDiem chỉ tính tổng điểm tại chỗ, không gửi lên server.
@Singleton
class ChamDiemRepository @Inject constructor(
    private val lookupRepository: LookupRepository
) {
    suspend fun getTemplate(): List<ChamDiem> = lookupRepository.getChamDiemTemplate()
}
