package com.pdfcraft.studio.ui.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pdfcraft.studio.ui.theme.BrandRed

/**
 * fix37: Home screen design system.
 * fix40: code 0 = HOME_DEFAULT_DESIGN (old fix38 design 12 "Rainbow List", HomeDesignFavorites.kt).
 * Designs 1..30 = HOME_DESIGNS (HomeDesignPresets.kt + HomeDesignPresets2.kt), futuristic / tech looks.
 * Favorites = HOME_FAVORITE_DESIGNS (full copies, so they never change when 1..30 change).
 * Every design shows the same 4 options (Create PDF, View PDF, My Projects, Icon Center)
 * plus the logo size slider; only look and arrangement change.
 */
enum class HomeLayout {
    LIST, GRID2, HERO, ROWS, SHEET, CIRCLES, DOCK, BENTO, ZIGZAG, TIMELINE, CHIPS, ORBIT,
    // fix38: circle family (HomeDesignCircles.kt)
    CIRCLE_GRID, CIRCLE_ROW, CIRCLE_LIST, CIRCLE_DIAMOND, CIRCLE_STAGGER,
    CIRCLE_HERO, CIRCLE_ORBIT, CIRCLE_BUBBLES, CIRCLE_PANEL, CIRCLE_DOCK
}

/** fix38: how one option circle is painted. fix40: + NEON (glow ring) and GLASS (frosted). */
enum class HomeCircleStyle { FILLED, RING, HALO, SOFT, NEON, GLASS }

/** fix40: tech pattern drawn over the screen background (HomeDesignTech.kt). */
enum class HomeBgPattern { NONE, GRID, DOTS, SCANLINES, RADAR, CIRCUIT }

/** Logo + title + tagline: centered column, left aligned column, or one compact row. */
enum class HomeHeader { CENTER, LEFT, BAR }

/** Vertical placement: everything centered, everything at the top, or header top + options bottom. */
enum class HomePlacement { CENTER, TOP, SPREAD }

enum class HomeLogoFrame { CIRCLE, ROUNDED, RING, PLAIN }

enum class HomeIconBadge { CIRCLE, ROUNDED, OUTLINE, PLAIN }

data class HomeDesign(
    val name: String,
    val layout: HomeLayout,
    val header: HomeHeader = HomeHeader.CENTER,
    val placement: HomePlacement = HomePlacement.CENTER,
    // screen background
    val bg: Color = Color.White,
    val bgBrush: Brush? = null,
    val title: Color = Color(0xFF1F1F1F),
    val sub: Color = Color(0xFF8A8A8A),
    val titleSize: Int = 26,
    // logo
    val logoFrame: HomeLogoFrame = HomeLogoFrame.CIRCLE,
    val logoBg: Color = Color.White,
    val logoRing: Color = Color(0x22000000),
    // option cards
    val card: Color = Color.White,
    val cardBrush: Brush? = null,
    val cardFg: Color = Color(0xFF1F1F1F),
    val cardSub: Color = Color(0xFF8A8A8A),
    val corner: Dp = 20.dp,
    val outline: Color? = null,
    val elevation: Dp = 0.dp,
    val shadowColor: Color = Color.Black,
    // icon badge inside a card
    val badge: HomeIconBadge = HomeIconBadge.CIRCLE,
    val iconBg: Color = Color(0x1AE4293B),
    val iconFg: Color = BrandRed,
    /** Optional per option colors (badge background, or card background when colorCards = true). */
    val itemColors: List<Color> = emptyList(),
    val colorCards: Boolean = false,
    // slider, timeline line, hero accents
    val accent: Color = BrandRed,
    // panel behind options for SHEET and DOCK layouts
    val sheet: Color = Color.White,
    // floating design button
    val fab: Color = BrandRed,
    val fabFg: Color = Color.White,
    val spacing: Dp = 14.dp,
    val showSub: Boolean = true,
    // fix38: circle family
    val circle: Dp = 88.dp,
    val circleStyle: HomeCircleStyle = HomeCircleStyle.FILLED,
    /** label color under the circles (null = title color) */
    val label: Color? = null,
    // fix40: futuristic background pattern (NONE = plain background, like before)
    val pattern: HomeBgPattern = HomeBgPattern.NONE,
    val patternColor: Color = Color(0x22FFFFFF),
    val patternStep: Dp = 28.dp
)

val HOME_DESIGNS: List<HomeDesign> by lazy { HOME_DESIGNS_1 + HOME_DESIGNS_2 }

internal fun HomeDesign.cardColorFor(i: Int): Color? =
    if (colorCards && itemColors.isNotEmpty()) itemColors[i % itemColors.size] else null

internal fun HomeDesign.badgeColorFor(i: Int): Color =
    if (!colorCards && itemColors.isNotEmpty()) itemColors[i % itemColors.size] else iconBg

/**
 * fix37: chosen Home design and the floating button position.
 * Saved code 0 = default design, 1..30 = numbered design, FAVORITE_BASE + n = Favorite n.
 * fix40: new key design_v3, because numbers 1..30 are new designs now. A fix38 save is
 * moved once: favorites stay, old 12 = the new default, old 11 / 21 / 22 / 28 / 30 = the
 * favorites made from them, every other old number falls back to the default.
 */
object HomeDesignState {
    private const val PREFS = "pdfcraft_home_design"
    private const val KEY_OLD_DESIGN = "design"
    private const val KEY_V2_DESIGN = "design_v2"
    private const val KEY_DESIGN = "design_v3"
    private const val KEY_FAB_X = "fab_x"
    private const val KEY_FAB_Y = "fab_y"
    const val FAVORITE_BASE = 1000
    var design by mutableIntStateOf(0)
        private set
    /** 0..1 of the free width / height, so the button always stays on screen. */
    var fabX by mutableFloatStateOf(1f)
        private set
    var fabY by mutableFloatStateOf(0.62f)
        private set
    private var loaded = false

    fun favoriteCode(n: Int): Int = FAVORITE_BASE + n

    /** Favorite number (1, 2, ...) of a saved code, or 0 when it is not a favorite. */
    fun favoriteNumber(code: Int): Int {
        val n = code - FAVORITE_BASE
        return if (n in 1..HOME_FAVORITE_DESIGNS.size) n else 0
    }

    fun isValid(code: Int): Boolean =
        code == 0 || code in 1..HOME_DESIGNS.size || favoriteNumber(code) > 0

    /** fix40: fix38 code -> fix40 code (see the class comment). */
    private fun fromV2(code: Int): Int = when (code) {
        12 -> 0
        11 -> favoriteCode(2)
        21 -> favoriteCode(4)
        22 -> favoriteCode(5)
        28 -> favoriteCode(6)
        30 -> favoriteCode(7)
        else -> if (code > FAVORITE_BASE) code else 0
    }

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var migrated = false
        val saved = try {
            when {
                p.contains(KEY_DESIGN) -> p.getInt(KEY_DESIGN, 0)
                p.contains(KEY_V2_DESIGN) -> {
                    migrated = true
                    fromV2(p.getInt(KEY_V2_DESIGN, 0))
                }
                p.getInt(KEY_OLD_DESIGN, 0) == 5 -> {
                    migrated = true
                    favoriteCode(1)
                }
                else -> 0
            }
        } catch (e: ClassCastException) {
            0
        }
        design = if (isValid(saved)) saved else 0
        if (migrated) p.edit().putInt(KEY_DESIGN, design).apply()
        fabX = p.getFloat(KEY_FAB_X, 1f).coerceIn(0f, 1f)
        fabY = p.getFloat(KEY_FAB_Y, 0.62f).coerceIn(0f, 1f)
    }

    /** fix40: never null, code 0 = HOME_DEFAULT_DESIGN. */
    fun active(): HomeDesign {
        val code = design
        val fav = favoriteNumber(code)
        return when {
            code in 1..HOME_DESIGNS.size -> HOME_DESIGNS[code - 1]
            fav > 0 -> HOME_FAVORITE_DESIGNS[fav - 1]
            else -> HOME_DEFAULT_DESIGN
        }
    }

    fun select(context: Context, code: Int) {
        if (!isValid(code)) return
        loaded = true
        design = code
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DESIGN, code).apply()
    }

    fun moveFab(x: Float, y: Float) {
        fabX = x.coerceIn(0f, 1f)
        fabY = y.coerceIn(0f, 1f)
    }

    fun saveFab(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_FAB_X, fabX).putFloat(KEY_FAB_Y, fabY).apply()
    }
}
