package com.glasslauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.AppEntry
import com.glasslauncher.LibraryGroup
import com.glasslauncher.S

/** Halaman paling kanan: App Library, aplikasi otomatis terkelompok per kategori. */
@Composable
fun LibraryPage(
    groups: List<LibraryGroup>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onOpenGroup: (LibraryGroup) -> Unit,
    onSearch: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        GlassSearchBar(
            onSearch,
            S.t("App Library", "Pustaka Aplikasi"),
            Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(groups, key = { it.title }) { g ->
                LibraryTile(g, onLaunch, onLongPress) { onOpenGroup(g) }
            }
        }
    }
}

@Composable
private fun LibraryTile(
    group: LibraryGroup,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onOpen: () -> Unit
) {
    val apps = group.apps
    val useCluster = apps.size > 4

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassSurface(
            Modifier.fillMaxWidth().aspectRatio(1f).noRippleClickable(onOpen),
            RoundedCornerShape(26.dp)
        ) {
            Column(
                Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (row in 0 until 2) {
                    Row(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 2) {
                            val idx = row * 2 + col
                            Box(
                                Modifier.weight(1f).fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (useCluster && idx == 3) {
                                    MiniCluster(apps.drop(3).take(4), onOpen)
                                } else {
                                    apps.getOrNull(idx)?.let { a ->
                                        AppIconImage(
                                            a,
                                            Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .iosPress({ onLaunch(a) }, { onLongPress(a) })
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        GText(group.title, size = 12.sp, weight = FontWeight.Medium, shadow = true)
    }
}

@Composable
private fun MiniCluster(apps: List<AppEntry>, onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxSize().noRippleClickable(onOpen),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        for (r in 0 until 2) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (c in 0 until 2) {
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        apps.getOrNull(r * 2 + c)?.let { AppIconImage(it, Modifier.fillMaxSize()) }
                    }
                }
            }
        }
    }
}

/** Folder / kategori yang dibuka: kartu kaca di tengah layar dengan animasi membesar. */
@Composable
fun GroupOverlay(
    group: LibraryGroup?,
    onDismiss: () -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit
) {
    val last = remember { mutableStateOf<LibraryGroup?>(null) }
    if (group != null) last.value = group
    val g = last.value

    AnimatedVisibility(
        visible = group != null,
        enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 380f), initialScale = 0.75f),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.85f)
    ) {
        if (g != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .noRippleClickable(onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 22.dp)
                ) {
                    GText(g.title, size = 22.sp, weight = FontWeight.SemiBold, shadow = true)
                    Spacer(Modifier.height(14.dp))
                    GlassSurface(
                        Modifier.fillMaxWidth().noRippleClickable { },
                        RoundedCornerShape(38.dp),
                        strength = 1.3f
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.heightIn(max = 440.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(g.apps, key = { it.packageName }) { a ->
                                AppIconView(
                                    a,
                                    onClick = { onLaunch(a) },
                                    onLongClick = { onLongPress(a) },
                                    iconSize = 58.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
