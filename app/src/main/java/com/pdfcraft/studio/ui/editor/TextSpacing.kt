package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** fix43: Text Tools "Spacing" = letter spacing (em) + line spacing (multiplier). */
internal object TextSpacingRange {
    const val MIN_LETTER = -0.10f
    const val MAX_LETTER = 0.50f
    const val LETTER_STEPS = 59 // 0.01 em
    const val MIN_LINE = 0.50f
    const val MAX_LINE = 3.00f
    const val LINE_STEPS = 49 // 0.05x
}

internal fun textLetterSpacing(te: TextElement): TextUnit =
    if (abs(te.letterSpacingEm) < 0.001f) TextUnit.Unspecified else te.letterSpacingEm.em

/** Default Compose line height is about 1.2 em, so 1.0x keeps the old look. */
internal fun textLineHeight(te: TextElement): TextUnit =
    if (abs(te.lineSpacing - 1f) < 0.005f) TextUnit.Unspecified else (1.2f * te.lineSpacing).em

@Composable
internal fun TextSpacingSliderPanel(
    letterEm: Float,
    lineMult: Float,
    onLetterChange: (Float) -> Unit,
    onLineChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.text_spacing), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF212121))
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) { Text(stringResource(R.string.reset_label)) }
            TextButton(onClick = onDone) { Text(stringResource(R.string.done_label)) }
        }
        SpacingRow(
            label = stringResource(R.string.letter_spacing),
            valueText = String.format(Locale.US, "%.2f", letterEm),
            value = letterEm,
            min = TextSpacingRange.MIN_LETTER,
            max = TextSpacingRange.MAX_LETTER,
            steps = TextSpacingRange.LETTER_STEPS,
            snap = 100f,
            onChange = onLetterChange
        )
        SpacingRow(
            label = stringResource(R.string.line_spacing),
            valueText = String.format(Locale.US, "%.2fx", lineMult),
            value = lineMult,
            min = TextSpacingRange.MIN_LINE,
            max = TextSpacingRange.MAX_LINE,
            steps = TextSpacingRange.LINE_STEPS,
            snap = 20f,
            onChange = onLineChange
        )
    }
}

@Composable
private fun SpacingRow(
    label: String,
    valueText: String,
    value: Float,
    min: Float,
    max: Float,
    steps: Int,
    snap: Float,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = Color(0xFF424242))
        Spacer(Modifier.weight(1f))
        Text(valueText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
    }
    Slider(
        value = value.coerceIn(min, max),
        onValueChange = { onChange(((it * snap).roundToInt() / snap).coerceIn(min, max)) },
        valueRange = min..max,
        steps = steps
    )
}
