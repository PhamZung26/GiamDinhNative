package com.tc128.giamdinhnative.data.local

import androidx.room.*

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val containerNumber: String?,
    val itemEorId: Int?,
    val pathLocal: String? = null,
    val pathServer: String? = null,
    val serverId: Int? = null,
    val status: String = "Available",   // Available / PreRepair / PostRepair
    val isUploaded: Boolean = false,
    val isResized: Boolean = false,
    val isBackedUp: Boolean = false,
    val isDeletedLocal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastError: String? = null,   // lỗi chi tiết từ lần upload gần nhất (null nếu upload thành công)
    val uploadAttempts: Int = 0,

    // Ảnh chụp lúc quét seal — khi upload lên server chính, đồng thời gửi thêm 1 bản lên server
    // OCR (ocr.phamdung.uk/uploadseal/) để tập hợp dữ liệu huấn luyện, thay dần ocr.space
    val isSeal: Boolean = false,
    val sealNumber: String? = null,
    val isSealUploaded: Boolean = false
)

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity): Long

    @Update
    suspend fun update(photo: PhotoEntity)

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("SELECT * FROM photos WHERE containerNumber = :containerNumber ORDER BY createdAt DESC")
    suspend fun getByContainer(containerNumber: String): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE itemEorId = :itemEorId ORDER BY createdAt DESC")
    suspend fun getByItemEor(itemEorId: Int): List<PhotoEntity>

    // Lấy ảnh chưa resize, có file local
    @Query("SELECT * FROM photos WHERE isResized = 0 AND pathLocal IS NOT NULL ORDER BY createdAt ASC LIMIT 20")
    suspend fun getPendingResize(): List<PhotoEntity>

    // Chỉ upload ảnh đã resize xong
    @Query("SELECT * FROM photos WHERE isUploaded = 0 AND isResized = 1 AND pathLocal IS NOT NULL ORDER BY createdAt ASC LIMIT 10")
    suspend fun getPendingUpload(): List<PhotoEntity>

    // Toàn bộ ảnh chưa upload (không giới hạn số lượng, không cần đã resize) — dùng cho màn hình xem danh sách chờ
    @Query("SELECT * FROM photos WHERE isUploaded = 0 ORDER BY createdAt DESC")
    suspend fun getAllPending(): List<PhotoEntity>

    @Query("UPDATE photos SET isResized = 1 WHERE id = :id")
    suspend fun markResized(id: Long)

    @Query("UPDATE photos SET lastError = :error, uploadAttempts = uploadAttempts + 1 WHERE id = :id")
    suspend fun markUploadError(id: Long, error: String)

    // Giống Xamarin: upload xong chỉ đánh dấu isUploaded, KHÔNG xoá ngay file/record local
    @Query("UPDATE photos SET isUploaded = 1, lastError = NULL WHERE id = :id")
    suspend fun markUploaded(id: Long)

    // Dọn ảnh đã upload quá lâu — giống Xamarin DeleteItemsOverTime (ngưỡng 7 ngày)
    @Query("SELECT * FROM photos WHERE isUploaded = 1 AND createdAt < :thresholdMillis")
    suspend fun getUploadedOlderThan(thresholdMillis: Long): List<PhotoEntity>

    @Query("UPDATE photos SET isSealUploaded = 1 WHERE id = :id")
    suspend fun markSealUploaded(id: Long)
}

@Database(
    entities = [
        PhotoEntity::class,
        ComponentEntity::class,
        DamageCodeEntity::class,
        RepairMethodEntity::class,
        ItemRepairEntity::class,
        FastFillEntity::class,
        ChamDiemEntity::class,
        GradeEntity::class,
        SizeEntity::class,
        OptEntity::class,
        CleanMethodEntity::class,
        DepotEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun lookupDao(): LookupDao
}
