package com.pdfcraft.studio.core.image

sealed class ImageSizeOption {
    data object Default : ImageSizeOption()
    data class Preset(val kb: Int) : ImageSizeOption()
    data class Custom(val kb: Int) : ImageSizeOption()
    data class CustomBytes(val bytes: Int) : ImageSizeOption()

    val targetBytes: Int?
        get() = when (this) {
            is Default -> null
            is Preset -> kb * 1024
            is Custom -> kb * 1024
            is CustomBytes -> bytes
        }

    val label: String
        get() = when (this) {
            is Default -> "Keep Original Size"
            is Preset -> "$kb KB"
            is Custom -> "$kb KB"
            is CustomBytes -> "$bytes B"
        }

    companion object {
        val presetsKb = listOf(5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 250, 500, 750, 1000)
        val menuKb = listOf(5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
    }
}
