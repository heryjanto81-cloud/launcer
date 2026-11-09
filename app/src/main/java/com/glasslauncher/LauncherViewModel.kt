package com.glasslauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("glass_launcher", Context.MODE_PRIVATE)

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set

    var dock by mutableStateOf(readList("dock"))
        private set

    var hidden by mutableStateOf<Set<String>>(prefs.getStringSet("hidden", emptySet())?.toSet() ?: emptySet())
        private set

    var sortedHome by mutableStateOf(prefs.getBoolean("sorted_home", true))
        private set

    var customStatusBar by mutableStateOf(prefs.getBoolean("custom_status", true))
        private set

    var wallpaper by mutableIntStateOf(prefs.getInt("wallpaper", 0))
        private set

    var note by mutableStateOf(prefs.getString("note", "") ?: "")
        private set

    var counts by mutableStateOf(loadCounts())
        private set

    var homeTick by mutableIntStateOf(0)
        private set

    init {
        refresh()
    }

    // ---------- Data aplikasi ----------

    fun refresh() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val list = withContext(Dispatchers.Default) { loadApps(ctx) }
            apps = list
            ensureDock()
        }
    }

    private fun loadApps(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .mapNotNull { ri ->
                try {
                    val ai = ri.activityInfo
                    val installTime = try {
                        pm.getPackageInfo(ai.packageName, 0).firstInstallTime
                    } catch (_: Exception) {
                        0L
                    }
                    AppEntry(
                        packageName = ai.packageName,
                        activityName = ai.name,
                        label = ri.loadLabel(pm).toString(),
                        icon = IconRenderer.render(ri.loadIcon(pm), 168).asImageBitmap(),
                        category = Categorizer.categorize(ai.applicationInfo),
                        installTime = installTime
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    // ---------- Dock ----------

    private fun readList(key: String): List<String> =
        (prefs.getString(key, "") ?: "").split(",").filter { it.isNotBlank() }

    private fun saveDock(list: List<String>) {
        dock = list
        prefs.edit().putString("dock", list.joinToString(",")).apply()
    }

    private fun ensureDock() {
        if (apps.isEmpty()) return
        val valid = dock.filter { p -> apps.any { it.packageName == p } }
        if (prefs.contains("dock")) {
            if (valid != dock) saveDock(valid)
            return
        }
        // Pertama kali: pilih telepon, pesan, browser, kamera
        val picks = mutableListOf<String>()
        for (key in listOf("dialer", "messaging", "chrome", "camera")) {
            apps.firstOrNull { key in it.packageName && it.packageName !in picks }
                ?.let { picks += it.packageName }
        }
        for (a in apps) {
            if (picks.size >= 4) break
            if (a.packageName !in picks) picks += a.packageName
        }
        saveDock(picks.take(4))
    }

    fun toggleDock(pkg: String) {
        if (pkg in dock) {
            saveDock(dock - pkg)
        } else {
            val base = if (dock.size >= 4) dock.dropLast(1) else dock
            saveDock(base + pkg)
        }
    }

    // ---------- Pengaturan ----------

    fun toggleHidden(pkg: String) {
        val h = if (pkg in hidden) hidden - pkg else hidden + pkg
        hidden = h
        prefs.edit().putStringSet("hidden", h).apply()
    }

    fun restoreHidden() {
        hidden = emptySet()
        prefs.edit().remove("hidden").apply()
    }

    fun setSortedHome(value: Boolean) {
        sortedHome = value
        prefs.edit().putBoolean("sorted_home", value).apply()
    }

    fun setCustomStatusBar(value: Boolean) {
        customStatusBar = value
        prefs.edit().putBoolean("custom_status", value).apply()
    }

    fun selectWallpaper(index: Int) {
        wallpaper = index
        prefs.edit().putInt("wallpaper", index).apply()
    }

    fun updateNote(text: String) {
        note = text
        prefs.edit().putString("note", text).apply()
    }

    fun homePressed() {
        homeTick++
    }

    // ---------- Aksi aplikasi ----------

    private fun loadCounts(): Map<String, Int> =
        prefs.all
            .filterKeys { it.startsWith("cnt_") }
            .mapKeys { it.key.removePrefix("cnt_") }
            .mapValues { (it.value as? Int) ?: 0 }

    private fun countLaunch(pkg: String) {
        val n = (counts[pkg] ?: 0) + 1
        counts = counts + (pkg to n)
        prefs.edit().putInt("cnt_$pkg", n).apply()
    }

    fun launch(ctx: Context, app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        try {
            ctx.startActivity(intent)
            countLaunch(app.packageName)
        } catch (_: Exception) {
        }
    }

    fun showInfo(ctx: Context, pkg: String) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    fun uninstall(ctx: Context, pkg: String) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    fun openDefaultLauncherSettings(ctx: Context) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}
