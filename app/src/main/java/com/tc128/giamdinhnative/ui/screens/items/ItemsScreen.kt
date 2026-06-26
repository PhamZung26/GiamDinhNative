package com.tc128.giamdinhnative.ui.screens.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tc128.giamdinhnative.data.model.Container
import com.tc128.giamdinhnative.data.model.StatusOfContainer
import com.tc128.giamdinhnative.ui.components.UploadProgressBar
import com.tc128.giamdinhnative.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    onContainerClick: (Int) -> Unit,
    onNewItem: () -> Unit = {},
    viewModel: ItemsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isRefreshing = uiState.isLoading && uiState.containers.isNotEmpty()

    // Reload mỗi khi quay lại màn hình (vd: sau khi tạo container mới hoặc sửa ở màn detail)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    // Load more: derivedStateOf theo dõi vị trí scroll, LaunchedEffect chỉ kích hoạt khi thay đổi true→false→true
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = {
                    Column {
                        Text(
                            "Containers",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!uiState.isLoading && uiState.containers.isNotEmpty()) {
                            Text(
                                "${uiState.containers.size}${if (uiState.hasMore) "+" else ""} containers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewItem,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, null)
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            UploadProgressBar()

            // Search bar với nút clear
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text("Tìm số container...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )

            when {
                // Chỉ hiện spinner toàn màn khi CHƯA có dữ liệu gì để hiện (lần đầu thật sự).
                // Các lần load lại sau (resume, pull-to-refresh) đã có PullToRefreshBox lo việc
                // báo loading rồi — nếu không tách riêng, danh sách đang hiện sẽ bị thay bằng
                // spinner mỗi lần quay lại màn hình, gây hiện tượng chớp.
                uiState.isLoading && uiState.containers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.containers.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔍", fontSize = 40.sp)
                            Text("Không tìm thấy kết quả", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                uiState.containers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📦", fontSize = 40.sp)
                            Text("Không có dữ liệu", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 14.dp, end = 14.dp,
                            top = 4.dp, bottom = 72.dp  // bù BottomBar 56dp + spacing
                        ),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(uiState.containers, key = { it.id }) { container ->
                            ContainerCard(container, onClick = { onContainerClick(container.id) })
                        }
                        if (uiState.isLoadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        } // end Column
        } // end PullToRefreshBox
    }
}

internal fun statusColor(status: StatusOfContainer?): Color = when (status) {
    StatusOfContainer.Draft    -> Color(0xFFF59E0B)  // amber
    StatusOfContainer.Survey   -> Color(0xFF3B82F6)  // blue
    StatusOfContainer.Estimate -> Color(0xFFF97316)  // orange
    StatusOfContainer.Approval -> Color(0xFF8B5CF6)  // violet
    StatusOfContainer.Working  -> Color(0xFF6366F1)  // indigo
    StatusOfContainer.Complete -> Color(0xFF16A34A)  // dark green
    StatusOfContainer.Reject   -> Color(0xFFDC2626)  // red
    null                       -> Color(0xFF94A3B8)  // slate
}

// Grade color map theo chữ cái đầu (mã grade gồm 2 ký tự, vd "AA", "BC"...):
// A tốt nhất → xanh lá, D xấu nhất → đỏ
private fun gradeColor(gradeName: String?): Color = when (gradeName?.uppercase()?.trim()?.firstOrNull()) {
    'A'  -> Color(0xFF16A34A)   // xanh lá đậm
    'B'  -> Color(0xFF2563EB)   // xanh dương
    'C'  -> Color(0xFFD97706)   // cam
    'D'  -> Color(0xFFDC2626)   // đỏ
    else -> Color(0xFF64748B)   // xám
}

private fun gradeLabel(gradeName: String?): String =
    gradeName?.uppercase()?.trim()?.ifEmpty { null } ?: "—"

// Ngày tạo (DateTimeGatein) — server trả ISO local date time, hiển thị giống Xamarin: dd/MM/yyyy HH:mm
private val gateinOutputFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private fun formatGateinDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching { java.time.LocalDateTime.parse(iso).format(gateinOutputFormatter) }.getOrNull()
}

@Composable
private fun ContainerCard(container: Container, onClick: () -> Unit) {
    val grade = container.grade?.codeName
    val color = gradeColor(grade)
    val status = container.statusOfContainer
    val sColor = statusColor(status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Grade badge — hình tròn màu
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gradeLabel(grade),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = color
                )
            }

            // Main content
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = container.containerNumber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    container.size?.codeName?.let { SmallChip(it) }
                    container.opt?.codeName?.let { SmallChip(it) }
                    container.depot?.codeName?.let { SmallChip(it) }
                }
                // Ngày tạo (DateTimeGatein) — giống Xamarin ItemsPage (StringFormat dd/MM/yyyy HH:mm)
                formatGateinDate(container.dateTimeGatein)?.let { dateText ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Schedule, null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateText, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatusChip(status, sColor)
                // Người giám định
                val user = container.nguoiGiamDinh?.hoVaTen ?: container.nguoiTao?.hoVaTen
                if (user != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Person, null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(user, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Thanh màu bên phải theo grade
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun StatusChip(status: StatusOfContainer?, color: Color) {
    val label = status?.displayName ?: "Chưa rõ"
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SmallChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
