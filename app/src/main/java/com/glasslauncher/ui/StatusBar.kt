package com.glasslauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Tinggi area status bar (tetap dipakai walau status bar sistem disembunyikan). */
@Composable
fun statusBarHeight(): Dp {
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.statusBarsIgnoringVisibility.getTop(this).toDp() }
    return maxOf(top, 30.dp)
}

@Composable
fun rememberNow(): State<LocalDateTime> = produceState(initialValue = LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(60_000L - (System.currentTimeMillis() % 60_000L) + 20L)
    }
}

data class BatteryInfo(val level: Int, val charging: Boolean)

private fun readBattery(i: Intent?): BatteryInfo {
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo(pct, charging)
}

@Composable
fun rememberBattery(): State<BatteryInfo> {
    val ctx = LocalContext.current
    val state = remember {
        mutableStateOf(readBattery(ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))))
    }
    DisposableEffect(ctx) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                state.value = readBattery(i)
            }
        }
        ContextCompat.registerReceiver(
            ctx,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { ctx.unregisterReceiver(receiver) }
    }
    return state
}

@Composable
fun IosStatusBar(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val now by rememberNow()
    val battery by rememberBattery()
    val formatter = remember {
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm")
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(statusBarHeight())
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GText(now.format(formatter), size = 16.sp, weight = FontWeight.SemiBold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SignalBars()
            WifiIcon()
            GText("${battery.level}%", size = 12.sp, weight = FontWeight.Medium)
            BatteryIcon(battery)
        }
    }
}

@Composable
private fun SignalBars() {
    Canvas(Modifier.size(18.dp, 12.dp)) {
        val barW = size.width * 0.18f
        val gap = (size.width - 4 * barW) / 3f
        for (i in 0 until 4) {
            val h = size.height * (0.35f + 0.2167f * i)
            drawRoundRect(
                Color.White,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 3f)
            )
        }
    }
}

@Composable
private fun WifiIcon() {
    Canvas(Modifier.size(17.dp, 12.dp)) {
        val cx = size.width / 2f
        val cy = size.height
        val stroke = 1.8.dp.toPx()
        for (i in 1..3) {
            val r = size.height * i / 3f
            drawArc(
                Color.White,
                225f,
                90f,
                false,
                Offset(cx - r, cy - r),
                Size(2 * r, 2 * r),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        drawCircle(Color.White, radius = 1.6.dp.toPx(), center = Offset(cx, cy - 1.6.dp.toPx()))
    }
}

@Composable
private fun BatteryIcon(info: BatteryInfo) {
    Canvas(Modifier.size(26.dp, 12.dp)) {
        val nub = 2.dp.toPx()
        val bodyW = size.width - nub
        val stroke = 1.dp.toPx()
        val radius = CornerRadius(3.5.dp.toPx())
        drawRoundRect(
            Color.White.copy(alpha = 0.45f),
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(bodyW - stroke, size.height - stroke),
            cornerRadius = radius,
            style = Stroke(stroke)
        )
        drawRoundRect(
            Color.White.copy(alpha = 0.45f),
            topLeft = Offset(bodyW + 0.5.dp.toPx(), size.height * 0.32f),
            size = Size(nub - 0.5.dp.toPx(), size.height * 0.36f),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
        val fillColor = when {
            info.charging -> Color(0xFF34C759)
            info.level <= 20 -> Color(0xFFFF453A)
            else -> Color.White
        }
        val pad = 2.dp.toPx()
        val fullW = bodyW - 2 * pad
        drawRoundRect(
            fillColor,
            topLeft = Offset(pad, pad),
            size = Size((fullW * info.level / 100f).coerceAtLeast(2.dp.toPx()), size.height - 2 * pad),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}
