package com.pdfcraft.studio.core.image

sealed class ImageSizeOption {
    data object Default : ImageSizeOption()
    data class Preset(val kb: Int) : ImageSizeOption()
    data class Custom(val kb: Int) : ImageSizeOption()

    val targetBytes: Int?
        get() = when (this) {
            is Default -> null
            is Preset -> kb * 1024
            is Custom -> kb * 1024
        }

    val label: String
        get() = when (this) {
            is Default -> "Keep Original Size"
            is Preset -> formatKb(kb)
            is Custom -> formatKb(kb)
        }

    companion object {
        val presetsKb = listOf(5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 250, 500, 750, 1000)
        val menuKb = listOf(
            5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100,
            250, 500, 750,
            1024, 2048, 5120, 10240, 20480, 51200, 102400
        )

        fun formatKb(kb: Int): String {
            if (kb >= 1024) {
                if (kb % 1024 == 0) return (kb / 1024).toString() + " MB"
            }
            return kb.toString() + " KB"
        }
    }
}
