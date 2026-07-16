package com.tc128.giamdinhnative

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tc128.giamdinhnative.worker.PhotoResizeWorker
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
        // Lên lịch resize định kỳ mỗi 15 phút — worker này luôn chain sang upload ở cuối,
        // nên đây đã là đường upload định kỳ duy nhất. KHÔNG lịch riêng PhotoUploadWorker định kỳ
        // để tránh 2 upload-worker chạy song song gây upload trùng (xem PhotoUploadWorker).
        PhotoResizeWorker.schedulePeriodicResize(this)
    }
}
