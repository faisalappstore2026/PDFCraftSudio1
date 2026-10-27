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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
private fun TextMoveButton(symbol: String, desc: String, boxDp: Int = 52, onStep: () -> Unit) {
    val step = rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(2.dp)
            .size(boxDp.dp)
            .background(Color(0xFFEEEEEE), RoundedCornerShape(if (boxDp < 48) 10.dp else 12.dp))
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
        Text(symbol, color = Color.Black, fontSize = if (boxDp < 48) 16.sp else 20.sp)
    }
}

/** fix33: keep a leading minus and up to 3 digits; no decimals. */
private fun filterDegInput(raw: String): String {
    val neg = raw.trimStart().startsWith("-")
    val digits = raw.filter { it.isDigit() }.take(3)
    return (if (neg) "-" else "") + digits
}

/** fix33: stored 0..360 to shown -180..180. */
private fun shownRotation(deg: Float): Int {
    if (deg.isNaN()) return 0
    val n = ((deg % 360f) + 360f) % 360f
    return (if (n > 180f) n - 360f else n).roundToInt().coerceIn(-180, 180)
}

/** fix33: small tick marks + labels under a slider (t = 0..1 along the track). */
@Composable
private fun SliderTicks(marks: List<Pair<Float, String>>) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(15.dp)
    ) {
        val inset = 10.dp
        val track = maxWidth - inset * 2
        marks.forEach { (t, label) ->
            val x = inset + track * t.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .offset(x = x - 0.5.dp)
                    .width(1.dp)
                    .height(4.dp)
                    .background(Color.Gray)
            )
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(x = x - 18.dp, y = 4.dp)
                    .width(36.dp)
            )
        }
    }
}

/** fix33: compact slider row (48dp touch area drawn in 34dp). */
@Composable
private fun CompactSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onEnd: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = {
                TextSliderDrag.active = true
                onChange(it)
            },
            onValueChangeFinished = {
                TextSliderDrag.active = false
                onEnd()
            },
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * fix33 Move+Rotate+Size: one compact card.
 * Arrows (Left, Right, Up, Down; hold to repeat), Size slider with % ticks,
 * Rotate slider -180..180 (0 in the middle) + whole-number input.
 */
@Composable
fun TextMoveRotateSizePanel(
    textId: String,
    sizeSp: Float,
    rotationDeg: Float,
    onMove: (Float, Float) -> Unit,
    onSizeChange: (Float) -> Unit,
    onSizeEnd: () -> Unit,
    onRotate: (Float) -> Unit,
    onRotateEnd: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = LocalFocusManager.current
    val safeSp = sizeSp.coerceIn(TEXT_BOX_MIN_SP, TEXT_BOX_MAX_SP)
    val baseSp = remember(textId) { safeSp }
    val rotInt = shownRotation(rotationDeg)
    var rotText by remember(textId) { mutableStateOf(rotInt.toString()) }
    LaunchedEffect(rotInt) {
        if (rotText.toIntOrNull() != rotInt) rotText = rotInt.toString()
    }
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            // Swallow taps so the canvas does not deselect the text box.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    TextSliderDrag.active = false
                    focus.clearFocus()
                    onBack()
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(stringResource(R.string.back), fontSize = 12.sp)
            }
            Text(
                text = stringResource(R.string.text_mrs_title),
                color = Color.DarkGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            TextButton(
                onClick = {
                    TextSliderDrag.active = false
                    focus.clearFocus()
                    onDone()
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) { Text(stringResource(R.string.editor_done), fontSize = 12.sp) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextMoveButton("\u25C0", stringResource(R.string.text_move_left), 40) { onMove(-TEXT_MOVE_STEP_DP, 0f) }
            TextMoveButton("\u25B6", stringResource(R.string.text_move_right), 40) { onMove(TEXT_MOVE_STEP_DP, 0f) }
            TextMoveButton("\u25B2", stringResource(R.string.text_move_up), 40) { onMove(0f, -TEXT_MOVE_STEP_DP) }
            TextMoveButton("\u25BC", stringResource(R.string.text_move_down), 40) { onMove(0f, TEXT_MOVE_STEP_DP) }
        }
        val pct = (safeSp / baseSp.coerceAtLeast(0.5f) * 100f).roundToInt()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.text_mrs_size), color = Color.DarkGray, fontSize = 11.sp)
            Text(pct.toString() + "%  ·  " + safeSp.roundToInt().toString() + " sp", color = Color.DarkGray, fontSize = 11.sp)
        }
        CompactSlider(
            value = textBoxSpToT(safeSp),
            range = 0f..1f,
            onChange = { onSizeChange(textBoxTToSp(it)) },
            onEnd = onSizeEnd
        )
        val sizeMarks = listOf(0.25f, 0.5f, 1f, 2f, 4f)
            .filter { f -> baseSp * f >= TEXT_BOX_MIN_SP - 0.01f && baseSp * f <= TEXT_BOX_MAX_SP + 0.01f }
            .map { f -> textBoxSpToT(baseSp * f) to ((f * 100f).roundToInt().toString() + "%") }
        SliderTicks(sizeMarks)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.text_mrs_rotate), color = Color.DarkGray, fontSize = 11.sp)
            val inputDesc = stringResource(R.string.text_mrs_rotate_input)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = rotText,
                    onValueChange = { raw ->
                        val f = filterDegInput(raw)
                        val v = f.toIntOrNull()
                        if (v == null) {
                            rotText = f
                        } else {
                            val c = v.coerceIn(-180, 180)
                            rotText = if (c != v) c.toString() else f
                            onRotate(c.toFloat())
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (rotText.toIntOrNull() == null) rotText = rotInt.toString()
                        onRotateEnd()
                        focus.clearFocus()
                    }),
                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .width(52.dp)
                        .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                        .semantics { contentDescription = inputDesc }
                )
                Text("°", color = Color.DarkGray, fontSize = 11.sp)
            }
        }
        CompactSlider(
            value = rotInt.toFloat(),
            range = -180f..180f,
            onChange = { onRotate(it.roundToInt().toFloat()) },
            onEnd = onRotateEnd
        )
        SliderTicks(
            listOf(
                0f to "-180",
                0.25f to "-90",
                0.5f to "0",
                0.75f to "90",
                1f to "180"
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}
