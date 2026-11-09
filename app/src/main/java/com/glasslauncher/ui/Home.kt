@file:OptIn(ExperimentalFoundationApi::class)

package com.glasslauncher.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.glasslauncher.AppEntry
import com.glasslauncher.HomeItem
import com.glasslauncher.LauncherViewModel
import com.glasslauncher.LibraryGroup
import com.glasslauncher.buildHomeItems
import com.glasslauncher.buildLibrary
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

const val COLS = 4
const val ROWS = 6
const val PER_PAGE = COLS * ROWS

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val ctx = LocalContext.current
    val view = LocalView.current

    val apps = vm.apps
    val dockList = vm.dock
    val hiddenSet = vm.hidden
    val sorted = vm.sortedHome
    val counts = vm.counts

    val dockApps = remember(apps, dockList) {
        dockList.mapNotNull { p -> apps.firstOrNull { it.packageName == p } }
    }
    val pages: List<List<HomeItem>> = remember(apps, dockList, hiddenSet, sorted) {
        val chunks = buildHomeItems(apps, dockList.toSet(), hiddenSet, sorted).chunked(PER_PAGE)
        if (chunks.isEmpty()) listOf(emptyList<HomeItem>()) else chunks
    }
    val groups = remember(apps, counts) { buildLibrary(apps, counts) }
    val suggestions = remember(apps, counts) {
        apps.sortedByDescending { counts[it.packageName] ?: 0 }.take(4)
    }

    var openGroup by remember { mutableStateOf<LibraryGroup?>(null) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val overlayOpen = openGroup != null || menuApp != null || showSearch || showSettings

    val pageCount = pages.size + 2 // Today View + halaman Home + App Library
    val pager = rememberPagerState(initialPage = 1) { pageCount }
    val scope = rememberCoroutineScope()

    val closeAll = {
        openGroup = null
        menuApp = null
        showSearch = false
        showSettings = false
    }
    val launchApp: (AppEntry) -> Unit = { a ->
        closeAll()
        vm.launch(ctx, a)
    }
    val longPress: (AppEntry) -> Unit = { a -> menuApp = a }

    // Status bar sistem: sembunyikan kalau memakai status bar kustom
    val customBar = vm.customStatusBar
    SideEffect {
        val window = view.context.findActivity()?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (customBar) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    BackHandler(enabled = overlayOpen || pager.currentPage != 1) {
        when {
            menuApp != null -> menuApp = null
            showSettings -> showSettings = false
            showSearch -> showSearch = false
            openGroup != null -> openGroup = null
            else -> scope.launch { pager.animateScrollToPage(1) }
        }
    }

    LaunchedEffect(vm.homeTick) {
        if (vm.homeTick > 0) {
            closeAll()
            pager.animateScrollToPage(1)
        }
    }

    val wallpaper = Wallpapers[vm.wallpaper.coerceIn(0, Wallpapers.lastIndex)]
    val blurAmount by animateDpAsState(if (overlayOpen) 24.dp else 0.dp, label = "blur")
    val dimScale by animateFloatAsState(if (overlayOpen) 0.95f else 1f, label = "dimScale")

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        WallpaperLayer(wallpaper, Modifier.fillMaxSize())

        // ----- Konten utama (di-blur saat ada folder/menu/pencarian terbuka) -----
        Column(
            Modifier
                .fillMaxSize()
                .blur(blurAmount)
                .graphicsLayer {
                    scaleX = dimScale
                    scaleY = dimScale
                }
        ) {
            Spacer(Modifier.height(statusBarHeight()))

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { showSettings = true })
                    }
            ) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    Box(Modifier.fillMaxSize().pageEffect(pager, page)) {
                        when {
                            page == 0 -> TodayView(
                                vm = vm,
                                suggestions = suggestions,
                                onLaunch = launchApp,
                                onLongPress = longPress,
                                onSearch = { showSearch = true }
                            )
                            page == pageCount - 1 -> LibraryPage(
                                groups = groups,
                                onLaunch = launchApp,
                                onLongPress = longPress,
                                onOpenGroup = { openGroup = it },
                                onSearch = { showSearch = true }
                            )
                            else -> HomeGridPage(
                                items = pages.getOrElse(page - 1) { emptyList() },
                                onLaunch = launchApp,
                                onLongPress = longPress,
                                onOpenFolder = { openGroup = it },
                                onSearch = { showSearch = true }
                            )
                        }
                    }
                }
            }

            val onHome = pager.currentPage in 1..pages.size
            val dockAlpha by animateFloatAsState(if (onHome) 1f else 0f, label = "dockAlpha")
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageDots(pager)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.graphicsLayer { alpha = dockAlpha }) {
                    Dock(
                        apps = dockApps,
                        enabled = onHome,
                        onLaunch = launchApp,
                        onLongPress = longPress
                    )
                }
            }
        }

        // ----- Overlay -----
        GroupOverlay(
            group = openGroup,
            onDismiss = { openGroup = null },
            onLaunch = launchApp,
            onLongPress = longPress
        )
        SearchOverlay(
            visible = showSearch,
            apps = apps,
            onDismiss = { showSearch = false },
            onLaunch = launchApp,
            onLongPress = longPress
        )
        SettingsSheet(
            visible = showSettings,
            selectedWallpaper = vm.wallpaper,
            sortedHome = sorted,
            customStatusBar = customBar,
            hiddenCount = hiddenSet.size,
            onDismiss = { showSettings = false },
            onWallpaper = vm::selectWallpaper,
            onSortedChange = vm::setSortedHome,
            onStatusBarChange = vm::setCustomStatusBar,
            onRestoreHidden = vm::restoreHidden,
            onDefaultLauncher = { vm.openDefaultLauncherSettings(ctx) }
        )
        AppMenuOverlay(
            app = menuApp,
            inDock = menuApp?.let { it.packageName in dockList } == true,
            hiddenFromHome = menuApp?.let { it.packageName in hiddenSet } == true,
            onDismiss = { menuApp = null },
            onToggleDock = {
                menuApp?.let { vm.toggleDock(it.packageName) }
                menuApp = null
            },
            onToggleHidden = {
                menuApp?.let { vm.toggleHidden(it.packageName) }
                menuApp = null
            },
            onInfo = {
                menuApp?.let { vm.showInfo(ctx, it.packageName) }
                menuApp = null
            },
            onUninstall = {
                menuApp?.let { vm.uninstall(ctx, it.packageName) }
                menuApp = null
            }
        )

        // ----- Status bar ala iPhone (selalu di atas semuanya) -----
        if (customBar) {
            IosStatusBar(Modifier.align(Alignment.TopCenter))
        }
    }
}

/** Halaman bergeser: skala & transparansi berubah halus saat digeser (parallax tipis). */
private fun Modifier.pageEffect(state: PagerState, page: Int): Modifier = graphicsLayer {
    val offset = ((state.currentPage - page) + state.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
    val s = 1f - 0.1f * offset
    scaleX = s
    scaleY = s
    alpha = 1f - 0.6f * offset
}

@Composable
private fun HomeGridPage(
    items: List<HomeItem>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onOpenFolder: (LibraryGroup) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                // Geser ke bawah di halaman Home -> buka pencarian (seperti iPhone)
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > 140f) onSearch() },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy }
                )
            }
    ) {
        for (r in 0 until ROWS) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (c in 0 until COLS) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val item = items.getOrNull(r * COLS + c)) {
                            is HomeItem.App -> AppIconView(
                                item.app,
                                onClick = { onLaunch(item.app) },
                                onLongClick = { onLongPress(item.app) }
                            )
                            is HomeItem.Folder -> FolderIcon(item.group) { onOpenFolder(item.group) }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }
}

/** Ikon folder kaca berisi pratinjau 3x3 aplikasi di dalamnya. */
@Composable
private fun FolderIcon(group: LibraryGroup, onClick: () -> Unit) {
    val shown = group.apps.take(9)
    Column(
        Modifier.iosPress(onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassSurface(Modifier.size(60.dp), RoundedCornerShape(14.dp), strength = 1.2f) {
            Column(
                Modifier.fillMaxSize().padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (r in 0 until 3) {
                    Row(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (c in 0 until 3) {
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                shown.getOrNull(r * 3 + c)?.let { AppIconImage(it, Modifier.fillMaxSize()) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        GText(group.title, size = 11.sp, shadow = true, weight = FontWeight.Normal)
    }
}

@Composable
private fun PageDots(pager: PagerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pager.pageCount) { i ->
            val selected = i == pager.currentPage
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (selected) 0.95f else 0.35f))
            )
        }
    }
}

@Composable
private fun Dock(
    apps: List<AppEntry>,
    enabled: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit
) {
    GlassSurface(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(88.dp),
        RoundedCornerShape(34.dp),
        strength = 1.2f
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { a ->
                AppIconView(
                    a,
                    onClick = { if (enabled) onLaunch(a) },
                    onLongClick = { if (enabled) onLongPress(a) },
                    showLabel = false
                )
            }
        }
    }
}
