package com.glasslauncher

import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap

enum class AppCategory(private val en: String, private val idn: String) {
    SOCIAL("Social", "Sosial"),
    GAMES("Games", "Game"),
    CREATIVITY("Photo & Video", "Foto & Video"),
    ENTERTAINMENT("Entertainment", "Hiburan"),
    PRODUCTIVITY("Productivity", "Produktivitas"),
    FINANCE("Shopping & Finance", "Belanja & Keuangan"),
    TRAVEL("Travel", "Perjalanan"),
    GOOGLE("Google", "Google"),
    UTILITIES("Utilities", "Utilitas"),
    OTHER("Other", "Lainnya");

    val title: String get() = S.t(en, idn)
}

data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap,
    val category: AppCategory,
    val installTime: Long
)

data class LibraryGroup(val title: String, val apps: List<AppEntry>)

sealed interface HomeItem {
    data class App(val app: AppEntry) : HomeItem
    data class Folder(val group: LibraryGroup) : HomeItem
}

/**
 * Mengelompokkan aplikasi otomatis: game, foto/video, Google, sosial, dst.
 * Urutan aturan penting: yang lebih spesifik dicek lebih dulu.
 */
object Categorizer {

    private val rules: List<Pair<AppCategory, List<String>>> = listOf(
        AppCategory.GAMES to listOf(
            "supercell", "mojang", "garena", "roblox", "mihoyo", "activision", "moonton",
            "pubg", "minecraft", "freefire", "mobilelegends", ".games", ".game.", "com.king.",
            "com.ea.", "gameloft", "tencent.ig", "miniclip", "rovio", "zynga"
        ),
        AppCategory.CREATIVITY to listOf(
            "camera", "gallery", "photos", "photo", "snapseed", "capcut", "inshot", "picsart",
            "lightroom", "vsco", "videoeditor", "editor.video"
        ),
        AppCategory.ENTERTAINMENT to listOf(
            "youtube", "netflix", "spotify", "music", "video", "vlc", "disney", "hotstar", "vidio",
            "podcast", "tiktok", "twitch", "soundcloud", "joox", "player", "radio", "wetv", "iqiyi",
            "viu", "bilibili"
        ),
        AppCategory.SOCIAL to listOf(
            "whatsapp", "telegram", "facebook", "instagram", "twitter", "discord", "snapchat",
            "linkedin", "reddit", "signal", "jp.naver.line", "messenger", "messaging", "mms",
            "threads", "wechat", "kakao", "tinder", "bumble", "pinterest", "tumblr"
        ),
        AppCategory.FINANCE to listOf(
            "bank", "gopay", "id.ovo", "ovo.id", "dana", "shopee", "tokopedia", "lazada", "paypal",
            "wallet", "blibli", "bca", "mandiri", "bni", "brimo", "amazon", "aliexpress",
            "bukalapak", "jenius", "linkaja", "akulaku", "kredivo", "seabank", "jago", "finance",
            "payment"
        ),
        AppCategory.TRAVEL to listOf(
            "maps", "gojek", "grab", "uber", "traveloka", "booking", "airbnb", "waze", "tiket",
            "navigation", "transit"
        ),
        AppCategory.PRODUCTIVITY to listOf(
            "docs", "sheets", "slides", "keep", "calendar", "drive", "office", "notion", "slack",
            "zoom", "teams", "outlook", "gmail", "android.gm", "tachyon", "onenote", "excel",
            "powerpoint", "pdf", "notes", "todoist", "trello", "dropbox", "evernote", "email"
        ),
        AppCategory.UTILITIES to listOf(
            "calculator", "clock", "settings", "filemanager", "files", "documentsui", "contacts",
            "dialer", "phone", "flashlight", "torch", "weather", "security", "cleaner", "vpn",
            "packageinstaller", "recorder", "compass", "telecom", "backup"
        )
    )

    @Suppress("DEPRECATION")
    fun categorize(info: ApplicationInfo): AppCategory {
        val pkg = info.packageName.lowercase()

        if ((info.flags and ApplicationInfo.FLAG_IS_GAME) != 0) return AppCategory.GAMES

        for ((category, keys) in rules) {
            if (keys.any { pkg.contains(it) }) return category
        }

        when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_NEWS -> return AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.CREATIVITY
            ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_MAPS -> return AppCategory.TRAVEL
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
        }

        if (pkg.startsWith("com.google.") || pkg == "com.android.chrome" || pkg == "com.android.vending") {
            return AppCategory.GOOGLE
        }
        if (pkg.startsWith("com.android.") || pkg.startsWith("com.miui.") ||
            pkg.startsWith("com.samsung.") || pkg.startsWith("com.sec.") ||
            pkg.startsWith("android")
        ) {
            return AppCategory.UTILITIES
        }
        return AppCategory.OTHER
    }
}

/** Isi halaman Home. Kalau [sorted] aktif: aplikasi sejenis otomatis dijadikan satu folder. */
fun buildHomeItems(
    apps: List<AppEntry>,
    dock: Set<String>,
    hidden: Set<String>,
    sorted: Boolean
): List<HomeItem> {
    val visible = apps.filter { it.packageName !in dock && it.packageName !in hidden }
    if (!sorted) return visible.map { HomeItem.App(it) }

    val grouped = visible.groupBy { it.category }
    val items = mutableListOf<HomeItem>()
    for (category in AppCategory.entries) {
        val list = grouped[category] ?: continue
        if (list.size >= 2) {
            items += HomeItem.Folder(LibraryGroup(category.title, list))
        } else {
            items += list.map { HomeItem.App(it) }
        }
    }
    return items
}

/** Isi App Library: Suggestions, Recently Added, lalu semua kategori. */
fun buildLibrary(apps: List<AppEntry>, counts: Map<String, Int>): List<LibraryGroup> {
    val out = mutableListOf<LibraryGroup>()
    val suggestions = apps.sortedByDescending { counts[it.packageName] ?: 0 }.take(8)
    if (suggestions.isNotEmpty()) out += LibraryGroup(S.t("Suggestions", "Saran"), suggestions)
    val recent = apps.sortedByDescending { it.installTime }.take(8)
    if (recent.isNotEmpty()) out += LibraryGroup(S.t("Recently Added", "Baru Ditambahkan"), recent)
    val grouped = apps.groupBy { it.category }
    for (category in AppCategory.entries) {
        grouped[category]?.let { out += LibraryGroup(category.title, it) }
    }
    return out
}
