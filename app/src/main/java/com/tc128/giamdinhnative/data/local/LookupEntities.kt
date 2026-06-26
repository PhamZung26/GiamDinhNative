package com.tc128.giamdinhnative.data.local

import androidx.room.*

// Cache local cho các danh mục lookup (Component/DamageCode/RepairMethod/ItemRepair) —
// một số danh mục (đặc biệt ItemRepair) khá lớn, lưu Room để dùng offline, giống MAUI (SQLite).

@Entity(tableName = "components")
data class ComponentEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val nameVn: String?,
    val nameEn: String?
)

@Entity(tableName = "damage_codes")
data class DamageCodeEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val name: String?
)

@Entity(tableName = "repair_methods")
data class RepairMethodEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val name: String?
)

@Entity(tableName = "item_repairs")
data class ItemRepairEntity(
    @PrimaryKey val id: Int,
    val componentId: Int,
    val repairMethodId: Int,
    val location: String?,
    val length: Int?,
    val wide: Int?,
    val sts: String?,
    val qty: Int
)

@Entity(tableName = "fast_fills")
data class FastFillEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val name: String?
)

@Entity(tableName = "cham_diems")
data class ChamDiemEntity(
    @PrimaryKey val id: Int,
    val nhom: String?,
    val chiTiet: String?,
    val dienGiai: String,
    val diemSo: Int,
    val dinhNghia: String?
)

// Trước đây Grade/Size/Opt/CleanMethod/Depot chỉ cache RAM (var field trong repository) —
// mất ngay khi app bị kill, màn đầu tiên sau khi mở lại app phải chờ gọi API mới có dữ liệu,
// gây cảm giác load chậm. Cache xuống SQLite như các danh mục khác để có ngay từ lần mở app.
@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val name: String?
)

@Entity(tableName = "sizes")
data class SizeEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val name: String?
)

@Entity(tableName = "opts")
data class OptEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val name: String?
)

@Entity(tableName = "clean_methods")
data class CleanMethodEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val name: String?
)

@Entity(tableName = "depots")
data class DepotEntity(
    @PrimaryKey val id: Int,
    val codeName: String,
    val name: String?
)

@Dao
interface LookupDao {
    @Query("SELECT * FROM components")
    suspend fun getComponents(): List<ComponentEntity>

    @Transaction
    suspend fun replaceComponents(items: List<ComponentEntity>) {
        clearComponents(); insertComponents(items)
    }

    @Query("DELETE FROM components")
    suspend fun clearComponents()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(items: List<ComponentEntity>)

    @Query("SELECT * FROM damage_codes")
    suspend fun getDamageCodes(): List<DamageCodeEntity>

    @Transaction
    suspend fun replaceDamageCodes(items: List<DamageCodeEntity>) {
        clearDamageCodes(); insertDamageCodes(items)
    }

    @Query("DELETE FROM damage_codes")
    suspend fun clearDamageCodes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDamageCodes(items: List<DamageCodeEntity>)

    @Query("SELECT * FROM repair_methods")
    suspend fun getRepairMethods(): List<RepairMethodEntity>

    @Transaction
    suspend fun replaceRepairMethods(items: List<RepairMethodEntity>) {
        clearRepairMethods(); insertRepairMethods(items)
    }

    @Query("DELETE FROM repair_methods")
    suspend fun clearRepairMethods()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepairMethods(items: List<RepairMethodEntity>)

    @Query("SELECT * FROM item_repairs")
    suspend fun getItemRepairs(): List<ItemRepairEntity>

    @Transaction
    suspend fun replaceItemRepairs(items: List<ItemRepairEntity>) {
        clearItemRepairs(); insertItemRepairs(items)
    }

    @Query("DELETE FROM item_repairs")
    suspend fun clearItemRepairs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemRepairs(items: List<ItemRepairEntity>)

    @Query("SELECT * FROM fast_fills")
    suspend fun getFastFills(): List<FastFillEntity>

    @Transaction
    suspend fun replaceFastFills(items: List<FastFillEntity>) {
        clearFastFills(); insertFastFills(items)
    }

    @Query("DELETE FROM fast_fills")
    suspend fun clearFastFills()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFastFills(items: List<FastFillEntity>)

    @Query("SELECT * FROM cham_diems")
    suspend fun getChamDiems(): List<ChamDiemEntity>

    @Transaction
    suspend fun replaceChamDiems(items: List<ChamDiemEntity>) {
        clearChamDiems(); insertChamDiems(items)
    }

    @Query("DELETE FROM cham_diems")
    suspend fun clearChamDiems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChamDiems(items: List<ChamDiemEntity>)

    @Query("SELECT * FROM grades")
    suspend fun getGrades(): List<GradeEntity>

    @Transaction
    suspend fun replaceGrades(items: List<GradeEntity>) {
        clearGrades(); insertGrades(items)
    }

    @Query("DELETE FROM grades")
    suspend fun clearGrades()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(items: List<GradeEntity>)

    @Query("SELECT * FROM sizes")
    suspend fun getSizes(): List<SizeEntity>

    @Transaction
    suspend fun replaceSizes(items: List<SizeEntity>) {
        clearSizes(); insertSizes(items)
    }

    @Query("DELETE FROM sizes")
    suspend fun clearSizes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSizes(items: List<SizeEntity>)

    @Query("SELECT * FROM opts")
    suspend fun getOpts(): List<OptEntity>

    @Transaction
    suspend fun replaceOpts(items: List<OptEntity>) {
        clearOpts(); insertOpts(items)
    }

    @Query("DELETE FROM opts")
    suspend fun clearOpts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpts(items: List<OptEntity>)

    @Query("SELECT * FROM clean_methods")
    suspend fun getCleanMethods(): List<CleanMethodEntity>

    @Transaction
    suspend fun replaceCleanMethods(items: List<CleanMethodEntity>) {
        clearCleanMethods(); insertCleanMethods(items)
    }

    @Query("DELETE FROM clean_methods")
    suspend fun clearCleanMethods()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleanMethods(items: List<CleanMethodEntity>)

    @Query("SELECT * FROM depots")
    suspend fun getDepots(): List<DepotEntity>

    @Transaction
    suspend fun replaceDepots(items: List<DepotEntity>) {
        clearDepots(); insertDepots(items)
    }

    @Query("DELETE FROM depots")
    suspend fun clearDepots()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepots(items: List<DepotEntity>)
}
