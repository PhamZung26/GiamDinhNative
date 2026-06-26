package com.tc128.giamdinhnative

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tc128.giamdinhnative.worker.PhotoResizeWorker
import com.tc128.giamdinhnative.worker.PhotoUploadWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GiamDinhNativeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Lên lịch upload định kỳ mỗi 15 phút ngay khi app khởi động
        PhotoResizeWorker.schedulePeriodicResize(this)
        PhotoUploadWorker.schedulePeriodicUpload(this)
    }
}
