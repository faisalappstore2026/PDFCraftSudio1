package com.pdfcraft.studio.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * fix41: defaults of the Home top image (res/drawable-nodpi/home_top_image.jpg = Grok1.jpg).
 * scale 1.00 = full width of the Home content, offsetX positive = right, offsetY positive = down (dp).
 * Send the "Copy Details" text to change these defaults.
 */
object HomeTopImageFit {
    const val SCALE = 1.00f
    const val OFFSET_X = 0
    const val OFFSET_Y = 0
    const val MIN_SCALE = 0.20f
    const val MAX_SCALE = 2.00f
    /** 0.20..2.00 with a point every 0.05 */
    const val SCALE_STEPS = 35
    /** dp moved by one Up / Down / Left / Right press */
    const val MOVE_STEP = 4
}

/** fix41: saved Home top image size and position (SharedPreferences "home_top_image"). */
internal object HomeTopImageState {
    private const val PREFS = "home_top_image"
    private var loaded = false
    var scale by mutableStateOf(HomeTopImageFit.SCALE)
        private set
    var offsetX by mutableStateOf(HomeTopImageFit.OFFSET_X)
        private set
    var offsetY by mutableStateOf(HomeTopImageFit.OFFSET_Y)
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        scale = p.getFloat("scale", HomeTopImageFit.SCALE)
            .coerceIn(HomeTopImageFit.MIN_SCALE, HomeTopImageFit.MAX_SCALE)
        offsetX = p.getInt("offset_x", HomeTopImageFit.OFFSET_X)
        offsetY = p.getInt("offset_y", HomeTopImageFit.OFFSET_Y)
    }

    private fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("scale", scale)
            .putInt("offset_x", offsetX)
            .putInt("offset_y", offsetY)
            .apply()
    }

    fun changeScale(context: Context, value: Float) {
        scale = ((value * 100f).roundToInt() / 100f)
            .coerceIn(HomeTopImageFit.MIN_SCALE, HomeTopImageFit.MAX_SCALE)
        save(context)
    }

    fun move(context: Context, dx: Int, dy: Int) {
        offsetX = (offsetX + dx).coerceIn(-600, 600)
        offsetY = (offsetY + dy).coerceIn(-600, 600)
        save(context)
    }

    fun details(): String =
        String.format(Locale.US, "HOME_TOP_IMAGE scale=%.2f offsetX=%d offsetY=%d", scale, offsetX, offsetY)
}

/** fix41: the Home top image with the saved size and position. */
@Composable
internal fun HomeTopImage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    HomeTopImageState.ensureLoaded(context)
    val s = HomeTopImageState.scale
    val ox = HomeTopImageState.offsetX
    val oy = HomeTopImageState.offsetY
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.home_top_image),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth(s.coerceAtMost(1f))
                .graphicsLayer {
                    val extra = if (s > 1f) s else 1f
                    scaleX = extra
                    scaleY = extra
                    translationX = ox.dp.toPx()
                    translationY = oy.dp.toPx()
                }
        )
    }
}

@Composable
private fun TopImageButton(text: String, accent: Color, label: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .background(accent.copy(alpha = 0.14f), shape)
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Text(text = text, color = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** fix41: top image size slider, Up / Down / Left / Right buttons and Copy Details (below the icon size slider). */
@Composable
internal fun HomeTopImageControls(accent: Color, label: Color) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    HomeTopImageState.ensureLoaded(context)
    val step = HomeTopImageFit.MOVE_STEP
    val copiedMsg = stringResource(R.string.home_top_image_copied)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.home_top_image_size), color = label, fontSize = 13.sp)
        Slider(
            value = HomeTopImageState.scale,
            onValueChange = { HomeTopImageState.changeScale(context, it) },
            valueRange = HomeTopImageFit.MIN_SCALE..HomeTopImageFit.MAX_SCALE,
            steps = HomeTopImageFit.SCALE_STEPS,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.25f),
                activeTickColor = if (accent.luminance() > 0.6f) Color(0xAA000000) else Color(0xCCFFFFFF),
                inactiveTickColor = accent.copy(alpha = 0.85f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopImageButton(stringResource(R.string.home_top_image_up), accent, label, Modifier.weight(1f)) {
                HomeTopImageState.move(context, 0, -step)
            }
            TopImageButton(stringResource(R.string.home_top_image_down), accent, label, Modifier.weight(1f)) {
                HomeTopImageState.move(context, 0, step)
            }
            TopImageButton(stringResource(R.string.home_top_image_left), accent, label, Modifier.weight(1f)) {
                HomeTopImageState.move(context, -step, 0)
            }
            TopImageButton(stringResource(R.string.home_top_image_right), accent, label, Modifier.weight(1f)) {
                HomeTopImageState.move(context, step, 0)
            }
        }
        TopImageButton(stringResource(R.string.home_top_image_copy), accent, label, Modifier.fillMaxWidth()) {
            clipboard.setText(AnnotatedString(HomeTopImageState.details()))
            Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
        }
    }
}
