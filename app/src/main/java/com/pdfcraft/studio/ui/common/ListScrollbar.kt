package com.pdfcraft.studio.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BtnFill = Color(0xFF7C3AED)
private val BtnFillDrag = Color(0xFF5B21B6)

private class ScrollBtnMetrics {
    var maxScroll = 1f
    var stride = 1f
    var total = 1
    var trackPx = 1f
}

@Composable
fun VerticalScrollButton(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val info = listState.layoutInfo
    val visible = info.visibleItemsInfo
    val total = info.totalItemsCount
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat().coerceAtLeast(1f)
    val spacing = info.mainAxisItemSpacing.toFloat()
    val measuredAvg = if (visible.isEmpty()) 1f
    else visible.map { it.size }.average().toFloat().coerceAtLeast(1f)
    val stride = measuredAvg + spacing

    val first = visible.firstOrNull()
    val last = visible.lastOrNull()
    val scrollPx = if (first == null) 0f
    else (first.index * stride - first.offset).coerceAtLeast(0f)
    val below = if (last == null) 0f
    else {
        val after = (total - 1 - last.index).coerceAtLeast(0)
        (last.offset + last.size - viewport).coerceAtLeast(0f) + after * stride
    }
    val contentH = scrollPx + viewport + below
    val maxScroll = (contentH - viewport).coerceAtLeast(0f)
    val canScroll = maxScroll > 2f && total > 0 && visible.isNotEmpty() &&
        (listState.canScrollForward || listState.canScrollBackward || scrollPx > 1f)
    val computedFrac = if (maxScroll <= 0f) 0f else (scrollPx / maxScroll).coerceIn(0f, 1f)

    var dragging by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    val scrolling = listState.isScrollInProgress

    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            shown = true
        } else {
            delay(1000)
            shown = false
        }
    }

    if (!canScroll || (!shown && !scrolling && !dragging)) return

    val btnH = 64.dp
    val shape = RoundedCornerShape(10.dp)
    val widthAnim by animateDpAsState(if (dragging) 20.dp else 16.dp, label = "scrollBtnW")
    val shadowAnim by animateDpAsState(if (dragging) 8.dp else 3.dp, label = "scrollBtnSh")
    val bg = if (dragging) BtnFillDrag else BtnFill
    val metrics = remember { ScrollBtnMetrics() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .zIndex(8f)
    ) {
        val density = LocalDensity.current
        val btnHpx = with(density) { btnH.toPx() }
        val edgePx = with(density) { 8.dp.toPx() }
        val trackPx = (with(density) { maxHeight.toPx() } - btnHpx - edgePx * 2f).coerceAtLeast(1f)
        metrics.maxScroll = maxScroll
        metrics.stride = stride
        metrics.total = total
        metrics.trackPx = trackPx

        var yPx by remember { mutableStateOf(computedFrac * trackPx) }
        LaunchedEffect(computedFrac, trackPx, dragging) {
            if (!dragging) yPx = computedFrac * trackPx
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 5.dp)
                .offset { IntOffset(0, (edgePx + yPx).roundToInt()) }
                .width(widthAnim)
                .height(btnH)
                .alpha(0.95f)
                .shadow(shadowAnim, shape)
                .clip(shape)
                .border(1.5.dp, Color.White, shape)
                .background(bg)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        dragging = true
                        shown = true
                        drag(down.id) { change ->
                            change.consume()
                            val dy = change.position.y - change.previousPosition.y
                            val track = metrics.trackPx.coerceAtLeast(1f)
                            yPx = (yPx + dy).coerceIn(0f, track)
                            val frac = (yPx / track).coerceIn(0f, 1f)
                            val target = frac * metrics.maxScroll
                            val st = metrics.stride.coerceAtLeast(1f)
                            val idx = (target / st).toInt()
                                .coerceIn(0, (metrics.total - 1).coerceAtLeast(0))
                            val off = (target - idx * st).toInt().coerceAtLeast(0)
                            scope.launch { listState.scrollToItem(idx, off) }
                        }
                        dragging = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.width(widthAnim).height(btnH)) {
                val cx = size.width / 2f
                val s = 3.5.dp.toPx()
                val upY = size.height * 0.36f
                val downY = size.height * 0.64f
                val up = Path().apply {
                    moveTo(cx, upY - s)
                    lineTo(cx - s, upY + s * 0.55f)
                    lineTo(cx + s, upY + s * 0.55f)
                    close()
                }
                val down = Path().apply {
                    moveTo(cx, downY + s)
                    lineTo(cx - s, downY - s * 0.55f)
                    lineTo(cx + s, downY - s * 0.55f)
                    close()
                }
                drawPath(up, Color.White)
                drawPath(down, Color.White)
            }
        }
    }
}

@Composable
fun DocumentScrollBar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    VerticalScrollButton(listState = listState, modifier = modifier)
}
