package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pdfcraft.studio.R
import com.pdfcraft.studio.core.image.ImageSizeOption

private val AccentBlue = Color(0xFF1976D2)
private val CardBorder = Color(0xFFE0E0E0)
private val SelectedBg = Color(0xFFE3F2FD)
private val PageBg = Color(0xFFFAFAFA)
private val ShapeLg = RoundedCornerShape(14.dp)
private val ShapeMd = RoundedCornerShape(12.dp)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportImagesDialog(
    selectedOption: ImageSizeOption,
    onOptionSelected: (ImageSizeOption) -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit,
    pageCount: Int = 1,
    selectedStartPageIndex: Int? = null,
    onStartPageSelected: (Int?) -> Unit = {},
    onRatioSelected: (String) -> Unit = {}
) {
    var showPagePicker by remember { mutableStateOf(false) }
    var selectedImportRatio by remember { mutableStateOf<String?>(null) }
    var draftPageIndex by remember(selectedStartPageIndex) {
        mutableStateOf<Int?>(selectedStartPageIndex)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.import_images_action),
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        Text(
                            text = "\u2715",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp)
                                .clickable(onClick = onDismiss)
                                .padding(8.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onImportClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = ShapeLg,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.import_images_action),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.image_import_instructions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.image_import_instructions_secondary),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF616161)
                )

                Spacer(modifier = Modifier.height(18.dp))
                SectionLabel(stringResource(R.string.image_import_section_size))
                Spacer(modifier = Modifier.height(8.dp))
                ImageImportSizeRow(
                    selectedOption = selectedOption,
                    onOptionSelected = onOptionSelected
                )

                Spacer(modifier = Modifier.height(18.dp))
                SectionLabel(stringResource(R.string.image_import_section_page))
                Spacer(modifier = Modifier.height(8.dp))

                val pageBtnLabel = if (selectedStartPageIndex != null) {
                    stringResource(R.string.import_page_selected, selectedStartPageIndex + 1)
                } else {
                    stringResource(R.string.import_select_page)
                }
                OutlinedButton(
                    onClick = {
                        draftPageIndex = selectedStartPageIndex
                        showPagePicker = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = ShapeMd,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = AccentBlue
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue)
                ) {
                    Text(
                        text = pageBtnLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                SectionLabel(stringResource(R.string.image_import_section_layout))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ImportRatioButton(
                        label = "9:16",
                        icon = "▯",
                        selected = selectedImportRatio == "portrait",
                        onClick = {
                            if (selectedImportRatio == "portrait") {
                                selectedImportRatio = null
                            } else {
                                selectedImportRatio = "portrait"
                                onRatioSelected("portrait")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ImportRatioButton(
                        label = "16:9",
                        icon = "▭",
                        selected = selectedImportRatio == "landscape",
                        onClick = {
                            if (selectedImportRatio == "landscape") {
                                selectedImportRatio = null
                            } else {
                                selectedImportRatio = "landscape"
                                onRatioSelected("landscape")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ImportRatioButton(
                        label = "1:1",
                        icon = "□",
                        selected = selectedImportRatio == "square",
                        onClick = {
                            if (selectedImportRatio == "square") {
                                selectedImportRatio = null
                            } else {
                                selectedImportRatio = "square"
                                onRatioSelected("square")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.import_ratio_manual_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showPagePicker) {
        ImportStartPagePickerDialog(
            pageCount = pageCount.coerceAtLeast(1),
            selectedIndex = draftPageIndex,
            onSelect = { page ->
                // Tap same page again → unselect
                draftPageIndex = if (draftPageIndex == page) null else page
            },
            onNext = {
                onStartPageSelected(draftPageIndex)
                showPagePicker = false
            },
            onDismiss = { showPagePicker = false }
        )
    }
}


@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF9E9E9E)
    )
}

@Composable
private fun ImportStartPagePickerDialog(
    pageCount: Int,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, ShapeLg)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.import_select_page_instruction),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                for (i in 0 until pageCount) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapeMd)
                            .clickable { onSelect(i) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = i == selectedIndex,
                            onClick = { onSelect(i) }
                        )
                        Text(
                            text = stringResource(R.string.import_page_item, i + 1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onNext, shape = ShapeMd) {
                    Text(
                        text = stringResource(R.string.import_select_page_next),
                        color = AccentBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun SizeSelectCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    maxLines: Int = 1
) {
    val bg = when {
        selected -> SelectedBg
        prominent -> PageBg
        else -> Color.White
    }
    val borderColor = if (selected) AccentBlue else CardBorder
    val borderWidth = if (selected) 2.dp else 1.dp
    Row(
        modifier = modifier
            .heightIn(min = if (prominent) 48.dp else 42.dp)
            .clip(ShapeMd)
            .background(bg, ShapeMd)
            .border(borderWidth, borderColor, ShapeMd)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (prominent) 14.dp else 6.dp,
                vertical = if (prominent) 12.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Text(
                text = "\u2713  ",
                color = AccentBlue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            color = if (selected) AccentBlue else Color.Black,
            style = if (prominent) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.labelMedium,
            fontWeight = if (selected || prominent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = maxLines
        )
    }
}

@Composable
private fun ImageImportSizeRow(
    selectedOption: ImageSizeOption,
    onOptionSelected: (ImageSizeOption) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var customOpen by remember { mutableStateOf(false) }
    var restingKb by remember { mutableStateOf(5) }
    val presetKb = (selectedOption as? ImageSizeOption.Preset)?.kb
    val customKb = (selectedOption as? ImageSizeOption.Custom)?.kb
    LaunchedEffect(presetKb, customKb) {
        if (presetKb != null) restingKb = presetKb
        if (customKb != null) restingKb = customKb
    }
    val fieldKb = when (selectedOption) {
        is ImageSizeOption.Preset -> selectedOption.kb
        is ImageSizeOption.Custom -> selectedOption.kb
        else -> restingKb
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SizeSelectCard(
            label = stringResource(R.string.original_image_size_label),
            selected = selectedOption is ImageSizeOption.Default,
            prominent = true,
            maxLines = 2,
            onClick = {
                menuOpen = false
                onOptionSelected(ImageSizeOption.Default)
            },
            modifier = Modifier.weight(1.35f)
        )
        Box(modifier = Modifier.weight(0.85f)) {
            PsSizeCombo(
                value = ImageSizeOption.formatKb(fieldKb),
                open = menuOpen,
                onToggle = { menuOpen = menuOpen.not() },
                onDismiss = { menuOpen = false },
                selectedOption = selectedOption,
                centerKb = fieldKb,
                onPreset = { kb ->
                    menuOpen = false
                    restingKb = kb
                    onOptionSelected(ImageSizeOption.Preset(kb))
                },
                onCustom = {
                    menuOpen = false
                    customOpen = true
                }
            )
        }
    }
    if (customOpen) {
        CustomKbDialog(
            selectedOption = selectedOption,
            onApply = { kb ->
                customOpen = false
                restingKb = kb
                onOptionSelected(ImageSizeOption.Custom(kb))
            },
            onDismiss = { customOpen = false }
        )
    }
}

@Composable
private fun PsSizeCombo(
    value: String,
    open: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    selectedOption: ImageSizeOption,
    centerKb: Int,
    onPreset: (Int) -> Unit,
    onCustom: () -> Unit
) {
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.White)
                .border(1.dp, Color(0xFF808080))
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = Color.Black,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(text = "\u25BC", color = Color.Black, fontSize = 10.sp)
        }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { 40.dp.roundToPx() }),
                onDismissRequest = onDismiss,
                properties = PopupProperties(focusable = true, clippingEnabled = false)
            ) {
                PsSizeMenu(
                    selectedOption = selectedOption,
                    centerKb = centerKb,
                    onPreset = onPreset,
                    onCustom = onCustom
                )
            }
        }
    }
}

@Composable
private fun PsSizeMenu(
    selectedOption: ImageSizeOption,
    centerKb: Int,
    onPreset: (Int) -> Unit,
    onCustom: () -> Unit
) {
    val menu = ImageSizeOption.menuKb
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val itemDp = 22.dp
    val customOn = selectedOption is ImageSizeOption.Custom
    val presetKb = (selectedOption as? ImageSizeOption.Preset)?.kb
    val anchorKb = if (presetKb != null) presetKb else centerKb
    LaunchedEffect(customOn, anchorKb) {
        val at = menu.indexOf(anchorKb)
        val index = if (customOn) 0 else if (at < 0) 0 else at + 1
        val itemPx = with(density) { itemDp.toPx() }
        scroll.scrollTo((index * itemPx).toInt())
    }
    Row(
        modifier = Modifier
            .heightIn(max = 220.dp)
            .shadow(3.dp)
            .background(Color.White)
            .border(1.dp, Color(0xFF808080))
    ) {
        Column(
            modifier = Modifier
                .width(96.dp)
                .verticalScroll(scroll)
                .padding(vertical = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemDp)
                    .background(if (customOn) Color(0xFF316AC5) else Color.White)
                    .clickable { onCustom() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Custom",
                    color = if (customOn) Color.White else Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp),
                    fontSize = 12.sp
                )
            }
            menu.forEach { kb ->
                val picked = kb
                val sel = if (presetKb == null) false else presetKb == picked
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemDp)
                        .background(if (sel) Color(0xFF316AC5) else Color.White)
                        .clickable { onPreset(picked) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = ImageSizeOption.formatKb(picked),
                        color = if (sel) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
        if (scroll.maxValue > 0) {
            PsMenuBar(scroll)
        }
    }
}

@Composable
private fun PsMenuBar(scroll: androidx.compose.foundation.ScrollState) {
    BoxWithConstraints(
        modifier = Modifier
            .width(14.dp)
            .fillMaxHeight()
            .background(Color(0xFFECE9D8))
            .border(1.dp, Color(0xFFA0A0A0))
    ) {
        val track = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val maxv = scroll.maxValue.coerceAtLeast(1)
        val thumb = (track * track / (track + maxv)).coerceIn(28f, track * 0.55f)
        val y = ((track - thumb) * scroll.value / maxv.toFloat()).toInt().coerceAtLeast(0)
        Box(
            modifier = Modifier
                .offset { IntOffset(0, y) }
                .width(14.dp)
                .height(with(LocalDensity.current) { thumb.toDp() })
                .background(Color(0xFFD4D0C8))
                .border(1.dp, Color(0xFF808080))
        )
    }
}

@Composable
private fun CustomKbDialog(
    selectedOption: ImageSizeOption,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var text by remember {
        mutableStateOf(
            when (selectedOption) {
                is ImageSizeOption.Custom -> selectedOption.kb.toString()
                is ImageSizeOption.Preset -> selectedOption.kb.toString()
                else -> ""
            }
        )
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeLg)
                .background(Color.White, ShapeLg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.image_size_custom_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Type a size in KB. Tap outside the box to hide the cursor.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF616161)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter(Char::isDigit).take(6)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.image_size_custom_dialog_hint)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = ShapeMd,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = AccentBlue,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val n = text.toIntOrNull()
                    if (n != null) {
                        if (n > 0) onApply(n)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = ShapeMd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Apply", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun ImportRatioButton(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) SelectedBg else Color.White
    val borderColor = if (selected) AccentBlue else CardBorder
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = ShapeMd,
        contentPadding = PaddingValues(horizontal = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = bg,
            contentColor = if (selected) AccentBlue else Color.Black
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp, borderColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) AccentBlue else Color.Black,
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = icon,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) AccentBlue else Color(0xFF424242)
            )
        }
    }
}
