package com.tc128.giamdinhnative.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

private val LogoutIcon: ImageVector get() = Icons.Default.ExitToApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onLogout: () -> Unit,
    onOpenPendingUploads: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Reload số ảnh chờ upload mỗi khi quay lại màn hình (vd: sau khi xóa/upload ở PendingUploadsScreen)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.load()
        }
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLogout()
    }

    LaunchedEffect(uiState.catalogSyncMessage) {
        uiState.catalogSyncMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissCatalogSyncMessage()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(LogoutIcon, null) },
            title = { Text("Đăng xuất") },
            text = { Text("Bạn có chắc muốn đăng xuất?") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Đăng xuất") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông tin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.size(88.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(com.tc128.giamdinhnative.R.drawable.logo_tc128),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("GiamDinh Native", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("v${uiState.currentVersionName}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cập nhật", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()

                    val update = uiState.availableUpdate
                    if (update != null) {
                        InfoRow(
                            icon = Icons.Default.SystemUpdate,
                            label = "Có bản cập nhật mới",
                            value = "v${update.versionName}",
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                        update.releaseNotes?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = viewModel::downloadUpdate,
                            enabled = !uiState.isDownloadingUpdate,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tải và cài đặt")
                        }
                    } else {
                        uiState.updateError?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = viewModel::checkForUpdate,
                            enabled = !uiState.isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Đang kiểm tra...")
                            } else {
                                Icon(Icons.Default.SystemUpdate, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Kiểm tra cập nhật")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tài khoản", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    InfoRow(Icons.Default.Person, "Người dùng", uiState.username ?: "—")
                    InfoRow(Icons.Default.Cloud, "Server", "tc128hp.hopto.org")
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Danh mục", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    Text(
                        "Size, Opt, Grade, Phương pháp vệ sinh... tự đồng bộ mỗi lần đăng nhập. Bấm để đồng bộ lại ngay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = viewModel::syncCatalog,
                        enabled = !uiState.isSyncingCatalog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSyncingCatalog) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Đang đồng bộ...")
                        } else {
                            Icon(Icons.Default.Sync, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Đồng bộ danh mục")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onOpenPendingUploads)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Trạng thái đồng bộ", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    InfoRow(
                        icon = if (uiState.pendingUploadCount == 0) Icons.Default.CloudDone
                               else Icons.Default.CloudUpload,
                        label = "Ảnh chờ upload (bấm để xem danh sách)",
                        value = if (uiState.pendingUploadCount == 0) "Đã đồng bộ tất cả ✓"
                                else "${uiState.pendingUploadCount} ảnh",
                        valueColor = if (uiState.pendingUploadCount == 0)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    if (uiState.pendingUploadCount > 0) {
                        Button(
                            onClick = viewModel::triggerUpload,
                            enabled = !uiState.isUploading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Đang upload...")
                            } else {
                                Icon(Icons.Default.CloudUpload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Upload ngay")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(LogoutIcon, null)
                Spacer(Modifier.width(8.dp))
                Text("Đăng xuất")
            }

            // Né NavigationBar dưới cùng (BottomBar tab) — không dựa vào Scaffold's bottom padding
            // vì NavigationBar đã tự chiếm đúng phần không gian của nó rồi (xem MainScreen.kt)
            Spacer(Modifier.navigationBarsPadding().height(32.dp))
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor)
        }
    }
}
