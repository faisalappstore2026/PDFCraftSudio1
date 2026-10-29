package com.pdfcraft.studio.ui.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * fix40: logo size slider points. 64..200 dp with 16 steps = a point every 8 dp,
 * shown on the track while sliding and the thumb snaps to them.
 */
internal const val HOME_ICON_SIZE_STEPS = 16

/** fix41: default logo size = point 9 from the left (64 + 8 x 8 = 128 dp). */
internal const val HOME_ICON_SIZE_DEFAULT = 128f

/** fix40: futuristic background pattern of a Home design (grid, dots, scan lines, radar, circuit). */
internal fun Modifier.homePattern(d: HomeDesign): Modifier {
    if (d.pattern == HomeBgPattern.NONE) return this
    val color = d.patternColor
    return this.drawBehind {
        val step = max(d.patternStep.toPx(), 4f)
        val line = max(1.dp.toPx(), 1f)
        when (d.pattern) {
            HomeBgPattern.NONE -> Unit
            HomeBgPattern.GRID -> {
                var x = 0f
                while (x <= size.width) {
                    drawLine(color, Offset(x, 0f), Offset(x, size.height), line)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(color, Offset(0f, y), Offset(size.width, y), line)
                    y += step
                }
            }
            HomeBgPattern.DOTS -> {
                val r = max(step * 0.07f, 1.5f)
                var y = step / 2f
                var row = 0
                while (y <= size.height) {
                    var x = if (row % 2 == 0) step / 2f else step
                    while (x <= size.width) {
                        drawCircle(color, r, Offset(x, y))
                        x += step
                    }
                    y += step
                    row++
                }
            }
            HomeBgPattern.SCANLINES -> {
                var y = 0f
                while (y <= size.height) {
                    drawLine(color, Offset(0f, y), Offset(size.width, y), line)
                    y += step
                }
            }
            HomeBgPattern.RADAR -> {
                val c = Offset(size.width / 2f, size.height * 0.42f)
                val far = max(size.width, size.height)
                var r = step
                while (r <= far) {
                    drawCircle(color, r, c, style = Stroke(width = line))
                    r += step
                }
                drawLine(color, Offset(c.x, 0f), Offset(c.x, size.height), line)
                drawLine(color, Offset(0f, c.y), Offset(size.width, c.y), line)
            }
            HomeBgPattern.CIRCUIT -> {
                val dot = max(step * 0.09f, 2f)
                var y = step / 2f
                var row = 0
                while (y <= size.height) {
                    var x = step / 2f
                    var col = 0
                    while (x <= size.width) {
                        when ((row * 3 + col * 7) % 5) {
                            0 -> {
                                drawLine(color, Offset(x, y), Offset(x + step, y), line)
                                drawCircle(color, dot, Offset(x + step, y))
                            }
                            1 -> {
                                drawLine(color, Offset(x, y), Offset(x, y + step), line)
                                drawCircle(color, dot, Offset(x, y + step))
                            }
                            2 -> {
                                drawLine(color, Offset(x, y), Offset(x + step / 2f, y), line)
                                drawLine(color, Offset(x + step / 2f, y), Offset(x + step, y + step / 2f), line)
                                drawCircle(color, dot, Offset(x, y))
                            }
                            else -> Unit
                        }
                        x += step
                        col++
                    }
                    y += step
                    row++
                }
            }
        }
    }
}
