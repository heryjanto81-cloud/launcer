package com.glasslauncher

import java.util.Locale

/** Teks sederhana dua bahasa: Indonesia kalau bahasa HP = Indonesia, selain itu Inggris. */
object S {
    private val isId: Boolean = Locale.getDefault().language.let { it == "in" || it == "id" }

    fun t(en: String, id: String): String = if (isId) id else en
}
