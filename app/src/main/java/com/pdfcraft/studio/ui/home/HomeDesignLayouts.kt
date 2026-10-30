package com.pdfcraft.studio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** fix37: the 12 option arrangements (layout kinds) used by the 30 Home designs. */
@Composable
internal fun HomeOptions(d: HomeDesign, entries: List<HomeEntry>, logoSize: Float) {
    // fix38: circle family layouts (Home designs 1..30, HomeDesignCircles.kt)
    if (d.layout.isCircleFamily()) {
        HomeCircleOptions(d, entries, logoSize)
        return
    }
    val gap = d.spacing
    when (d.layout) {
        HomeLayout.LIST -> entries.forEachIndexed { i, e ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(58.dp)
                    .homeCard(d, RoundedCornerShape(d.corner), d.cardColorFor(i), e.onClick)
                    .padding(horizontal = 18.dp)
            ) {
                HomeBadge(d, i, e.icon, 34.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = e.title, color = d.cardFg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        HomeLayout.ROWS -> entries.forEachIndexed { i, e -> RowCard(d, i, e, Modifier.fillMaxWidth()) }
        HomeLayout.GRID2 -> entries.chunked(2).forEachIndexed { r, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { c, e ->
                    TileCard(d, r * 2 + c, e, Modifier.weight(1f).aspectRatio(1f))
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f)) // fix54: same tile size
            }
        }
        HomeLayout.HERO -> {
            val hero = entries[0]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .homeCard(d, RoundedCornerShape(d.corner + 6.dp), d.cardColorFor(0), hero.onClick)
                    .padding(20.dp)
            ) {
                HomeBadge(d, 0, hero.icon, 60.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    EntryTexts(d, hero, center = false, titleSize = 21)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
                for (i in 1 until entries.size) {
                    TileCard(d, i, entries[i], Modifier.weight(1f).aspectRatio(0.82f), badgeSize = 40.dp, titleSize = 13, showSub = false)
                }
            }
        }
        HomeLayout.CIRCLES -> entries.chunked(2).forEachIndexed { r, pair ->
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { c, e ->
                    val i = r * 2 + c
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(128.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = e.onClick)
                            .padding(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(88.dp)
                                .homeCard(d, CircleShape, d.cardColorFor(i), e.onClick)
                        ) {
                            Icon(imageVector = e.icon, contentDescription = null, tint = d.iconFg, modifier = Modifier.size(38.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = e.title,
                            color = d.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }
        HomeLayout.BENTO -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier.fillMaxWidth().height(230.dp)
            ) {
                val e0 = entries[0]
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .homeCard(d, RoundedCornerShape(d.corner), d.cardColorFor(0), e0.onClick)
                        .padding(16.dp)
                ) {
                    HomeBadge(d, 0, e0.icon, 58.dp)
                    Column {
                        EntryTexts(d, e0, center = false, titleSize = 20)
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    for (i in 1..2) {
                        val e = entries[i]
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .homeCard(d, RoundedCornerShape(d.corner), d.cardColorFor(i), e.onClick)
                                .padding(12.dp)
                        ) {
                            HomeBadge(d, i, e.icon, 36.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = e.title, color = d.cardFg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        }
                    }
                }
            }
            RowCard(d, 3, entries[3], Modifier.fillMaxWidth())
            for (i in 4 until entries.size) RowCard(d, i, entries[i], Modifier.fillMaxWidth()) // fix54
        }
        HomeLayout.ZIGZAG -> entries.forEachIndexed { i, e ->
            val left = i % 2 == 0
            val big = d.corner + 8.dp
            val small = 6.dp
            Box(
                contentAlignment = if (left) Alignment.CenterStart else Alignment.CenterEnd,
                modifier = Modifier.fillMaxWidth()
            ) {
                RowCard(
                    d, i, e,
                    Modifier.fillMaxWidth(0.84f),
                    if (left) RoundedCornerShape(topStart = big, topEnd = small, bottomEnd = big, bottomStart = small)
                    else RoundedCornerShape(topStart = small, topEnd = big, bottomEnd = small, bottomStart = big)
                )
            }
        }
        HomeLayout.TIMELINE -> Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { i, e ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(30.dp)) {
                        Spacer(modifier = Modifier.height(31.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(d.accent)
                        )
                        if (i < entries.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(51.dp)
                                    .background(d.accent.copy(alpha = 0.4f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    RowCard(d, i, e, Modifier.weight(1f).padding(bottom = 12.dp).height(76.dp))
                }
            }
        }
        HomeLayout.CHIPS -> entries.chunked(2).forEachIndexed { r, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { c, e ->
                    val i = r * 2 + c
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .homeCard(d, RoundedCornerShape(26.dp), d.cardColorFor(i), e.onClick)
                            .padding(horizontal = 10.dp)
                    ) {
                        Icon(imageVector = e.icon, contentDescription = null, tint = d.iconFg, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = e.title,
                            color = d.cardFg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f)) // fix54: same chip size
            }
        }
        HomeLayout.ORBIT -> {
            val tile = 90.dp
            OrbitTile(d, 0, entries[0], tile)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OrbitTile(d, 1, entries[1], tile)
                HomeLogo(d, logoSize.coerceAtMost(104f).dp)
                OrbitTile(d, 2, entries[2], tile)
            }
            OrbitTile(d, 3, entries[3], tile)
            for (i in 4 until entries.size) OrbitTile(d, i, entries[i], tile) // fix54
        }
        // SHEET and DOCK are drawn in HomeBody
        else -> entries.forEachIndexed { i, e -> RowCard(d, i, e, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun OrbitTile(d: HomeDesign, i: Int, e: HomeEntry, size: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(size)
            .homeCard(d, RoundedCornerShape(d.corner), d.cardColorFor(i), e.onClick)
            .padding(6.dp)
    ) {
        HomeBadge(d, i, e.icon, 38.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = e.title,
            color = d.cardFg,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
