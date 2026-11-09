package com.glasslauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.AppEntry
import com.glasslauncher.S
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Pencarian
// ---------------------------------------------------------------------------

@Composable
fun SearchOverlay(
    visible: Boolean,
    apps: List<AppEntry>,
    onDismiss: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(visible) {
        if (visible) {
            query = ""
            delay(120)
            try {
                focus.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(120))
    ) {
        val results = remember(query, apps) {
            if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
        }
        val launchApp: (AppEntry) -> Unit = { a ->
            focusManager.clearFocus()
            onLaunch(a)
        }
        val close = {
            focusManager.clearFocus()
            onDismiss()
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .imePadding()
        ) {
            Spacer(Modifier.height(statusBarHeight()))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSurface(Modifier.weight(1f).height(44.dp), RoundedCornerShape(22.dp)) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchIcon()
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                results.firstOrNull()?.let { launchApp(it) }
                            }),
                            modifier = Modifier.weight(1f).focusRequester(focus),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        GText(
                                            S.t("Search apps", "Cari aplikasi"),
                                            size = 17.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                GText(
                    S.t("Cancel", "Batal"),
                    size = 17.sp,
                    color = Color(0xFF0A84FF),
                    modifier = Modifier.noRippleClickable { close() }
                )
            }
            if (results.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    GText(S.t("No results", "Tidak ada hasil"), size = 17.sp, color = Color.White.copy(alpha = 0.6f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(results, key = { it.packageName }) { a ->
                        AppIconView(
                            a,
                            onClick = { launchApp(a) },
                            onLongClick = { onLongPress(a) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Menu tekan-lama pada ikon
// ---------------------------------------------------------------------------

@Composable
fun AppMenuOverlay(
    app: AppEntry?,
    inDock: Boolean,
    hiddenFromHome: Boolean,
    onDismiss: () -> Unit,
    onToggleDock: () -> Unit,
    onToggleHidden: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    val last = remember { mutableStateOf<AppEntry?>(null) }
    if (app != null) last.value = app
    val a = last.value

    AnimatedVisibility(
        visible = app != null,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(120))
    ) {
        if (a != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .noRippleClickable(onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIconImage(a, Modifier.size(76.dp))
                    Spacer(Modifier.height(8.dp))
                    GText(a.label, size = 17.sp, weight = FontWeight.SemiBold, shadow = true)
                    Spacer(Modifier.height(18.dp))
                    GlassSurface(
                        Modifier.width(260.dp).noRippleClickable { },
                        RoundedCornerShape(18.dp),
                        strength = 1.4f
                    ) {
                        Column {
                            MenuRow(
                                if (inDock) S.t("Remove from Dock", "Hapus dari Dock")
                                else S.t("Add to Dock", "Tambah ke Dock"),
                                Color.White,
                                onToggleDock
                            )
                            MenuDivider()
                            MenuRow(
                                if (hiddenFromHome) S.t("Show on Home Screen", "Tampilkan di Layar Utama")
                                else S.t("Hide from Home Screen", "Sembunyikan dari Layar Utama"),
                                Color.White,
                                onToggleHidden
                            )
                            MenuDivider()
                            MenuRow(S.t("App Info", "Info Aplikasi"), Color.White, onInfo)
                            MenuDivider()
                            MenuRow(S.t("Uninstall App", "Hapus Aplikasi"), Color(0xFFFF453A), onUninstall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(48.dp).noRippleClickable(onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        GText(text, size = 17.sp, color = color)
    }
}

@Composable
private fun MenuDivider() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.2f)))
}

// ---------------------------------------------------------------------------
// Pengaturan layar utama (tekan lama di area kosong)
// ---------------------------------------------------------------------------

@Composable
fun SettingsSheet(
    visible: Boolean,
    selectedWallpaper: Int,
    sortedHome: Boolean,
    customStatusBar: Boolean,
    hiddenCount: Int,
    onDismiss: () -> Unit,
    onWallpaper: (Int) -> Unit,
    onSortedChange: (Boolean) -> Unit,
    onStatusBarChange: (Boolean) -> Unit,
    onRestoreHidden: () -> Unit,
    onDefaultLauncher: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + slideInVertically(tween(260)) { it / 2 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .noRippleClickable(onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            GlassSurface(
                Modifier.fillMaxWidth().noRippleClickable { },
                RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                strength = 1.4f
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        GText(S.t("Home Screen", "Layar Utama"), size = 20.sp, weight = FontWeight.Bold)
                        GText(
                            S.t("Done", "Selesai"),
                            size = 17.sp,
                            weight = FontWeight.SemiBold,
                            color = Color(0xFF0A84FF),
                            modifier = Modifier.noRippleClickable(onDismiss)
                        )
                    }

                    GText(S.t("Wallpaper", "Wallpaper"), size = 13.sp, color = Color.White.copy(alpha = 0.65f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Wallpapers.forEachIndexed { i, wp ->
                            val selected = i == selectedWallpaper
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(wp.blobs[0], wp.base[1], wp.blobs[1])))
                                    .border(
                                        if (selected) 3.dp else 1.dp,
                                        if (selected) Color.White else Color.White.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .noRippleClickable { onWallpaper(i) }
                            )
                        }
                    }

                    SettingRow(
                        S.t("Auto-sort into folders", "Urutkan otomatis ke folder"),
                        S.t("Games, Photos, Google, Social… grouped for you", "Game, Foto, Google, Sosial… dikelompokkan otomatis"),
                        sortedHome,
                        onSortedChange
                    )
                    SettingRow(
                        S.t("Custom status bar", "Status bar kustom"),
                        S.t("Swipe down from the top to open notifications", "Geser dari atas untuk membuka notifikasi"),
                        customStatusBar,
                        onStatusBarChange
                    )

                    if (hiddenCount > 0) {
                        ActionRow(
                            S.t("Restore hidden apps ($hiddenCount)", "Tampilkan lagi app tersembunyi ($hiddenCount)"),
                            onRestoreHidden
                        )
                    }
                    ActionRow(S.t("Set as default launcher", "Jadikan launcher utama"), onDefaultLauncher)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            GText(title, size = 16.sp, weight = FontWeight.Medium)
            GText(subtitle, size = 12.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 2)
        }
        Spacer(Modifier.width(12.dp))
        GlassSwitch(checked, onChange)
    }
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    GlassSurface(
        Modifier.fillMaxWidth().height(48.dp).noRippleClickable(onClick),
        RoundedCornerShape(14.dp),
        strength = 0.8f
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            GText(text, size = 16.sp, color = Color(0xFF0A84FF), align = TextAlign.Start)
        }
    }
}
