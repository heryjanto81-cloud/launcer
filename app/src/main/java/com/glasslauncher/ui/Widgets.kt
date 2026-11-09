package com.glasslauncher.ui

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasslauncher.AppEntry
import com.glasslauncher.LauncherViewModel
import com.glasslauncher.S
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Halaman paling kiri: "Today View" berisi widget kaca. */
@Composable
fun TodayView(
    vm: LauncherViewModel,
    suggestions: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassSearchBar(onSearch, S.t("Search", "Cari"))
        ClockWidget()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BatteryWidget(Modifier.weight(1f).height(150.dp))
            StorageWidget(Modifier.weight(1f).height(150.dp))
        }
        CalendarWidget()
        SuggestionsWidget(suggestions, onLaunch, onLongPress)
        NotesWidget(vm.note, vm::updateNote)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun GlassSearchBar(onClick: () -> Unit, hint: String, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier.fillMaxWidth().height(44.dp).noRippleClickable(onClick),
        RoundedCornerShape(22.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchIcon()
            Spacer(Modifier.width(8.dp))
            GText(hint, size = 17.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

// ---------------------------------------------------------------------------
// Jam
// ---------------------------------------------------------------------------

private fun greeting(hour: Int): String = when {
    hour < 11 -> S.t("Good morning", "Selamat pagi")
    hour < 15 -> S.t("Good afternoon", "Selamat siang")
    hour < 18 -> S.t("Good afternoon", "Selamat sore")
    else -> S.t("Good evening", "Selamat malam")
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    val now by rememberNow()
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }

    GlassSurface(modifier.fillMaxWidth().height(160.dp), RoundedCornerShape(30.dp)) {
        Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            AnalogClock(now, Modifier.size(124.dp))
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                GText(greeting(now.hour), size = 13.sp, color = Color.White.copy(alpha = 0.75f))
                GText(now.format(timeFmt), size = 42.sp, weight = FontWeight.Bold)
                GText(now.format(dateFmt), size = 13.sp, color = Color.White.copy(alpha = 0.75f))
            }
        }
    }
}

private fun polar(c: Offset, radius: Float, deg: Float): Offset {
    val rad = (deg - 90f) * (PI.toFloat() / 180f)
    return Offset(c.x + radius * cos(rad), c.y + radius * sin(rad))
}

@Composable
fun AnalogClock(now: LocalDateTime, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(Color.White.copy(alpha = 0.12f), radius = r)
        drawCircle(Color.White.copy(alpha = 0.35f), radius = r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
        for (i in 0 until 12) {
            val major = i % 3 == 0
            drawLine(
                Color.White.copy(alpha = if (major) 0.95f else 0.5f),
                polar(c, r * 0.80f, i * 30f),
                polar(c, r * 0.90f, i * 30f),
                strokeWidth = if (major) 2.5.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        val minute = now.minute + now.second / 60f
        val hour = (now.hour % 12) + minute / 60f
        drawLine(Color.White, c, polar(c, r * 0.48f, hour * 30f), strokeWidth = 4.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color.White, c, polar(c, r * 0.72f, minute * 6f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Color(0xFFFF9F0A), radius = 4.dp.toPx(), center = c)
    }
}

// ---------------------------------------------------------------------------
// Baterai & penyimpanan (cincin)
// ---------------------------------------------------------------------------

@Composable
private fun RingWidget(
    modifier: Modifier,
    progress: Float,
    color: Color,
    centerText: String,
    caption: String
) {
    GlassSurface(modifier, RoundedCornerShape(30.dp)) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(2.dp)) {
                    val sw = 9.dp.toPx()
                    val d = size.minDimension - sw
                    val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                    drawArc(
                        Color.White.copy(alpha = 0.18f), 0f, 360f, false,
                        topLeft, Size(d, d), style = Stroke(sw)
                    )
                    drawArc(
                        color, -90f, 360f * progress.coerceIn(0f, 1f), false,
                        topLeft, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round)
                    )
                }
                GText(centerText, size = 18.sp, weight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            GText(caption, size = 12.sp, color = Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
fun BatteryWidget(modifier: Modifier = Modifier) {
    val battery by rememberBattery()
    val color = when {
        battery.charging -> Color(0xFF34C759)
        battery.level <= 20 -> Color(0xFFFF453A)
        else -> Color(0xFF30D158)
    }
    RingWidget(
        modifier,
        battery.level / 100f,
        color,
        "${battery.level}%",
        if (battery.charging) S.t("Charging", "Mengisi daya") else S.t("Battery", "Baterai")
    )
}

@Composable
fun StorageWidget(modifier: Modifier = Modifier) {
    val storage = remember {
        try {
            val s = StatFs(Environment.getDataDirectory().path)
            val total = s.totalBytes
            Pair(total - s.availableBytes, total)
        } catch (_: Exception) {
            Pair(0L, 1L)
        }
    }
    val used = storage.first
    val total = storage.second
    val usedGb = used / 1_000_000_000.0
    val totalGb = total / 1_000_000_000.0
    RingWidget(
        modifier,
        if (total > 0) used.toFloat() / total.toFloat() else 0f,
        Color(0xFF0A84FF),
        "%.0f GB".format(usedGb),
        S.t("of %.0f GB".format(totalGb), "dari %.0f GB".format(totalGb))
    )
}

// ---------------------------------------------------------------------------
// Kalender
// ---------------------------------------------------------------------------

@Composable
fun CalendarWidget(modifier: Modifier = Modifier) {
    val now by rememberNow()
    val today: LocalDate = now.toLocalDate()
    val first = today.withDayOfMonth(1)
    val startOffset = first.dayOfWeek.value % 7 // Minggu = 0
    val daysInMonth = today.lengthOfMonth()
    val rows = (startOffset + daysInMonth + 6) / 7
    val letters = S.t("S M T W T F S", "M S S R K J S").split(" ")
    val monthName = today.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())

    GlassSurface(modifier.fillMaxWidth(), RoundedCornerShape(30.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GText(monthName.uppercase(), size = 13.sp, weight = FontWeight.Bold, color = Color(0xFFFF453A))
                GText(today.year.toString(), size = 13.sp, color = Color.White.copy(alpha = 0.6f))
            }
            Row(Modifier.fillMaxWidth()) {
                letters.forEach {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        GText(it, size = 11.sp, color = Color.White.copy(alpha = 0.55f), weight = FontWeight.SemiBold)
                    }
                }
            }
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c - startOffset + 1
                        Box(Modifier.weight(1f).height(28.dp), contentAlignment = Alignment.Center) {
                            if (day in 1..daysInMonth) {
                                if (day == today.dayOfMonth) {
                                    Box(
                                        Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFFF453A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GText(day.toString(), size = 13.sp, weight = FontWeight.Bold, align = TextAlign.Center)
                                    }
                                } else {
                                    GText(day.toString(), size = 13.sp, align = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Saran aplikasi & catatan
// ---------------------------------------------------------------------------

@Composable
fun SuggestionsWidget(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(modifier.fillMaxWidth(), RoundedCornerShape(30.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GText(S.t("Suggestions", "Saran"), size = 13.sp, weight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                apps.forEach { a ->
                    AppIconView(a, onClick = { onLaunch(a) }, onLongClick = { onLongPress(a) })
                }
            }
        }
    }
}

@Composable
fun NotesWidget(note: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(modifier.fillMaxWidth().heightIn(min = 120.dp), RoundedCornerShape(30.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GText(S.t("Notes", "Catatan"), size = 14.sp, weight = FontWeight.Bold, color = Color(0xFFFFD60A))
            BasicTextField(
                value = note,
                onValueChange = onChange,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                        if (note.isEmpty()) {
                            GText(
                                S.t("Tap to write a note…", "Ketuk untuk menulis catatan…"),
                                size = 15.sp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}
