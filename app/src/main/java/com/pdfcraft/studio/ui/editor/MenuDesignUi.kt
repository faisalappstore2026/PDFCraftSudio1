package com.pdfcraft.studio.ui.editor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat

/** fix35/fix36: UI pieces that draw the editor with the active design. fix40: window = Favorite Design. */
@Composable
internal fun currentMenuDesign(): MenuDesign {
    val ctx = LocalContext.current
    remember { MenuDesignState.ensureLoaded(ctx.applicationContext) }
    return MenuDesignState.active()
}

/** fix40: look of the dropdown lists (Menu Design 1..30, or the window design lists). */
@Composable
internal fun currentDropdownDesign(): DropdownDesign {
    val ctx = LocalContext.current
    remember { MenuDesignState.ensureLoaded(ctx.applicationContext) }
    return MenuDesignState.activeDropdown()
}

/** fix36: solid color or gradient background, used for top bar / tools row / pages area. */
internal fun Modifier.designBackground(color: Color, brush: Brush?): Modifier =
    if (brush != null) this.background(brush) else this.background(color)

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** fix36: dark status bar icons on light top bars, light icons on dark top bars. */
@Composable
internal fun EditorStatusBarIcons(design: MenuDesign) {
    val view = LocalView.current
    val darkIcons = design.topBarFg.luminance() < 0.5f
    DisposableEffect(view, darkIcons) {
        val window = if (view.isInEditMode) null else view.context.findActivity()?.window
        val myToken = com.pdfcraft.studio.ui.home.StatusBarIconsOwner.claim() // fix37
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        }
        onDispose {
            // the rest of the app uses light bars with dark icons (MainActivity);
            // fix37: skip when a newer screen (Home) already set the icons
            if (window != null && com.pdfcraft.studio.ui.home.StatusBarIconsOwner.isOwner(myToken)) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }
}

internal fun DropdownDesign.iconArgb(): Int = accent.toArgb()

/** Material3 1.2 DropdownMenu has no container params: theme its Surface instead. fix40: dropdown design. */
@Composable
internal fun DesignedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    properties: PopupProperties = PopupProperties(focusable = true),
    menuId: Int = 0, // fix44: DropdownPlacement id (0 = not moved / scaled)
    content: @Composable ColumnScope.() -> Unit
) {
    val d = currentDropdownDesign()
    DropdownPlacement.ensureLoaded(androidx.compose.ui.platform.LocalContext.current) // fix44
    DropdownBorderAnim.ensureLoaded(androidx.compose.ui.platform.LocalContext.current) // fix43
    val scheme = MaterialTheme.colorScheme.copy(surface = d.container, surfaceTint = d.container)
    val shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(d.corner))
    MaterialTheme(colorScheme = scheme, shapes = shapes, typography = MaterialTheme.typography) {
        val brush = d.containerBrush
        val outline = d.outline
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .then(if (brush != null) Modifier.background(brush) else Modifier)
                .then(if (outline != null) Modifier.border(d.outlineWidth, outline, RoundedCornerShape(d.corner)) else Modifier)
                .then(
                    // fix52: dots menu = its own animation only, static lines only on the three tool lists
                    if (menuId == DropdownPlacement.DOTS) Modifier.dropdownBorderAnim(DropdownBorderAnim.dotsChoice, d.corner)
                    else Modifier.editorBorderLine(DropdownBorderAnim.choice, d.corner, page = false)
                ), // fix43, fix48, fix51
            offset = dropdownOffset(menuId), // fix44
            properties = properties,
            content = { DropdownScaledContent(menuId, content) }
        )
    }
}

@Composable
internal fun MenuItemFrame(
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val d = currentDropdownDesign()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val shape = RoundedCornerShape(d.itemCorner)
    val itemOutline = d.itemOutline
    val itemBrush = d.itemBrush
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.itemMarginH, vertical = d.itemMarginV)
            .clip(shape)
            .then(
                if (!pressed && itemBrush != null) Modifier.background(itemBrush, shape)
                else Modifier.background(if (pressed) d.pressed else d.itemBg, shape)
            )
            .then(if (itemOutline != null) Modifier.border(1.dp, itemOutline, shape) else Modifier)
            .clickable(
                interactionSource = src,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = d.itemPadH, vertical = d.itemPadV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (d.accentBar) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(if (enabled) d.accent else d.subText, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        content()
    }
}

@Composable
internal fun MenuItemText(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false
) {
    val d = currentDropdownDesign()
    Text(
        text = text,
        color = if (!enabled || secondary) d.subText else d.text,
        fontSize = if (secondary) (d.fontSize.value - 1f).sp else d.fontSize,
        fontWeight = if (secondary) FontWeight.Normal else d.fontWeight,
        lineHeight = (d.fontSize.value + 4f).sp,
        letterSpacing = d.letterSpacing,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}

@Composable
internal fun MenuDesignDivider() {
    val d = currentDropdownDesign()
    when (d.divider) {
        MenuDividerStyle.NONE -> Unit
        MenuDividerStyle.FULL -> HorizontalDivider(thickness = 0.5.dp, color = d.dividerColor)
        MenuDividerStyle.INSET -> HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = d.dividerColor
        )
    }
}

@Composable
internal fun MenuDesignHeader(text: String, expanded: Boolean, onClick: () -> Unit) {
    val d = currentMenuDesign()
    ToolArrowSize.ensureLoaded(LocalContext.current) // fix48
    val shape = RoundedCornerShape(d.headerCorner)
    val openBrush = d.headerBrushOpen
    val bgMod = if (expanded && openBrush != null) Modifier.background(openBrush, shape)
    else Modifier.background(if (expanded) d.headerBgOpen else d.headerBg, shape)
    val outline = if (expanded) d.headerOutlineOpen else d.headerOutline
    val fg = if (expanded) d.headerFgOpen else d.headerFg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(bgMod)
            .then(if (outline != null) Modifier.border(1.dp, outline, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = d.headerWeight,
            maxLines = 1
        )
        Box(modifier = Modifier.width(15.dp), contentAlignment = Alignment.Center) { // fix50, fix52: fixed spot for 1.5x
            Text(
                text = if (expanded) "▴" else "▾",
                color = fg,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
                // fix49, fix50: scaled around its own center, size and layout unchanged
                modifier = Modifier.graphicsLayer {
                    scaleX = ToolArrowSize.scale
                    scaleY = ToolArrowSize.scale
                    transformOrigin = TransformOrigin.Center
                },
                fontWeight = d.headerWeight
            )
        }
    }
}

