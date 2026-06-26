package com.tc128.giamdinhnative.ui.screens.chamdiem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// Màu khớp tên màu Xamarin.Forms dùng trong ChamDiem.xaml
private val OrangeRed = Color(0xFFFF4500)
private val Green = Color(0xFF008000)
private val GreenYellow = Color(0xFFADFF2F)
private val Red = Color(0xFFFF0000)
private val MidnightBlue = Color(0xFF191970)
private val Goldenrod = Color(0xFFDAA520)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChamDiemScreen(
    containerId: Int,
    onBack: () -> Unit,
    viewModel: ChamDiemViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(containerId) { viewModel.load(containerId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tổng điểm
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tổng điểm:", color = OrangeRed, fontSize = 30.sp)
                Spacer(Modifier.width(6.dp))
                Text("${uiState.tongDiem}", color = OrangeRed, fontSize = 30.sp)
            }

            // Header: tên nhóm hiện tại
            Text(
                uiState.currentGroupName ?: "",
                fontSize = 24.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Danh sách mục chấm điểm của nhóm hiện tại
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(uiState.currentGroupRows.filter { it.visible }, key = { it.item.id }) { row ->
                    ChamDiemItemRow(row = row, onCheckedChange = { viewModel.onCheckChanged(row, it) })
                }
            }

            // Thanh gradient chú thích grade (tĩnh, giống Xamarin)
            GradeLegendBar()

            // Năm SX - Tuổi
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Năm SX: ", color = Goldenrod, fontSize = 20.sp)
                Text("${uiState.yearManufacture}", color = Goldenrod, fontSize = 20.sp)
                Text(" - ", color = Goldenrod, fontSize = 20.sp)
                Text("Tuổi: ", color = Goldenrod, fontSize = 20.sp)
                Text("${uiState.tuoi}", color = Goldenrod, fontSize = 20.sp)
            }

            // Trước | Sau
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = viewModel::truoc,
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightBlue)
                ) { Text("Trước") }
                Button(
                    onClick = viewModel::sau,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeRed)
                ) { Text("Sau") }
            }
        }
    }
}

@Composable
private fun ChamDiemItemRow(row: ChamDiemRow, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = row.checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = OrangeRed)
        )
        Text(row.item.dienGiai, fontSize = 18.sp, modifier = Modifier.weight(1f).padding(top = 8.dp))
        Text("${row.item.diemSo}", color = OrangeRed, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

// 3 đoạn gradient liền nhau + nhãn — đúng layout XAML: Green->GreenYellow->OrangeRed->Red
@Composable
private fun GradeLegendBar() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(10.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Green, GreenYellow))))
            Box(Modifier.weight(1f).fillMaxHeight().background(Brush.horizontalGradient(listOf(GreenYellow, OrangeRed))))
            Box(Modifier.weight(1f).fillMaxHeight().background(Brush.horizontalGradient(listOf(OrangeRed, Red))))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AA", color = Green, fontSize = 24.sp)
                Text("0 -> 3", fontSize = 24.sp)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BB", color = GreenYellow, fontSize = 24.sp)
                Text("4 -> 6", fontSize = 24.sp)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CC", color = OrangeRed, fontSize = 24.sp)
                Text("7 -> ...", fontSize = 24.sp)
            }
        }
    }
}
