package com.pdfcraft.studio.ui.home

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * fix38: Home "Favorite Design" list, shown above 1..30 in the Home Design picker.
 * Add a new favorite by appending it at the end (it becomes Favorite 2, 3, ...).
 */
internal val HOME_FAVORITE_DESIGNS: List<HomeDesign> = listOf(
    // Favorite 1 = old Home design 5 "Candy Circles" (fix37), kept exactly
    HomeDesign(
        name = "Candy Circles",
        layout = HomeLayout.CIRCLES,
        header = HomeHeader.CENTER,
        placement = HomePlacement.CENTER,
        bg = Color(0xFFFFF0F6),
        bgBrush = Brush.verticalGradient(listOf(Color(0xFFFFF0F6), Color(0xFFEEF4FF))),
        title = Color(0xFF3B2A4A),
        sub = Color(0xFF8E7AA0),
        colorCards = true,
        itemColors = listOf(Color(0xFFFF8FB1), Color(0xFF7FB8FF), Color(0xFFFFC75F), Color(0xFF6FD6A8)),
        iconFg = Color.White,
        elevation = 6.dp,
        shadowColor = Color(0xFFFF8FB1),
        accent = Color(0xFFFF6F9F),
        fab = Color(0xFFFF6F9F)
    )
)
