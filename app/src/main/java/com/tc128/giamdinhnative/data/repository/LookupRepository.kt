package com.tc128.giamdinhnative.data.repository

import com.tc128.giamdinhnative.data.local.ChamDiemEntity
import com.tc128.giamdinhnative.data.local.CleanMethodEntity
import com.tc128.giamdinhnative.data.local.ComponentEntity
import com.tc128.giamdinhnative.data.local.DamageCodeEntity
import com.tc128.giamdinhnative.data.local.DepotEntity
import com.tc128.giamdinhnative.data.local.FastFillEntity
import com.tc128.giamdinhnative.data.local.GradeEntity
import com.tc128.giamdinhnative.data.local.ItemRepairEntity
import com.tc128.giamdinhnative.data.local.LookupDao
import com.tc128.giamdinhnative.data.local.OptEntity
import com.tc128.giamdinhnative.data.local.RepairMethodEntity
import com.tc128.giamdinhnative.data.local.SizeEntity
import com.tc128.giamdinhnative.data.model.ChamDiem
import com.tc128.giamdinhnative.data.model.Component
import com.tc128.giamdinhnative.data.model.DamageCode
import com.tc128.giamdinhnative.data.model.FastFill
import com.tc128.giamdinhnative.data.model.ItemRepair
import com.tc128.giamdinhnative.data.model.RepairMethod
import com.tc128.giamdinhnative.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

// Mọi danh mục lookup đều cache xuống Room (SQLite), đọc local trước, chỉ gọi API khi local
// rỗng (lần đầu) hoặc khi refresh() được gọi tay. Giống MAUI (SQLite local cache) — tránh màn
// hình đầu tiên sau khi mở app phải chờ API mới load được, vì cache RAM mất ngay khi app bị kill.
@Singleton
class LookupRepository @Inject constructor(
    private val apiService: ApiService,
    private val lookupDao: LookupDao
) {
    suspend fun getGrades(): List<Pair<Int, String>> {
        val local = lookupDao.getGrades()
        if (local.isNotEmpty()) return local.map { it.id to it.codeName }
        val remote = apiService.getGrades()
        lookupDao.replaceGrades(remote.map { GradeEntity(it.id, it.codeName, it.name) })
        return remote.map { it.id to it.codeName }
    }

    suspend fun getSizes(): List<Pair<Int, String>> {
        val local = lookupDao.getSizes()
        val pairs = if (local.isNotEmpty()) {
            local.map { it.id to it.codeName }
        } else {
            val remote = apiService.getSizes()
            lookupDao.replaceSizes(remote.map { SizeEntity(it.id, it.codeName, it.name) })
            remote.map { it.id to it.codeName }
        }
        return pairs.sortedBy { it.second.uppercase() }
    }

    suspend fun getOpts(): List<Pair<Int, String>> {
        val local = lookupDao.getOpts()
        val pairs = if (local.isNotEmpty()) {
            local.map { it.id to it.codeName }
        } else {
            val remote = apiService.getOpts()
            lookupDao.replaceOpts(remote.map { OptEntity(it.id, it.codeName, it.name) })
            remote.map { it.id to it.codeName }
        }
        return pairs.sortedBy { it.second.uppercase() }
    }

    suspend fun getCleanMethods(): List<Pair<Int, String>> {
        val local = lookupDao.getCleanMethods()
        if (local.isNotEmpty()) return local.map { it.id to it.codeName }
        val remote = apiService.getCleanMethods()
        lookupDao.replaceCleanMethods(remote.map { CleanMethodEntity(it.id, it.codeName, it.name) })
        return remote.map { it.id to it.codeName }
    }

    suspend fun getDepots(): List<Pair<Int, String>> {
        val local = lookupDao.getDepots()
        if (local.isNotEmpty()) return local.map { it.id to it.codeName }
        val remote = apiService.getDepots()
        lookupDao.replaceDepots(remote.map { DepotEntity(it.id, it.codeName, it.name) })
        return remote.map { it.id to it.codeName }
    }

    suspend fun getFastFills(): List<FastFill> {
        val local = lookupDao.getFastFills()
        if (local.isNotEmpty()) return local.map { it.toModel() }
        val remote = apiService.getFastFills()
        lookupDao.replaceFastFills(remote.map { it.toEntity() })
        return remote
    }

    // ChamDiem là catalog hạng mục checklist (không phải điểm đã chấm của 1 container) — cache như danh mục khác
    suspend fun getChamDiemTemplate(): List<ChamDiem> {
        val local = lookupDao.getChamDiems()
        if (local.isNotEmpty()) return local.map { it.toModel() }
        val remote = apiService.getChamDiems()
        lookupDao.replaceChamDiems(remote.map { it.toEntity() })
        return remote
    }

    suspend fun getComponents(): List<Component> {
        val local = lookupDao.getComponents()
        if (local.isNotEmpty()) return local.map { it.toModel() }
        val remote = apiService.getComponents()
        lookupDao.replaceComponents(remote.map { it.toEntity() })
        return remote
    }

    suspend fun getDamageCodes(): List<DamageCode> {
        val local = lookupDao.getDamageCodes()
        if (local.isNotEmpty()) return local.map { it.toModel() }
        val remote = apiService.getDamageCodes()
        lookupDao.replaceDamageCodes(remote.map { it.toEntity() })
        return remote
    }

    suspend fun getRepairMethods(): List<RepairMethod> {
        val local = lookupDao.getRepairMethods()
        if (local.isNotEmpty()) return local.map { it.toModel() }
        val remote = apiService.getRepairMethods()
        lookupDao.replaceRepairMethods(remote.map { it.toEntity() })
        return remote
    }

    // Catalog mẫu sửa chữa — có thể rất lớn, ưu tiên đọc local, lọc client-side giống MAUI
    suspend fun getItemRepairs(): List<ItemRepair> {
        val local = lookupDao.getItemRepairs()
        if (local.isNotEmpty()) return local.map { it.toModel() }
        val remote = apiService.getItemRepairs()
        lookupDao.replaceItemRepairs(remote.map { it.toEntity() })
        return remote
    }

    // Đồng bộ lại toàn bộ danh mục từ server, ghi đè local — gọi tay khi cần (vd: pull-to-refresh)
    suspend fun refreshAll() {
        val components = apiService.getComponents()
        lookupDao.replaceComponents(components.map { it.toEntity() })

        val damageCodes = apiService.getDamageCodes()
        lookupDao.replaceDamageCodes(damageCodes.map { it.toEntity() })

        val repairMethods = apiService.getRepairMethods()
        lookupDao.replaceRepairMethods(repairMethods.map { it.toEntity() })

        val itemRepairs = apiService.getItemRepairs()
        lookupDao.replaceItemRepairs(itemRepairs.map { it.toEntity() })

        val grades = apiService.getGrades()
        lookupDao.replaceGrades(grades.map { GradeEntity(it.id, it.codeName, it.name) })

        val sizes = apiService.getSizes()
        lookupDao.replaceSizes(sizes.map { SizeEntity(it.id, it.codeName, it.name) })

        val opts = apiService.getOpts()
        lookupDao.replaceOpts(opts.map { OptEntity(it.id, it.codeName, it.name) })

        val cleanMethods = apiService.getCleanMethods()
        lookupDao.replaceCleanMethods(cleanMethods.map { CleanMethodEntity(it.id, it.codeName, it.name) })

        val depots = apiService.getDepots()
        lookupDao.replaceDepots(depots.map { DepotEntity(it.id, it.codeName, it.name) })

        val fastFills = apiService.getFastFills()
        lookupDao.replaceFastFills(fastFills.map { it.toEntity() })

        val chamDiems = apiService.getChamDiems()
        lookupDao.replaceChamDiems(chamDiems.map { it.toEntity() })
    }
}

private fun ComponentEntity.toModel() = Component(id, codeName, nameVn, nameEn)
private fun Component.toEntity() = ComponentEntity(id, codeName, nameVn, nameEn)

private fun DamageCodeEntity.toModel() = DamageCode(id, codeName, name)
private fun DamageCode.toEntity() = DamageCodeEntity(id, codeName, name)

private fun RepairMethodEntity.toModel() = RepairMethod(id, codeName, name)
private fun RepairMethod.toEntity() = RepairMethodEntity(id, codeName, name)

private fun ItemRepairEntity.toModel() = ItemRepair(id, componentId, repairMethodId, location, length, wide, sts, qty)
private fun ItemRepair.toEntity() = ItemRepairEntity(id, componentId, repairMethodId, location, length, wide, sts, qty)

private fun FastFillEntity.toModel() = FastFill(id, codeName, name)
private fun FastFill.toEntity() = FastFillEntity(id, codeName, name)

private fun ChamDiemEntity.toModel() = ChamDiem(id, nhom, chiTiet, dienGiai, diemSo, dinhNghia)
private fun ChamDiem.toEntity() = ChamDiemEntity(id, nhom, chiTiet, dienGiai, diemSo, dinhNghia)
