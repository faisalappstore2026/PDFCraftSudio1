package com.pdfcraft.studio.ui.home

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * fix43: default Home design (saved code 0, the Default button in the Home Design picker)
 * = Home Favorite 5 "Sorbet List". A getter, so the list below is ready when it is read.
 */
internal val HOME_DEFAULT_DESIGN: HomeDesign
    get() = HOME_FAVORITE_DESIGNS[4]

/**
 * fix38: Home "Favorite Design" list, shown above 1..30 in the Home Design picker.
 * Add a new favorite by appending it at the end (it becomes the next Favorite number).
 * Every entry is a full copy, so it never changes when the numbered designs change.
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
    ),
    // Favorite 2 = fix38 Home design 11 "Neon Dock" (fix40), kept exactly
    HomeDesign(
        name = "Neon Dock",
        layout = HomeLayout.CIRCLE_DOCK,
        header = HomeHeader.CENTER,
        placement = HomePlacement.SPREAD,
        bg = Color(0xFF0A0A0F),
        bgBrush = Brush.verticalGradient(listOf(Color(0xFF0A0A0F), Color(0xFF1A1A2E))),
        title = Color.White,
        sub = Color(0xFF9FA3B8),
        logoFrame = HomeLogoFrame.RING,
        logoRing = Color(0xFF00F5D4),
        cardFg = Color(0xFFE6E6F0),
        corner = 30.dp,
        outline = Color(0xFF2E2E45),
        iconFg = Color(0xFF0A0A0F),
        itemColors = listOf(Color(0xFF00F5D4), Color(0xFFF15BB5), Color(0xFFFEE440), Color(0xFF9B5DE5)),
        colorCards = true,
        accent = Color(0xFF00F5D4),
        sheet = Color(0xFF1C1C2B),
        fab = Color(0xFF00F5D4),
        fabFg = Color(0xFF0A0A0F),
        circle = 54.dp,
        circleStyle = HomeCircleStyle.HALO
    ),
    // Favorite 3 = fix38 Home design 12 "Rainbow List" (fix40), kept exactly
    HomeDesign(
        name = "Rainbow List",
        layout = HomeLayout.CIRCLE_LIST,
        header = HomeHeader.BAR,
        placement = HomePlacement.TOP,
        bg = Color(0xFFF7F7FB),
        title = Color(0xFF1F1F2E),
        sub = Color(0xFF80809A),
        card = Color.White,
        cardFg = Color(0xFF1F1F2E),
        cardSub = Color(0xFF80809A),
        corner = 22.dp,
        elevation = 4.dp,
        shadowColor = Color(0xFFB0B0C8),
        iconFg = Color.White,
        itemColors = listOf(Color(0xFFFF4D6D), Color(0xFFFF9F1C), Color(0xFF2EC4B6), Color(0xFF5E60CE)),
        colorCards = true,
        accent = Color(0xFF5E60CE),
        fab = Color(0xFF5E60CE),
        spacing = 12.dp,
        circle = 54.dp
    ),
    // Favorite 4 = fix38 Home design 21 "Milk Dock" (fix40), kept exactly
    HomeDesign(
        name = "Milk Dock",
        layout = HomeLayout.CIRCLE_DOCK,
        header = HomeHeader.CENTER,
        placement = HomePlacement.CENTER,
        bg = Color(0xFFF5F5F5),
        title = Color(0xFF222222),
        sub = Color(0xFF8A8A8A),
        logoFrame = HomeLogoFrame.CIRCLE,
        logoBg = Color.White,
        cardFg = Color(0xFF333333),
        corner = 24.dp,
        elevation = 6.dp,
        shadowColor = Color(0xFF9E9E9E),
        iconFg = Color.White,
        itemColors = listOf(Color(0xFFE4293B), Color(0xFF424242), Color(0xFF757575), Color(0xFFE4293B)),
        colorCards = true,
        accent = Color(0xFFE4293B),
        sheet = Color.White,
        fab = Color(0xFF222222),
        fabFg = Color.White,
        circle = 56.dp
    ),
    // Favorite 5 = fix38 Home design 22 "Sorbet List" (fix40), kept exactly
    HomeDesign(
        name = "Sorbet List",
        layout = HomeLayout.CIRCLE_LIST,
        header = HomeHeader.CENTER,
        placement = HomePlacement.CENTER,
        bg = Color(0xFFFFF4EC),
        bgBrush = Brush.verticalGradient(listOf(Color(0xFFFFE9F2), Color(0xFFEAF6FF))),
        title = Color(0xFF3F2B4F),
        sub = Color(0xFF8D7A9E),
        card = Color(0x99FFFFFF),
        cardFg = Color(0xFF3F2B4F),
        cardSub = Color(0xFF8D7A9E),
        corner = 34.dp,
        outline = Color(0x339C7FB5),
        itemColors = listOf(Color(0xFFFF6B9A), Color(0xFFFF9F45), Color(0xFF33B5A5), Color(0xFF7A6CFF)),
        colorCards = true,
        accent = Color(0xFFFF6B9A),
        fab = Color(0xFFFF6B9A),
        spacing = 10.dp,
        circle = 56.dp,
        circleStyle = HomeCircleStyle.SOFT
    ),
    // Favorite 6 = fix38 Home design 28 "Jelly Panel" (fix40), kept exactly
    HomeDesign(
        name = "Jelly Panel",
        layout = HomeLayout.CIRCLE_PANEL,
        header = HomeHeader.LEFT,
        placement = HomePlacement.CENTER,
        bg = Color(0xFF6C2BD9),
        bgBrush = Brush.verticalGradient(listOf(Color(0xFF7B2FF7), Color(0xFFC246D6))),
        title = Color.White,
        sub = Color(0xFFF0DDFF),
        logoFrame = HomeLogoFrame.CIRCLE,
        cardFg = Color(0xFF3B1670),
        corner = 28.dp,
        elevation = 5.dp,
        shadowColor = Color(0xFF3B1670),
        iconFg = Color.White,
        itemColors = listOf(Color(0xFF7B2FF7), Color(0xFFF72585), Color(0xFF4CC9F0), Color(0xFFFF9E00)),
        colorCards = true,
        accent = Color.White,
        sheet = Color(0xFFF7F0FF),
        fab = Color.White,
        fabFg = Color(0xFF7B2FF7),
        spacing = 12.dp,
        circle = 78.dp
    ),
    // Favorite 7 = fix38 Home design 30 "Twilight List" (fix40), kept exactly
    HomeDesign(
        name = "Twilight List",
        layout = HomeLayout.CIRCLE_LIST,
        header = HomeHeader.LEFT,
        placement = HomePlacement.TOP,
        bg = Color(0xFF1E1B4B),
        bgBrush = Brush.verticalGradient(listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95))),
        title = Color.White,
        sub = Color(0xFFC4B5FD),
        logoFrame = HomeLogoFrame.RING,
        logoRing = Color(0xFFF0ABFC),
        card = Color(0x26FFFFFF),
        cardFg = Color.White,
        cardSub = Color(0xFFDDD6FE),
        corner = 40.dp,
        outline = Color(0x33FFFFFF),
        iconFg = Color(0xFF1E1B4B),
        itemColors = listOf(Color(0xFFF0ABFC), Color(0xFF93C5FD), Color(0xFFFCD34D), Color(0xFF86EFAC)),
        colorCards = true,
        accent = Color(0xFFF0ABFC),
        fab = Color(0xFFF0ABFC),
        fabFg = Color(0xFF1E1B4B),
        spacing = 12.dp,
        circle = 52.dp,
        circleStyle = HomeCircleStyle.HALO
    )
)
