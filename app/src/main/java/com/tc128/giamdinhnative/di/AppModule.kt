package com.tc128.giamdinhnative.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Đánh dấu CoroutineScope sống theo vòng đời toàn ứng dụng (không bị hủy khi 1 màn hình rời đi). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Scope cấp ứng dụng cho các tác vụ fire-and-forget PHẢI hoàn tất kể cả khi màn hình gọi đã bị
     * đóng — ví dụ: lưu ảnh + lên lịch worker sau khi chụp. viewModelScope bị hủy khi user bấm Back
     * ngay sau chụp, làm mất các bước chạy sau điểm suspend (đã gây lỗi "không update vệ sinh").
     * SupervisorJob: 1 tác vụ lỗi không kéo sập các tác vụ khác.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
