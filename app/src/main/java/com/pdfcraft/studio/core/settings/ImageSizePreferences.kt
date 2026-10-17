package com.pdfcraft.studio.core.settings

import android.content.Context
import com.pdfcraft.studio.core.image.ImageSizeOption

class ImageSizePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedOption(): ImageSizeOption {
        val mode = prefs.getString(KEY_MODE, null)
        if (mode == "original") return ImageSizeOption.Default
        if (mode == "preset") {
            val kb = prefs.getInt(KEY_KB, 5)
            if (kb <= 0) return ImageSizeOption.Default
            return ImageSizeOption.Preset(kb)
        }
        if (mode == "custom_kb") {
            val kb = prefs.getInt(KEY_KB, 1)
            if (kb <= 0) return ImageSizeOption.Default
            return ImageSizeOption.Custom(kb)
        }
        if (mode == "custom_bytes") {
            val bytes = prefs.getInt(KEY_BYTES, 1024)
            val kb = if (bytes <= 0) 1 else (bytes + 1023) / 1024
            return ImageSizeOption.Custom(kb)
        }
        val kb = prefs.getInt(KEY_KB, -1)
        if (kb <= 0) return ImageSizeOption.Default
        val isCustom = prefs.getBoolean(KEY_IS_CUSTOM, false)
        return if (isCustom) ImageSizeOption.Custom(kb) else ImageSizeOption.Preset(kb)
    }

    fun saveOption(option: ImageSizeOption) {
        prefs.edit().apply {
            when (option) {
                is ImageSizeOption.Default -> {
                    putInt(KEY_KB, -1)
                    putBoolean(KEY_IS_CUSTOM, false)
                    putString(KEY_MODE, "original")
                }
                is ImageSizeOption.Preset -> {
                    putInt(KEY_KB, option.kb)
                    putBoolean(KEY_IS_CUSTOM, false)
                    putString(KEY_MODE, "preset")
                }
                is ImageSizeOption.Custom -> {
                    putInt(KEY_KB, option.kb)
                    putBoolean(KEY_IS_CUSTOM, true)
                    putString(KEY_MODE, "custom_kb")
                }
            }
        }.apply()
    }

    fun getPageBackgroundColor(): Long {
        val v = prefs.getLong(KEY_PAGE_BG_COLOR, DEFAULT_PAGE_BG_COLOR)
        return if ((v.toInt() and 0xFFFFFF) == 0xE0E0E0) 0xFFFFFFFFL else v
    }

    fun savePageBackgroundColor(colorArgb: Long) {
        prefs.edit().putLong(KEY_PAGE_BG_COLOR, colorArgb).apply()
    }

    fun getNumberingBg(): Long = prefs.getLong(KEY_NUM_BG, 0xFF7C4DFFL)
    fun getNumberingFg(): Long = prefs.getLong(KEY_NUM_FG, 0xFFFFFFFFL)
    fun getNumberingWeight(): Float = prefs.getFloat(KEY_NUM_WEIGHT, 0.67f)
    fun getNumberingAlpha(): Float = prefs.getFloat(KEY_NUM_ALPHA, 0.9f)
    fun getNumberingSize(): Float = prefs.getFloat(KEY_NUM_SIZE, 0.18f)

    fun saveNumberingStyle(bg: Long, fg: Long, weight: Float, alpha: Float, size: Float) {
        prefs.edit()
            .putLong(KEY_NUM_BG, bg)
            .putLong(KEY_NUM_FG, fg)
            .putFloat(KEY_NUM_WEIGHT, weight)
            .putFloat(KEY_NUM_ALPHA, alpha)
            .putFloat(KEY_NUM_SIZE, size)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "pdfcraft_settings"
        const val KEY_KB = "selected_image_size_kb"
        const val KEY_IS_CUSTOM = "selected_image_size_is_custom"
        const val KEY_MODE = "selected_image_size_mode"
        const val KEY_BYTES = "selected_image_size_bytes"
        const val KEY_PAGE_BG_COLOR = "page_background_color"
        const val KEY_NUM_BG = "numbering_bg"
        const val KEY_NUM_FG = "numbering_fg"
        const val KEY_NUM_WEIGHT = "numbering_weight"
        const val KEY_NUM_ALPHA = "numbering_alpha"
        const val KEY_NUM_SIZE = "numbering_size"
        // 3rd swatch default (medium light gray)
        const val DEFAULT_PAGE_BG_COLOR = 0xFFFFFFFFL
    }
}

