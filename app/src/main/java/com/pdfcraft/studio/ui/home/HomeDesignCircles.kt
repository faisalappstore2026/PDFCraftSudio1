package com.pdfcraft.studio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * fix38: circle family layouts for Home designs 1..30, all made from the
 * Candy Circles reference (round option buttons with the label under them).
 * Every layout always shows all 4 options: Create PDF, View PDF, My Projects, Icon Center.
 */
private val CIRCLE_FAMILY = setOf(
    HomeLayout.CIRCLE_GRID, HomeLayout.CIRCLE_ROW, HomeLayout.CIRCLE_LIST,
    HomeLayout.CIRCLE_DIAMOND, HomeLayout.CIRCLE_STAGGER, HomeLayout.CIRCLE_HERO,
    HomeLayout.CIRCLE_ORBIT, HomeLayout.CIRCLE_BUBBLES, HomeLayout.CIRCLE_PANEL,
    HomeLayout.CIRCLE_DOCK
)

internal fun HomeLayout.isCircleFamily(): Boolean = this in CIRCLE_FAMILY

private fun HomeDesign.circleColorFor(i: Int): Color = cardColorFor(i) ?: card

/** the round button itself: filled, ring, halo or soft tint */
@Composable
private fun CircleFace(d: HomeDesign, i: Int, e: HomeEntry, size: Dp) {
    val color = d.circleColorFor(i)
    val look: Modifier = when (d.circleStyle) {
        HomeCircleStyle.FILLED -> Modifier.homeCard(d, CircleShape, color, null)
        HomeCircleStyle.HALO -> Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.24f))
            .padding(size * 0.11f)
            .homeCard(d, CircleShape, color, null)
        HomeCircleStyle.RING -> Modifier
            .clip(CircleShape)
            .background(d.card)
            .border(3.dp, color, CircleShape)
        HomeCircleStyle.SOFT -> Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f))
    }
    val tint: Color = when (d.circleStyle) {
        HomeCircleStyle.FILLED, HomeCircleStyle.HALO -> d.iconFg
        HomeCircleStyle.RING, HomeCircleStyle.SOFT -> color
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size).then(look)) {
        Icon(imageVector = e.icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.43f))
    }
}

/** circle + label under it, the whole column is the click target */
@Composable
private fun CircleItem(
    d: HomeDesign,
    i: Int,
    e: HomeEntry,
    size: Dp,
    modifier: Modifier = Modifier.width(size + 40.dp),
    labelSize: Int = 14,
    labelColor: Color = d.label ?: d.title
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = e.onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        CircleFace(d, i, e, size)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = e.title,
            color = labelColor,
            fontSize = labelSize.sp,
            lineHeight = (labelSize + 3).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CircleGrid(d: HomeDesign, entries: List<HomeEntry>, labelColor: Color) {
    entries.chunked(2).forEachIndexed { r, pair ->
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            pair.forEachIndexed { c, e ->
                CircleItem(d, r * 2 + c, e, d.circle, labelColor = labelColor)
            }
        }
    }
}

@Composable
internal fun HomeCircleOptions(d: HomeDesign, entries: List<HomeEntry>, logoSize: Float) {
    val c = d.circle
    val labelColor = d.label ?: d.title
    when (d.layout) {
        HomeLayout.CIRCLE_ROW -> Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            entries.forEachIndexed { i, e ->
                CircleItem(d, i, e, c, Modifier.weight(1f), labelSize = 12)
            }
        }
        HomeLayout.CIRCLE_LIST -> entries.forEachIndexed { i, e ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .homeCard(d, RoundedCornerShape(d.corner), null, e.onClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                CircleFace(d, i, e, c)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    EntryTexts(d, e, center = false)
                }
            }
        }
        HomeLayout.CIRCLE_DIAMOND -> {
            CircleItem(d, 0, entries[0], c)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                CircleItem(d, 1, entries[1], c)
                CircleItem(d, 2, entries[2], c)
            }
            CircleItem(d, 3, entries[3], c)
        }
        HomeLayout.CIRCLE_STAGGER -> Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(d.spacing)) {
                CircleItem(d, 0, entries[0], c)
                CircleItem(d, 2, entries[2], c)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(d.spacing),
                modifier = Modifier.padding(top = c * 0.6f)
            ) {
                CircleItem(d, 1, entries[1], c)
                CircleItem(d, 3, entries[3], c)
            }
        }
        HomeLayout.CIRCLE_HERO -> {
            CircleItem(d, 0, entries[0], c * 1.45f, Modifier.width(c * 1.45f + 40.dp), labelSize = 17)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1 until entries.size) {
                    CircleItem(d, i, entries[i], c * 0.72f, Modifier.weight(1f), labelSize = 12)
                }
            }
        }
        HomeLayout.CIRCLE_ORBIT -> {
            CircleItem(d, 0, entries[0], c, Modifier.width(c + 28.dp), labelSize = 12)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CircleItem(d, 1, entries[1], c, Modifier.width(c + 28.dp), labelSize = 12)
                HomeLogo(d, logoSize.coerceIn(64f, 100f).dp)
                CircleItem(d, 2, entries[2], c, Modifier.width(c + 28.dp), labelSize = 12)
            }
            CircleItem(d, 3, entries[3], c, Modifier.width(c + 28.dp), labelSize = 12)
        }
        HomeLayout.CIRCLE_BUBBLES -> {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircleItem(d, 0, entries[0], c * 1.2f)
                CircleItem(d, 1, entries[1], c * 0.85f)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircleItem(d, 2, entries[2], c * 0.85f)
                CircleItem(d, 3, entries[3], c * 1.2f)
            }
        }
        HomeLayout.CIRCLE_PANEL -> Column(
            verticalArrangement = Arrangement.spacedBy(d.spacing),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .homeCard(d, RoundedCornerShape(d.corner + 8.dp), d.sheet, null)
                .padding(vertical = 18.dp, horizontal = 6.dp)
        ) {
            CircleGrid(d, entries, d.cardFg)
        }
        HomeLayout.CIRCLE_DOCK -> Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .homeCard(d, RoundedCornerShape(d.corner), d.sheet, null)
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            entries.forEachIndexed { i, e ->
                CircleItem(d, i, e, c, Modifier.weight(1f), labelSize = 11, labelColor = d.cardFg)
            }
        }
        // CIRCLE_GRID (and any other value): 2 x 2 circles like Candy Circles
        else -> CircleGrid(d, entries, labelColor)
    }
}
