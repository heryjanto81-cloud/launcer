@file:OptIn(ExperimentalFoundationApi::class)

package com.glasslauncher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.AppEntry

// ---------------------------------------------------------------------------
// Wallpaper (digambar lewat kode: gradasi + "blob" warna, mirip wallpaper iOS)
// ---------------------------------------------------------------------------

data class WallpaperPreset(val base: List<Color>, val blobs: List<Color>)

val Wallpapers = listOf(
    WallpaperPreset(
        base = listOf(Color(0xFF0A0F2C), Color(0xFF1A2A6C), Color(0xFF2D1B69)),
        blobs = listOf(Color(0xFF00D4FF), Color(0xFFFF3D9A), Color(0xFF8B5CF6))
    ),
    WallpaperPreset(
        base = listOf(Color(0xFF2A0845), Color(0xFF6A1B6F), Color(0xFFB5365C)),
        blobs = listOf(Color(0xFFFF9A3C), Color(0xFFFF4D6D), Color(0xFFFFD166))
    ),
    WallpaperPreset(
        base = listOf(Color(0xFF021B3A), Color(0xFF05547A), Color(0xFF0B8AA5)),
        blobs = listOf(Color(0xFF2DE2C6), Color(0xFF3A86FF), Color(0xFF00B4D8))
    ),
    WallpaperPreset(
        base = listOf(Color(0xFF3B1E6E), Color(0xFF8E54E9), Color(0xFFC779D0)),
        blobs = listOf(Color(0xFF7CE0FF), Color(0xFFFFA8D6), Color(0xFFB8A1FF))
    ),
    WallpaperPreset(
        base = listOf(Color(0xFF0B0B0F), Color(0xFF1C1C24), Color(0xFF2A2A36)),
        blobs = listOf(Color(0xFF4F5BD5), Color(0xFF8E8E9A), Color(0xFF30D5C8))
    )
)

@Composable
fun WallpaperLayer(preset: WallpaperPreset, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            Brush.linearGradient(
                preset.base,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        val r = size.maxDimension * 0.6f
        val centers = listOf(
            Offset(size.width * 0.15f, size.height * 0.15f),
            Offset(size.width * 0.9f, size.height * 0.45f),
            Offset(size.width * 0.3f, size.height * 0.92f)
        )
        preset.blobs.forEachIndexed { i, color ->
            val center = centers[i % centers.size]
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = 0.7f), Color.Transparent),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Efek kaca
// ---------------------------------------------------------------------------

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    strength: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.10f))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.26f * strength),
                        Color.White.copy(alpha = 0.10f * strength)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.7f),
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.4f)
                    )
                ),
                shape
            ),
        content = content
    )
}

val LabelShadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 1.5f), 5f)

@Composable
fun GText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 15.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.White,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    shadow: Boolean = false
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            textAlign = align,
            shadow = if (shadow) LabelShadow else null
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/** Efek tekan ala iOS: ikon mengecil pelan lalu memantul kembali. */
@Composable
fun Modifier.iosPress(onClick: () -> Unit, onLongClick: (() -> Unit)? = null): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 450f),
        label = "press"
    )
    return this
        .graphicsLayer {
            scaleX = s
            scaleY = s
        }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
}

// ---------------------------------------------------------------------------
// Ikon aplikasi
// ---------------------------------------------------------------------------

val IconShape = RoundedCornerShape(22)

@Composable
fun AppIconImage(app: AppEntry, modifier: Modifier = Modifier) {
    Image(
        bitmap = app.icon,
        contentDescription = app.label,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(IconShape)
            .border(0.6.dp, Color.White.copy(alpha = 0.22f), IconShape)
    )
}

@Composable
fun AppIconView(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 60.dp,
    showLabel: Boolean = true
) {
    Column(
        modifier = modifier.iosPress(onClick, onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIconImage(app, Modifier.size(iconSize))
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            GText(
                text = app.label,
                size = 11.sp,
                align = TextAlign.Center,
                shadow = true,
                modifier = Modifier.widthIn(max = iconSize + 16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Komponen kecil
// ---------------------------------------------------------------------------

@Composable
fun SearchIcon(modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.7f)) {
    Canvas(modifier.size(16.dp)) {
        val sw = 2.dp.toPx()
        val r = size.minDimension * 0.34f
        drawCircle(color, radius = r, center = Offset(r + sw / 2, r + sw / 2), style = Stroke(sw))
        val s = r + sw / 2 + r * 0.72f
        drawLine(
            color,
            Offset(s, s),
            Offset(size.width - sw / 2, size.height - sw / 2),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, label = "switchX")
    val bg by animateColorAsState(
        if (checked) Color(0xFF34C759) else Color.White.copy(alpha = 0.25f),
        label = "switchBg"
    )
    Box(
        Modifier
            .size(51.dp, 31.dp)
            .clip(CircleShape)
            .background(bg)
            .noRippleClickable { onChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x, 2.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
