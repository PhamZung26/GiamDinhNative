package com.tc128.giamdinhnative.ui.screens.camera

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionWrapper(
    containerId: String,
    itemEorId: Int?,
    photoStatus: String? = null,
    updateCleanDate: Boolean = false,
    onBack: () -> Unit,
    onPhotoCaptured: (String) -> Unit = {},
    viewModel: CameraViewModel = hiltViewModel()
) {
    val permission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }

    when {
        permission.status.isGranted -> {
            CameraScreen(
                containerId = containerId,
                itemEorId = itemEorId,
                onBack = onBack,
                onPhotoCaptured = { path ->
                    viewModel.onPhotoCaptured(containerId, itemEorId, path, photoStatus, updateCleanDate)
                    onPhotoCaptured(path)
                }
            )
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (permission.status.shouldShowRationale)
                        "Ứng dụng cần quyền Camera để chụp ảnh container."
                    else
                        "Quyền Camera bị từ chối. Vui lòng cấp quyền trong Cài đặt.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permission.launchPermissionRequest() }) {
                    Text("Cấp quyền Camera")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onBack) { Text("Quay lại") }
            }
        }
    }
}
