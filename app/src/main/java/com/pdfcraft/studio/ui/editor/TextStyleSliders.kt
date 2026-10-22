package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Card shown at the top of the canvas after Border Color OK. */
@Composable
fun TextBorderSliderPanel(
    widthDp: Float,
    onWidthChange: (Float) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    SliderCard(title = "Border", onDone = onDone, modifier = modifier) {
        LabeledSlider(
            label = "Thickness",
            valueText = ((widthDp * 10f).roundToInt() / 10f).toString(),
            value = widthDp,
            range = 0.5f..8f,
            onChange = onWidthChange
        )
    }
}

/** Card shown at the top of the canvas after Background Color OK. */
@Composable
fun TextBgSliderPanel(
    cornerDp: Float,
    alpha: Float,
    onCornerChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    SliderCard(title = "Background", onDone = onDone, modifier = modifier) {
        LabeledSlider(
            label = "Corner radius",
            valueText = cornerDp.roundToInt().toString(),
            value = cornerDp,
            range = 0f..24f,
            onChange = onCornerChange
        )
        LabeledSlider(
            label = "Opacity",
            valueText = (alpha * 100f).roundToInt().toString() + "%",
            value = alpha,
            range = 0.05f..1f,
            onChange = onAlphaChange
        )
    }
}

@Composable
private fun SliderCard(
    title: String,
    onDone: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .background(Color.White, RoundedCornerShape(14.dp))
            // Swallow taps so the canvas does not deselect the text box.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            TextButton(onClick = onDone) { Text("Done") }
        }
        content()
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.DarkGray, fontSize = 13.sp)
        Text(valueText, color = Color.DarkGray, fontSize = 13.sp)
    }
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        valueRange = range
    )
}
