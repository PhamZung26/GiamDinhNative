package com.tc128.giamdinhnative.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tc128.giamdinhnative.worker.PhotoUploadWorker
import kotlinx.coroutines.delay

/**
 * Banner nhỏ hiển thị trạng thái upload realtime.
 * Đặt bên trong Scaffold content, ngay dưới TopAppBar của các màn hình cần theo dõi.
 *
 * Tự ẩn sau 3 giây khi upload xong.
 */
@Composable
fun UploadProgressBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val workInfos by produceState<List<WorkInfo>>(initialValue = emptyList()) {
        WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(PhotoUploadWorker.WORK_NAME)
            .observeForever { value = it ?: emptyList() }
    }

    val activeWork = workInfos.firstOrNull {
        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
    }
    val lastDone = workInfos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED }

    val uploaded = activeWork?.progress?.getInt(PhotoUploadWorker.KEY_UPLOADED, -1) ?: -1
    val total    = activeWork?.progress?.getInt(PhotoUploadWorker.KEY_TOTAL, 0) ?: 0
    val message  = activeWork?.progress?.getString(PhotoUploadWorker.KEY_PROGRESS)

    // Trạng thái hiển thị
    val isRunning = activeWork?.state == WorkInfo.State.RUNNING && total > 0
    var showDone by remember { mutableStateOf(false) }

    LaunchedEffect(lastDone?.id) {
        if (lastDone != null) {
            showDone = true
            delay(3000)
            showDone = false
        }
    }

    val visible = isRunning || showDone

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = if (isRunning) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message ?: "Đang upload ảnh...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (uploaded >= 0 && total > 0) {
                            LinearProgressIndicator(
                                progress = { uploaded.toFloat() / total },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .height(3.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (total > 0) {
                        Text(
                            text = "$uploaded/$total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Upload hoàn tất ✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
