package com.glasslauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.roundToInt

/**
 * Menggambar ikon aplikasi jadi kotak penuh (tanpa masking bentuk bawaan HP),
 * supaya nanti bisa dipotong jadi "squircle" ala iOS.
 */
object IconRenderer {

    fun render(d: Drawable, px: Int): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFF2C2C2E.toInt())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && d is AdaptiveIconDrawable) {
            // Adaptive icon: 108dp penuh, area terlihat 72dp -> perbesar supaya mengisi kotak
            val full = (px * 108f / 72f).roundToInt()
            val off = (px - full) / 2
            d.background?.let {
                it.setBounds(off, off, off + full, off + full)
                it.draw(c)
            }
            d.foreground?.let {
                it.setBounds(off, off, off + full, off + full)
                it.draw(c)
            }
        } else {
            val inset = (px * 0.11f).roundToInt()
            d.setBounds(inset, inset, px - inset, px - inset)
            d.draw(c)
        }
        return bmp
    }
}
