package com.tc128.giamdinhnative.ui.screens.cleancontainers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tc128.giamdinhnative.data.model.Container

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanContainersScreen(
    onContainerClick: (Int) -> Unit,
    viewModel: CleanContainersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    if (showFilterDialog) {
        CleanMethodFilterDialog(
            cleanMethods = uiState.cleanMethods,
            selectedId = uiState.selectedCleanMethodId,
            isFilterJustClean = uiState.isFilterJustClean,
            onSelectCleanMethod = viewModel::onCleanMethodFilterChange,
            onFilterJustCleanChange = viewModel::onFilterJustCleanChange,
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Container cần vệ sinh") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterAlt, contentDescription = "Lọc")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("Tìm số container...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.containers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocalCarWash,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Không có container cần vệ sinh", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.containers, key = { it.id }) { container ->
                        DirtyContainerRow(container, onClick = { onContainerClick(container.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DirtyContainerRow(container: Container, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth().clickableRow(onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    container.containerNumber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${container.sizeName ?: "?"} • ${container.cleanMethodName ?: "Chưa chọn PP vệ sinh"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Ngày tạo (DateTimeGatein) — giống danh sách container chính
                formatGateinDate(container.dateTimeGatein)?.let { dateText ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Schedule, null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dateText, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Icon(Icons.Default.LocalCarWash, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

// Ngày tạo (DateTimeGatein) — server trả ISO local date time, hiển thị dd/MM/yyyy HH:mm
private val gateinOutputFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private fun formatGateinDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching { java.time.LocalDateTime.parse(iso).format(gateinOutputFormatter) }.getOrNull()
}

// Popup lọc — tương đương popup "Lọc theo phương án vệ sinh" của Xamarin (chip chọn 1 phương án,
// không có lựa chọn "Tất cả", kèm checkbox hiện container vừa vệ sinh xong)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CleanMethodFilterDialog(
    cleanMethods: List<Pair<Int, String>>,
    selectedId: Int?,
    isFilterJustClean: Boolean,
    onSelectCleanMethod: (Int) -> Unit,
    onFilterJustCleanChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lọc theo phương án vệ sinh") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    cleanMethods.forEach { (id, name) ->
                        FilterChip(
                            selected = selectedId == id,
                            onClick = { onSelectCleanMethod(id) },
                            label = { Text(name) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Hiển thị container mới vệ sinh xong",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = isFilterJustClean, onCheckedChange = onFilterJustCleanChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}
