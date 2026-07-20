package com.tc128.giamdinhnative.ui.screens.images

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.tc128.giamdinhnative.ui.components.ZoomableImagePagerDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImagesScreen(
    containerId: String,
    onBack: () -> Unit,
    onOpenCamera: (containerId: String) -> Unit,
    viewModel: ImagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var zoomIndex by remember { mutableStateOf<Int?>(null) }

    // Danh sách ảnh gộp (local trước, server sau) — dùng cho next/pre trong màn xem ảnh toàn màn hình
    val allModels: List<Any> = remember(uiState.photos, uiState.serverUrls) {
        uiState.photos.mapNotNull { photo ->
            (photo.pathLocal?.let { java.io.File(it) }) ?: photo.pathServer
        } + uiState.serverUrls
    }

    zoomIndex?.let { index ->
        ZoomableImagePagerDialog(models = allModels, initialIndex = index, onDismiss = { zoomIndex = null })
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(containerId, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.load(containerId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hình ảnh", fontWeight = FontWeight.Bold)
                        Text(containerId, fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (cameraPermission.status.isGranted) onOpenCamera(containerId)
                    else cameraPermission.launchPermissionRequest()
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when {
                // Chỉ hiện spinner toàn màn khi CHƯA có ảnh nào để hiện — màn hình reload mỗi lần
                // quay lại (vd: sau khi chụp ảnh), nếu không tách riêng thì ảnh đang hiện sẽ bị
                // thay bằng spinner mỗi lần resume, gây chớp.
                uiState.isLoading && uiState.photos.isEmpty() && uiState.serverUrls.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.photos.isEmpty() && uiState.serverUrls.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📷", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Chưa có hình ảnh", style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Nhấn nút camera để chụp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(uiState.photos, key = { _, it -> "local_${it.id}" }) { index, photo ->
                            PhotoGridItem(
                                photo = photo,
                                onDelete = { viewModel.deletePhoto(photo) },
                                onZoom = { zoomIndex = index }
                            )
                        }
                        val localCount = uiState.photos.size
                        itemsIndexed(uiState.serverUrls, key = { _, it -> "server_$it" }) { index, url ->
                            ServerPhotoGridItem(url = url, onZoom = { zoomIndex = localCount + index })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerPhotoGridItem(url: String, onZoom: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onZoom)
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: com.tc128.giamdinhnative.data.local.PhotoEntity,
    onDelete: () -> Unit,
    onZoom: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    if (showError && photo.lastError != null) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Lỗi upload ảnh") },
            text = { Text(photo.lastError, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { showError = false }) { Text("Đóng") } }
        )
    }

    val imagePath = photo.pathLocal ?: photo.pathServer
    val imageModel = imagePath?.let { if (photo.pathLocal != null) java.io.File(photo.pathLocal) else it }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (imageModel != null) Modifier.clickable(onClick = onZoom) else Modifier)
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Upload badge (top-left) — đỏ + bấm xem chi tiết lỗi nếu lần upload gần nhất thất bại
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(3.dp)
                .then(
                    if (photo.lastError != null) Modifier.clickable { showError = true } else Modifier
                )
        ) {
            Icon(
                imageVector = when {
                    photo.lastError != null -> Icons.Default.ErrorOutline
                    photo.isUploaded -> Icons.Default.CloudDone
                    else -> Icons.Default.CloudUpload
                },
                contentDescription = if (photo.lastError != null) "Xem lỗi upload" else null,
                tint = when {
                    photo.lastError != null -> Color(0xFFEF4444)
                    photo.isUploaded -> Color(0xFF4ADE80)
                    else -> Color(0xFFFBBF24)
                },
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
