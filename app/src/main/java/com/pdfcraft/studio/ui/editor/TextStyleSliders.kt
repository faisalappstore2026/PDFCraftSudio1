package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.pow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.pdfcraft.studio.R
import kotlin.math.roundToInt

/** Card shown at the top of the canvas after Border Color OK. */
@Composable
fun TextBorderSliderPanel(
    widthDp: Float,
    onWidthChange: (Float) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    SliderCard(onBack = onBack, onDone = onDone, modifier = modifier) {
        LabeledSlider(
            label = stringResource(R.string.tt_thickness),
            valueText = ((widthDp * 10f).roundToInt() / 10f).toString(),
            value = widthDp,
            range = 0.5f..8f,
            onChange = onWidthChange
        )
    }
}

/** Card shown at the top of the canvas after Spread Color OK. */
@Composable
fun TextSpreadSliderPanel(
    spreadDp: Float,
    onSpreadChange: (Float) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    SliderCard(onBack = onBack, onDone = onDone, modifier = modifier) {
        LabeledSlider(
            label = stringResource(R.string.tt_spread),
            valueText = spreadDp.roundToInt().toString(),
            value = spreadDp.coerceIn(0f, 40f),
            range = 0f..40f,
            onChange = onSpreadChange
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
    padLeft: Float = 0f,
    padRight: Float = 0f,
    padTop: Float = 0f,
    padBottom: Float = 0f,
    onPadLeftChange: (Float) -> Unit = {},
    onPadRightChange: (Float) -> Unit = {},
    onPadTopChange: (Float) -> Unit = {},
    onPadBottomChange: (Float) -> Unit = {},
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var linkLR by rememberSaveable { mutableStateOf(true) }
    var linkTB by rememberSaveable { mutableStateOf(true) }
    SliderCard(onBack = onBack, onDone = onDone, modifier = modifier) {
        LabeledSlider(
            label = stringResource(R.string.tt_corner_radius),
            valueText = cornerDp.roundToInt().toString(),
            value = cornerDp,
            range = 0f..24f,
            onChange = onCornerChange
        )
        LabeledSlider(
            label = stringResource(R.string.tt_opacity),
            valueText = (alpha * 100f).roundToInt().toString() + "%",
            value = alpha,
            range = 0.05f..1f,
            onChange = onAlphaChange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PadSlider(
                stringResource(R.string.tt_pad_left),
                padLeft,
                { v ->
                    onPadLeftChange(v)
                    if (linkLR) onPadRightChange(v)
                },
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            PadSlider(
                stringResource(R.string.tt_pad_right),
                padRight,
                { v ->
                    onPadRightChange(v)
                    if (linkLR) onPadLeftChange(v)
                },
                Modifier.weight(1f)
            )
            Checkbox(
                checked = linkLR,
                onCheckedChange = { linkLR = it },
                modifier = Modifier.size(48.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PadSlider(
                stringResource(R.string.tt_pad_top),
                padTop,
                { v ->
                    onPadTopChange(v)
                    if (linkTB) onPadBottomChange(v)
                },
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            PadSlider(
                stringResource(R.string.tt_pad_bottom),
                padBottom,
                { v ->
                    onPadBottomChange(v)
                    if (linkTB) onPadTopChange(v)
                },
                Modifier.weight(1f)
            )
            Checkbox(
                checked = linkTB,
                onCheckedChange = { linkTB = it },
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/** Compact 0..40 slider with its number, used for background padding. */
@Composable
private fun PadSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.DarkGray, fontSize = 13.sp)
            Text(value.roundToInt().toString(), color = Color.DarkGray, fontSize = 13.sp)
        }
        Slider(
            value = value.coerceIn(0f, 40f),
            onValueChange = {
                TextSliderDrag.active = true
                onChange(it.roundToInt().toFloat())
            },
            onValueChangeFinished = { TextSliderDrag.active = false },
            valueRange = 0f..40f
        )
    }
}

@Composable
private fun SliderCard(
    onBack: () -> Unit,
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
            TextButton(
                onClick = {
                    TextSliderDrag.active = false
                    onBack()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.back))
            }
            TextButton(onClick = {
                TextSliderDrag.active = false
                onDone()
            }) { Text(stringResource(R.string.editor_done)) }
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
        onValueChange = {
            TextSliderDrag.active = true
            onChange(it)
        },
        onValueChangeFinished = { TextSliderDrag.active = false },
        valueRange = range
    )
}

/** fix30: true while a finger drags a text slider; the canvas hides the box border + handles. */
internal object TextSliderDrag {
    var active by mutableStateOf(false)
}

private const val TEXT_BOX_MIN_SP = 6f
private const val TEXT_BOX_MAX_SP = 96f
private const val TEXT_MOVE_STEP_DP = 4f

/** Log scale: equal slider travel = equal zoom factor (6sp..96sp = 16x range). */
private fun textBoxSpToT(sp: Float): Float =
    (ln(sp.coerceIn(TEXT_BOX_MIN_SP, TEXT_BOX_MAX_SP) / TEXT_BOX_MIN_SP) / ln(TEXT_BOX_MAX_SP / TEXT_BOX_MIN_SP))
        .coerceIn(0f, 1f)

private fun textBoxTToSp(t: Float): Float =
    TEXT_BOX_MIN_SP * (TEXT_BOX_MAX_SP / TEXT_BOX_MIN_SP).pow(t.coerceIn(0f, 1f))

/** fix30 Text Box Size: text + box scale together; Back / Done like the other cards. */
@Composable
fun TextBoxSizeSliderPanel(
    sizeSp: Float,
    onSizeChange: (Float) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    SliderCard(onBack = onBack, onDone = onDone, modifier = modifier) {
        val safeSp = sizeSp.coerceIn(TEXT_BOX_MIN_SP, TEXT_BOX_MAX_SP)
        LabeledSlider(
            label = stringResource(R.string.text_tool_text_size_title),
            valueText = safeSp.roundToInt().toString(),
            value = textBoxSpToT(safeSp),
            range = 0f..1f,
            onChange = { onSizeChange(textBoxTToSp(it)) }
        )
    }
}

/** fix30 Text Move: Up / Down / Left / Right; hold a button to repeat. */
@Composable
fun TextMovePanel(
    onMove: (Float, Float) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    SliderCard(onBack = onBack, onDone = onDone, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextMoveButton("\u25C0", stringResource(R.string.text_move_left)) { onMove(-TEXT_MOVE_STEP_DP, 0f) }
            TextMoveButton("\u25B2", stringResource(R.string.text_move_up)) { onMove(0f, -TEXT_MOVE_STEP_DP) }
            TextMoveButton("\u25BC", stringResource(R.string.text_move_down)) { onMove(0f, TEXT_MOVE_STEP_DP) }
            TextMoveButton("\u25B6", stringResource(R.string.text_move_right)) { onMove(TEXT_MOVE_STEP_DP, 0f) }
        }
    }
}

@Composable
private fun TextMoveButton(symbol: String, desc: String, onStep: () -> Unit) {
    val step = rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(2.dp)
            .size(52.dp)
            .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .semantics {
                contentDescription = desc
                role = Role.Button
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val job = scope.launch {
                            step.value()
                            delay(400)
                            while (true) {
                                step.value()
                                delay(60)
                            }
                        }
                        tryAwaitRelease()
                        job.cancel()
                    }
                )
            }
    ) {
        Text(symbol, color = Color.Black, fontSize = 20.sp)
    }
}
