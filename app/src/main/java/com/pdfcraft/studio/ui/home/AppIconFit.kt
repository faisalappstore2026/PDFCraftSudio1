package com.pdfcraft.studio.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pdfcraft.studio.R

/**
 * fix38: ICON_FIT of the new Circle.png icon (from Icon Center), used by the app icon
 * (res/drawable/ic_launcher_circle.xml) and by the Home window icon.
 * offsetX positive = right, offsetY positive = down (part of the round icon size).
 * scale 1.00 = the full adaptive icon canvas (108dp) = 1.5 x the visible round icon,
 * the same meaning as the Icon Center preview.
 */
object AppIconFit {
    const val OFFSET_X = 0.00f
    const val OFFSET_Y = 0.00f
    const val SCALE = 0.65f
    const val CANVAS_PER_CIRCLE = 1.5f
}

/** fix38: the new app icon inside a round frame (fills the frame, then ICON_FIT is applied). */
@Composable
internal fun AppCircleIconImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.app_circle_icon),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val shown = AppIconFit.CANVAS_PER_CIRCLE * AppIconFit.SCALE
                scaleX = shown
                scaleY = shown
                translationX = AppIconFit.OFFSET_X * size.width
                translationY = AppIconFit.OFFSET_Y * size.height
            }
    )
}
