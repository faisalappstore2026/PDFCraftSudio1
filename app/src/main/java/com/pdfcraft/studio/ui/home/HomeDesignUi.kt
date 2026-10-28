package com.pdfcraft.studio.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.AppIcons

/** fix37: one Home option (same 4 options in every design). */
internal class HomeEntry(
    val title: String,
    val sub: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

internal fun Modifier.homeBackground(color: Color, brush: Brush?): Modifier =
    if (brush != null) this.background(brush) else this.background(color)

/** card look: shadow, background (per option color, gradient or plain), outline, click */
internal fun Modifier.homeCard(d: HomeDesign, shape: Shape, color: Color?, onClick: (() -> Unit)?): Modifier {
    var m = this
    if (d.elevation > 0.dp) {
        m = m.shadow(d.elevation, shape, clip = false, ambientColor = d.shadowColor, spotColor = d.shadowColor)
    }
    m = m.clip(shape)
    val brush = d.cardBrush
    m = if (color != null) m.background(color) else if (brush != null) m.background(brush) else m.background(d.card)
    val line = d.outline
    if (line != null) m = m.border(1.dp, line, shape)
    return if (onClick != null) m.clickable(onClick = onClick) else m
}

/** fix37: full Home screen drawn with one of the 30 designs. */
@Composable
internal fun HomeDesignedScreen(
    design: HomeDesign,
    logoSize: Float,
    onLogoSizeChange: (Float) -> Unit,
    onCreatePdfClick: () -> Unit,
    onViewPdf: () -> Unit,
    onMyProjectsClick: () -> Unit,
    onIconCenterClick: () -> Unit
) {
    val d = design
    val entries = listOf(
        HomeEntry(stringResource(R.string.create_pdf), stringResource(R.string.home_sub_create), Icons.Filled.Add, onCreatePdfClick),
        HomeEntry(stringResource(R.string.view_pdf), stringResource(R.string.home_sub_view), AppIcons.Visibility, onViewPdf),
        HomeEntry(stringResource(R.string.my_projects), stringResource(R.string.home_sub_projects), AppIcons.FileDocument, onMyProjectsClick),
        HomeEntry(stringResource(R.string.icon_center), stringResource(R.string.home_sub_icon), Icons.Filled.Settings, onIconCenterClick)
    )
    HomeStatusBarIcons(darkIcons = d.title.luminance() < 0.5f)
    val edgeless = d.layout == HomeLayout.SHEET || d.layout == HomeLayout.DOCK
    Box(modifier = Modifier.fillMaxSize().homeBackground(d.bg, d.bgBrush).homePattern(d)) { // fix40
        Scaffold(containerColor = Color.Transparent) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val screenH = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = screenH)
                        .padding(
                            start = if (edgeless) 0.dp else 24.dp,
                            end = if (edgeless) 0.dp else 24.dp,
                            top = 20.dp,
                            bottom = if (d.layout == HomeLayout.SHEET) 0.dp else 16.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = when (d.placement) {
                        HomePlacement.CENTER -> Arrangement.spacedBy(28.dp, Alignment.CenterVertically)
                        HomePlacement.TOP -> Arrangement.spacedBy(28.dp, Alignment.Top)
                        HomePlacement.SPREAD -> Arrangement.SpaceBetween
                    }
                ) {
                    HomeHeaderBlock(
                        d = d,
                        logoSize = logoSize,
                        showLogo = d.layout != HomeLayout.ORBIT && d.layout != HomeLayout.CIRCLE_ORBIT,
                        modifier = Modifier.padding(horizontal = if (edgeless) 24.dp else 0.dp)
                    )
                    HomeBody(d, entries, logoSize, onLogoSizeChange)
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            HomeDesignFloatingButton(fabColor = d.fab, fabFg = d.fabFg)
        }
    }
}

@Composable
internal fun HomeLogo(d: HomeDesign, size: Dp) {
    val shape: Shape = if (d.logoFrame == HomeLogoFrame.ROUNDED) RoundedCornerShape(size * 0.28f) else CircleShape
    val ring = d.logoFrame == HomeLogoFrame.RING
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .then(if (ring) Modifier.border(3.dp, d.logoRing, CircleShape).padding(7.dp) else Modifier)
            .clip(shape)
            .background(if (d.logoFrame == HomeLogoFrame.PLAIN) Color.Transparent else d.logoBg)
    ) {
        // fix38: new Circle.png icon with ICON_FIT (AppIconFit.kt)
        AppCircleIconImage()
    }
}

@Composable
private fun HomeHeaderBlock(d: HomeDesign, logoSize: Float, showLogo: Boolean, modifier: Modifier) {
    val appName = stringResource(R.string.app_name)
    val tagline = stringResource(R.string.home_tagline)
    when (d.header) {
        HomeHeader.CENTER -> Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showLogo) {
                HomeLogo(d, logoSize.dp)
                Spacer(modifier = Modifier.height(18.dp))
            }
            Text(
                text = appName,
                color = d.title,
                fontSize = d.titleSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tagline,
                color = d.sub,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }
        HomeHeader.LEFT -> Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            if (showLogo) {
                HomeLogo(d, (logoSize * 0.75f).dp)
                Spacer(modifier = Modifier.height(14.dp))
            }
            Text(
                text = appName,
                color = d.title,
                fontSize = (d.titleSize + 4).sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = (d.titleSize + 10).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = tagline, color = d.sub, fontSize = 15.sp, lineHeight = 22.sp)
        }
        HomeHeader.BAR -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showLogo) {
                HomeLogo(d, (logoSize * 0.5f).coerceAtLeast(40f).dp)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    color = d.title,
                    fontSize = (d.titleSize - 4).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = tagline, color = d.sub, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
            }
        }
    }
}

@Composable
internal fun HomeBadge(d: HomeDesign, i: Int, icon: ImageVector, size: Dp) {
    val bg = d.badgeColorFor(i)
    val shape: Shape = if (d.badge == HomeIconBadge.ROUNDED) RoundedCornerShape(size * 0.3f) else CircleShape
    val look = when (d.badge) {
        HomeIconBadge.CIRCLE, HomeIconBadge.ROUNDED -> Modifier.clip(shape).background(bg)
        HomeIconBadge.OUTLINE -> Modifier.border(1.5.dp, d.iconFg, CircleShape)
        HomeIconBadge.PLAIN -> Modifier
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size).then(look)) {
        Icon(imageVector = icon, contentDescription = null, tint = d.iconFg, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
internal fun EntryTexts(d: HomeDesign, e: HomeEntry, center: Boolean, titleSize: Int = 16, showSub: Boolean = d.showSub) {
    Text(
        text = e.title,
        color = d.cardFg,
        fontSize = titleSize.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    if (showSub) {
        Text(
            text = e.sub,
            color = d.cardSub,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** wide card: badge, title + subtitle, arrow */
@Composable
internal fun RowCard(d: HomeDesign, i: Int, e: HomeEntry, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(d.corner)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .homeCard(d, shape, d.cardColorFor(i), e.onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        HomeBadge(d, i, e.icon, 44.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            EntryTexts(d, e, center = false)
        }
        Text(text = "›", color = d.cardSub, fontSize = 26.sp)
    }
}

/** square-ish tile: badge on top, text below */
@Composable
internal fun TileCard(d: HomeDesign, i: Int, e: HomeEntry, modifier: Modifier, badgeSize: Dp = 52.dp, titleSize: Int = 15, showSub: Boolean = d.showSub) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .homeCard(d, RoundedCornerShape(d.corner), d.cardColorFor(i), e.onClick)
            .padding(10.dp)
    ) {
        HomeBadge(d, i, e.icon, badgeSize)
        Spacer(modifier = Modifier.height(10.dp))
        EntryTexts(d, e, center = true, titleSize = titleSize, showSub = showSub)
    }
}

@Composable
private fun LogoSizeSlider(d: HomeDesign, logoSize: Float, onLogoSizeChange: (Float) -> Unit, label: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.icon_size), color = label, fontSize = 13.sp)
        Slider(
            value = logoSize,
            onValueChange = onLogoSizeChange,
            valueRange = 64f..200f,
            steps = HOME_ICON_SIZE_STEPS, // fix40: points while sliding
            colors = SliderDefaults.colors(
                thumbColor = d.accent,
                activeTrackColor = d.accent,
                inactiveTrackColor = d.accent.copy(alpha = 0.25f),
                activeTickColor = if (d.accent.luminance() > 0.6f) Color(0xAA000000) else Color(0xCCFFFFFF),
                inactiveTickColor = d.accent.copy(alpha = 0.85f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HomeBody(d: HomeDesign, entries: List<HomeEntry>, logoSize: Float, onLogoSizeChange: (Float) -> Unit) {
    val gap = d.spacing
    when (d.layout) {
        HomeLayout.SHEET -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(d.sheet)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(d.cardSub.copy(alpha = 0.4f))
            )
            entries.forEachIndexed { i, e -> RowCard(d, i, e, Modifier.fillMaxWidth()) }
            LogoSizeSlider(d, logoSize, onLogoSizeChange, d.cardSub)
        }
        HomeLayout.DOCK -> Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                LogoSizeSlider(d, logoSize, onLogoSizeChange, d.sub)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .homeCard(d, RoundedCornerShape(d.corner), d.sheet, null)
                    .padding(vertical = 12.dp, horizontal = 4.dp)
            ) {
                entries.forEachIndexed { i, e ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = e.onClick)
                            .padding(vertical = 6.dp, horizontal = 2.dp)
                    ) {
                        HomeBadge(d, i, e.icon, 46.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = e.title,
                            color = d.cardFg,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
        else -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.fillMaxWidth()
        ) {
            HomeOptions(d, entries, logoSize)
            Spacer(modifier = Modifier.height(4.dp))
            LogoSizeSlider(d, logoSize, onLogoSizeChange, d.sub)
        }
    }
}
