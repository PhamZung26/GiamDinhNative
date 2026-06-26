package com.tc128.giamdinhnative.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.tc128.giamdinhnative.ui.screens.about.AboutScreen
import com.tc128.giamdinhnative.ui.screens.camera.CameraPermissionWrapper
import com.tc128.giamdinhnative.ui.screens.chamdiem.ChamDiemScreen
import com.tc128.giamdinhnative.ui.screens.cleancontainers.CleanContainersScreen
import com.tc128.giamdinhnative.ui.screens.damages.DamagesScreen
import com.tc128.giamdinhnative.ui.screens.images.ImagesScreen
import com.tc128.giamdinhnative.ui.screens.itemdetail.ItemDetailScreen
import com.tc128.giamdinhnative.ui.screens.items.ItemsScreen
import com.tc128.giamdinhnative.ui.screens.newitem.NewItemScreen
import com.tc128.giamdinhnative.ui.screens.pendinguploads.PendingUploadsScreen
import com.tc128.giamdinhnative.ui.screens.vesinh.VeSinhScreen

// ── Tab definitions ───────────────────────────────────────────────────────────

sealed class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector = icon
) {
    data object Containers : Tab("tab_containers", "Containers", Icons.Default.ViewList)
    data object Cleaning   : Tab("tab_cleaning",   "Vệ sinh",   Icons.Default.LocalCarWash)
    data object NewItem    : Tab("tab_new_item",   "Tạo mới",   Icons.Default.AddBox)
    data object About      : Tab("tab_about",      "Thông tin", Icons.Default.AccountCircle)
}

private val tabs = listOf(Tab.Containers, Tab.Cleaning, Tab.NewItem, Tab.About)

// Routes dùng bên trong nested nav của tab Containers
private object Inner {
    const val ITEMS          = "items"
    const val ITEM_DETAIL    = "item_detail/{containerId}"
    const val IMAGES         = "images/{containerId}"
    const val DAMAGES        = "damages/{containerId}"
    const val CHAM_DIEM      = "chamdiem/{containerId}"
    const val CAMERA         = "camera/{containerId}?itemEorId={itemEorId}&photoStatus={photoStatus}&updateCleanDate={updateCleanDate}"
    const val PENDING_UPLOADS = "pending_uploads"
    const val CLEAN_CONTAINERS = "clean_containers"
    const val VE_SINH        = "vesinh/{containerId}"

    fun itemDetail(id: Int)                        = "item_detail/$id"
    fun images(id: String)                         = "images/$id"
    fun damages(id: String)                        = "damages/$id"
    fun chamDiem(id: Int)                          = "chamdiem/$id"
    fun camera(
        id: String,
        eorId: Int? = null,
        photoStatus: String? = null,
        updateCleanDate: Boolean = false
    ) = "camera/$id?itemEorId=${eorId ?: ""}&photoStatus=${photoStatus ?: ""}&updateCleanDate=$updateCleanDate"
    fun veSinh(id: Int)                            = "vesinh/$id"
}

// ── MainScreen ────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val rootNav = rememberNavController()

    // Ẩn bottom bar khi đang ở màn hình detail/camera
    val currentEntry by rootNav.currentBackStackEntryAsState()
    val showBottomBar by remember {
        derivedStateOf {
            val route = currentEntry?.destination?.route ?: ""
            // Chỉ hiện bottom bar ở root của mỗi tab
            route in listOf(Tab.Containers.route, Tab.Cleaning.route, Tab.NewItem.route, Tab.About.route,
                            Inner.ITEMS, Inner.CLEAN_CONTAINERS)
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BottomBar(navController = rootNav)
            }
        }
    ) { padding ->
        // Mỗi tab dùng NavHost riêng để giữ back-stack độc lập — chỉ truyền top padding,
        // KHÔNG truyền bottom (Scaffold tính cả phần inset điều hướng hệ thống vào
        // calculateBottomPadding(), truyền xuống sẽ tạo khoảng trắng thừa dưới mỗi màn root vì
        // NavigationBar đã tự chiếm đúng phần không gian của nó rồi — màn nào cần né NavigationBar
        // thì tự thêm Spacer/navigationBarsPadding ở cuối nội dung, như AboutScreen).
        NavHost(
            navController = rootNav,
            startDestination = Tab.Containers.route,
            modifier = androidx.compose.ui.Modifier.padding(top = padding.calculateTopPadding())
        ) {
            // ── Tab: Containers (nested nav) ──────────────────────────────
            navigation(
                startDestination = Inner.ITEMS,
                route = Tab.Containers.route
            ) {
                composable(Inner.ITEMS) {
                    ItemsScreen(
                        onContainerClick = { id -> rootNav.navigate(Inner.itemDetail(id)) },
                        onNewItem = { rootNav.navigate(Tab.NewItem.route) {
                            launchSingleTop = true
                        }}
                    )
                }

                composable(
                    route = Inner.ITEM_DETAIL,
                    arguments = listOf(navArgument("containerId") { type = NavType.IntType })
                ) { back ->
                    val id = back.arguments?.getInt("containerId") ?: 0
                    ItemDetailScreen(
                        containerId = id,
                        onBack = { rootNav.popBackStack() },
                        onOpenImages = { rootNav.navigate(Inner.images(id.toString())) },
                        onOpenDamages = { rootNav.navigate(Inner.damages(id.toString())) },
                        onOpenChamDiem = { rootNav.navigate(Inner.chamDiem(id)) },
                        onCameraDM = { rootNav.navigate(Inner.camera(id.toString(), photoStatus = "PreRepair")) },
                        onCameraAV = { rootNav.navigate(Inner.camera(id.toString(), photoStatus = "Available")) },
                        onCameraVS = { rootNav.navigate(Inner.camera(id.toString(), photoStatus = "Available", updateCleanDate = true)) },
                        onLogout = onLogout
                    )
                }

                composable(
                    route = Inner.CHAM_DIEM,
                    arguments = listOf(navArgument("containerId") { type = NavType.IntType })
                ) { back ->
                    val id = back.arguments?.getInt("containerId") ?: 0
                    ChamDiemScreen(
                        containerId = id,
                        onBack = { rootNav.popBackStack() }
                    )
                }

                composable(
                    route = Inner.IMAGES,
                    arguments = listOf(navArgument("containerId") { type = NavType.StringType })
                ) { back ->
                    val id = back.arguments?.getString("containerId") ?: ""
                    ImagesScreen(
                        containerId = id,
                        onBack = { rootNav.popBackStack() },
                        onOpenCamera = { rootNav.navigate(Inner.camera(it)) }
                    )
                }

                composable(
                    route = Inner.DAMAGES,
                    arguments = listOf(navArgument("containerId") { type = NavType.StringType })
                ) { back ->
                    val id = back.arguments?.getString("containerId") ?: ""
                    DamagesScreen(
                        containerId = id,
                        onBack = { rootNav.popBackStack() },
                        onOpenCamera = { cId, eorId ->
                            rootNav.navigate(Inner.camera(cId, eorId))
                        }
                    )
                }

                composable(
                    route = Inner.CAMERA,
                    arguments = listOf(
                        navArgument("containerId") { type = NavType.StringType },
                        navArgument("itemEorId") {
                            type = NavType.StringType
                            nullable = true; defaultValue = null
                        },
                        navArgument("photoStatus") {
                            type = NavType.StringType
                            nullable = true; defaultValue = null
                        },
                        navArgument("updateCleanDate") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { back ->
                    val id = back.arguments?.getString("containerId") ?: ""
                    val eorId = back.arguments?.getString("itemEorId")?.toIntOrNull()
                    val photoStatus = back.arguments?.getString("photoStatus")?.ifBlank { null }
                    val updateCleanDate = back.arguments?.getBoolean("updateCleanDate") ?: false
                    CameraPermissionWrapper(
                        containerId = id,
                        itemEorId = eorId,
                        photoStatus = photoStatus,
                        updateCleanDate = updateCleanDate,
                        onBack = { rootNav.popBackStack() }
                    )
                }
            }

            // ── Tab: Vệ sinh (nested nav) ──────────────────────────────────
            navigation(
                startDestination = Inner.CLEAN_CONTAINERS,
                route = Tab.Cleaning.route
            ) {
                composable(Inner.CLEAN_CONTAINERS) {
                    CleanContainersScreen(
                        onContainerClick = { id -> rootNav.navigate(Inner.veSinh(id)) }
                    )
                }

                composable(
                    route = Inner.VE_SINH,
                    arguments = listOf(navArgument("containerId") { type = NavType.IntType })
                ) { back ->
                    val id = back.arguments?.getInt("containerId") ?: 0
                    VeSinhScreen(
                        containerId = id,
                        onBack = { rootNav.popBackStack() },
                        onOpenCamera = {
                            rootNav.navigate(Inner.camera(id.toString(), photoStatus = "Available", updateCleanDate = true))
                        }
                    )
                }
            }

            // ── Tab: Tạo mới ─────────────────────────────────────────────
            composable(Tab.NewItem.route) {
                NewItemScreen(
                    onBack = { rootNav.popBackStack() },
                    onCreated = { containerId ->
                        // containerId là numeric id dạng String ("123")
                        val numericId = containerId.toIntOrNull() ?: return@NewItemScreen
                        rootNav.navigate(Inner.itemDetail(numericId)) {
                            popUpTo(Tab.NewItem.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Tab: Tài khoản ────────────────────────────────────────────
            composable(Tab.About.route) {
                AboutScreen(
                    onLogout = onLogout,
                    onOpenPendingUploads = { rootNav.navigate(Inner.PENDING_UPLOADS) }
                )
            }

            composable(Inner.PENDING_UPLOADS) {
                PendingUploadsScreen(onBack = { rootNav.popBackStack() })
            }
        }
    }
}

// ── Bottom Bar ────────────────────────────────────────────────────────────────

@Composable
private fun BottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        // Để NavigationBar tự xử lý system inset (gesture bar / 3-button nav)
        // rồi chỉ giới hạn height phần content bên trong bằng windowInsets(0)
        modifier = Modifier
            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
            .height(56.dp),
        windowInsets = WindowInsets(0, 0, 0, 0),
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}
