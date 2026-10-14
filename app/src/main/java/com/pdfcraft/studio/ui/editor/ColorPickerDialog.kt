package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.stringResource
import com.pdfcraft.studio.R
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class NamedColor(val name: String, val hex: Long)

private data class ColorCategory(val title: String, val colors: List<NamedColor>)

private val COLOR_CATEGORIES = listOf(
    ColorCategory(
        "Basic Colors",
        listOf(
            NamedColor("Black", 0xFF000000),
            NamedColor("White", 0xFFFFFFFF),
            NamedColor("Red", 0xFFFF0000),
            NamedColor("Green", 0xFF008000),
            NamedColor("Blue", 0xFF0000FF),
            NamedColor("Yellow", 0xFFFFFF00),
            NamedColor("Orange", 0xFFFFA500),
            NamedColor("Purple", 0xFF800080),
            NamedColor("Pink", 0xFFFFC0CB),
            NamedColor("Brown", 0xFFA52A2A),
            NamedColor("Gray", 0xFF808080)
        )
    ),
    ColorCategory(
        "Reds",
        listOf(
            NamedColor("Crimson", 0xFFDC143C),
            NamedColor("Scarlet", 0xFFFF2400),
            NamedColor("Ruby", 0xFF9B111E),
            NamedColor("Cherry", 0xFFD2042D),
            NamedColor("Burgundy", 0xFF800020),
            NamedColor("Maroon", 0xFF800000),
            NamedColor("Rose", 0xFFFF007F),
            NamedColor("Coral", 0xFFFF7F50)
        )
    ),
    ColorCategory(
        "Oranges",
        listOf(
            NamedColor("Orange", 0xFFFF6600),
            NamedColor("Tangerine", 0xFFF28500),
            NamedColor("Amber", 0xFFFFBF00),
            NamedColor("Apricot", 0xFFFBCEB1),
            NamedColor("Peach", 0xFFFFE5B4),
            NamedColor("Pumpkin", 0xFFFF7518)
        )
    ),
    ColorCategory(
        "Yellows",
        listOf(
            NamedColor("Gold", 0xFFFFD700),
            NamedColor("Lemon", 0xFFFFF44F),
            NamedColor("Mustard", 0xFFFFDB58),
            NamedColor("Canary", 0xFFFFEF00),
            NamedColor("Khaki", 0xFFF0E68C),
            NamedColor("Cream", 0xFFFFFDD0)
        )
    ),
    ColorCategory(
        "Greens",
        listOf(
            NamedColor("Lime", 0xFF32CD32),
            NamedColor("Green", 0xFF008000),
            NamedColor("Emerald", 0xFF50C878),
            NamedColor("Mint", 0xFF98FF98),
            NamedColor("Olive", 0xFF808000),
            NamedColor("Forest", 0xFF228B22),
            NamedColor("Teal", 0xFF008080)
        )
    ),
    ColorCategory(
        "Blues",
        listOf(
            NamedColor("Sky Blue", 0xFF87CEEB),
            NamedColor("Cyan", 0xFF00FFFF),
            NamedColor("Azure", 0xFF007FFF),
            NamedColor("Blue", 0xFF0000FF),
            NamedColor("Royal Blue", 0xFF4169E1),
            NamedColor("Navy", 0xFF000080),
            NamedColor("Aqua", 0xFF00CED1)
        )
    ),
    ColorCategory(
        "Purples",
        listOf(
            NamedColor("Lavender", 0xFFE6E6FA),
            NamedColor("Violet", 0xFF8F00FF),
            NamedColor("Purple", 0xFF800080),
            NamedColor("Orchid", 0xFFDA70D6),
            NamedColor("Plum", 0xFF8E4585),
            NamedColor("Magenta", 0xFFFF00FF)
        )
    ),
    ColorCategory(
        "Pinks",
        listOf(
            NamedColor("Light Pink", 0xFFFFB6C1),
            NamedColor("Pink", 0xFFFFC0CB),
            NamedColor("Hot Pink", 0xFFFF69B4),
            NamedColor("Rose", 0xFFFF007F),
            NamedColor("Salmon", 0xFFFA8072),
            NamedColor("Fuchsia", 0xFFFF00FF)
        )
    ),
    ColorCategory(
        "Browns",
        listOf(
            NamedColor("Beige", 0xFFF5F5DC),
            NamedColor("Tan", 0xFFD2B48C),
            NamedColor("Caramel", 0xFFC68E17),
            NamedColor("Copper", 0xFFB87333),
            NamedColor("Chocolate", 0xFF7B3F00),
            NamedColor("Brown", 0xFFA52A2A)
        )
    ),
    ColorCategory(
        "Grays / Neutrals",
        listOf(
            NamedColor("Light Gray", 0xFFD3D3D3),
            NamedColor("Silver", 0xFFC0C0C0),
            NamedColor("Gray", 0xFF808080),
            NamedColor("Slate", 0xFF708090),
            NamedColor("Charcoal", 0xFF36454F),
            NamedColor("Dark Gray", 0xFF555555)
        )
    )
)

/** One-page color window — every category/color kept, compact so it does not scroll. */
@Composable
fun ColorPickerDialog(
    initialColor: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    previewAsBackground: Boolean = false
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Fixed live preview — does NOT scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Elon Musk",
                        color = if (previewAsBackground) Color.Black else Color(selectedColor),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (previewAsBackground) {
                            Modifier
                                .background(Color(selectedColor), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        } else {
                            Modifier
                        }
                    )
                }

                // Scrollable color categories
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(COLOR_CATEGORIES) { category ->
                        Column {
                            Text(
                                text = category.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )
                            // 5 colors per row
                            category.colors.chunked(5).forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowColors.forEach { named ->
                                        ColorItem(
                                            named = named,
                                            isSelected = selectedColor == named.hex,
                                            onClick = { selectedColor = named.hex }
                                        )
                                    }
                                    repeat(5 - rowColors.size) {
                                        Spacer(modifier = Modifier.width(64.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                // Fixed OK button at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { onConfirm(selectedColor) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "OK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorItem(
    named: NamedColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(named.hex), CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.border(3.dp, Color(0xFF1976D2), CircleShape)
                    } else {
                        Modifier.border(1.dp, Color.LightGray, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = named.name,
            fontSize = 9.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "#%06X".format(named.hex and 0xFFFFFF),
            fontSize = 8.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ShadowSettingsDialog(
    initialColorArgb: Long,
    initialOffsetX: Float,
    initialOffsetY: Float,
    initialBlur: Float,
    onApply: (Long, Float, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var rgb by remember { mutableStateOf((initialColorArgb and 0x00FFFFFFL) or 0xFF000000L) }
    var opacity by remember {
        mutableStateOf(((initialColorArgb ushr 24) and 0xFFL).toFloat() / 255f)
    }
    var blur by remember { mutableStateOf(initialBlur.coerceIn(0f, 25f)) }
    var offsetX by remember { mutableStateOf(initialOffsetX.coerceIn(-20f, 20f)) }
    var offsetY by remember { mutableStateOf(initialOffsetY.coerceIn(-20f, 20f)) }
    fun composedColor(): Long {
        val a = (opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
        return (a.toLong() shl 24) or (rgb and 0x00FFFFFFL)
    }
    val previewColor = composedColor()
    var colorExpanded by remember { mutableStateOf(false) }
    val allShadowColors = remember {
        COLOR_CATEGORIES.flatMap { cat -> cat.colors }.distinctBy { it.hex }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                Text(stringResource(R.string.shadow_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.shadow_preview_sample),
                        style = TextStyle(
                            color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            shadow = Shadow(Color(previewColor), Offset(offsetX, offsetY), blur)
                        )
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { colorExpanded = !colorExpanded }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.shadow_color), fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(22.dp).background(Color(previewColor), CircleShape).border(1.dp, Color(0xFFBDBDBD), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (colorExpanded) "\u25B2" else "\u25BC", color = Color(0xFF1976D2), fontSize = 14.sp)
                }
                if (colorExpanded) {
                    Spacer(Modifier.height(8.dp))
                    allShadowColors.chunked(8).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { named ->
                                val selected = (rgb and 0x00FFFFFFL) == (named.hex and 0x00FFFFFFL)
                                Box(
                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                        .background(Color(named.hex), CircleShape)
                                        .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF1976D2) else Color(0xFFBDBDBD), CircleShape)
                                        .clickable { rgb = (named.hex and 0x00FFFFFFL) or 0xFF000000L }
                                )
                            }
                            repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                ShadowSliderRow(stringResource(R.string.shadow_opacity), opacity * 100f, 0f..100f, "${(opacity * 100f).toInt()}%") { opacity = (it / 100f).coerceIn(0f, 1f) }
                ShadowSliderRow(stringResource(R.string.shadow_blur_label), blur, 0f..25f, blur.toInt().toString()) { blur = it }
                ShadowSliderRow(stringResource(R.string.shadow_offset_x), offsetX, -20f..20f, offsetX.toInt().toString()) { offsetX = it }
                ShadowSliderRow(stringResource(R.string.shadow_offset_y), offsetY, -20f..20f, offsetY.toInt().toString()) { offsetY = it }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { rgb = 0xFF000000L; opacity = 0.50f; blur = 4f; offsetX = 2f; offsetY = 2f }) {
                        Text(stringResource(R.string.shadow_reset), color = Color(0xFF1976D2))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onApply(composedColor(), offsetX, offsetY, blur) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) {
                        Text(stringResource(R.string.shadow_apply), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShadowSliderRow(
    label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>,
    display: String, onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(display, color = Color(0xFF1976D2), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = valueRange,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF1976D2), activeTrackColor = Color(0xFF1976D2)))
    }
}
