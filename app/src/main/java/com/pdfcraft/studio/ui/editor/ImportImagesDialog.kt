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
import androidx.compose.ui.unit.dp
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
    LaunchedEffect(presetKb) {
        if (presetKb != null) restingKb = presetKb
    }
    val shownKb = if (presetKb != null) presetKb else restingKb
    val customOn = selectedOption is ImageSizeOption.Custom || selectedOption is ImageSizeOption.CustomBytes
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
            modifier = Modifier.weight(1.45f)
        )
        Box(modifier = Modifier.weight(0.78f)) {
            SizeSelectCard(
                label = if (presetKb != null) "${shownKb}KB" else "${shownKb}KB \u25BE",
                selected = presetKb != null,
                onClick = { menuOpen = menuOpen.not() },
                modifier = Modifier.fillMaxWidth()
            )
            if (menuOpen) {
                val density = LocalDensity.current
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { 52.dp.roundToPx() }),
                    onDismissRequest = { menuOpen = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    SizeKbMenu(
                        selectedKb = presetKb,
                        centerKb = shownKb,
                        onPick = { kb ->
                            menuOpen = false
                            restingKb = kb
                            onOptionSelected(ImageSizeOption.Preset(kb))
                        }
                    )
                }
            }
        }
        SizeSelectCard(
            label = stringResource(R.string.image_size_custom),
            selected = customOn,
            prominent = true,
            onClick = {
                menuOpen = false
                customOpen = true
            },
            modifier = Modifier.weight(0.72f)
        )
    }
    if (customOpen) {
        CustomBytesDialog(
            selectedOption = selectedOption,
            onApplyKb = { kb ->
                customOpen = false
                onOptionSelected(ImageSizeOption.Custom(kb))
            },
            onApplyBytes = { bytes ->
                customOpen = false
                onOptionSelected(ImageSizeOption.CustomBytes(bytes))
            },
            onDismiss = { customOpen = false }
        )
    }
}

@Composable
private fun SizeKbMenu(
    selectedKb: Int?,
    centerKb: Int,
    onPick: (Int) -> Unit
) {
    val menu = ImageSizeOption.menuKb
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val itemDp = 44.dp
    val viewDp = 220.dp
    val padDp = 88.dp
    LaunchedEffect(centerKb) {
        val index = menu.indexOf(centerKb).let { if (it < 0) 0 else it }
        val itemPx = with(density) { itemDp.toPx() }
        scroll.scrollTo((index * itemPx).toInt())
    }
    Column(
        modifier = Modifier
            .width(168.dp)
            .height(viewDp)
            .shadow(10.dp, ShapeMd)
            .clip(ShapeMd)
            .background(Color.White, ShapeMd)
            .border(1.dp, CardBorder, ShapeMd)
            .verticalScroll(scroll)
    ) {
        Spacer(modifier = Modifier.height(padDp))
        menu.forEach { kb ->
            val selected = kb == selectedKb
            val rowBg = if (selected) SelectedBg else Color.White
            val rowFg = if (selected) AccentBlue else Color.Black
            val line = if (selected) "\u2713  ${kb}KB" else "${kb}KB"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemDp)
                    .background(rowBg)
                    .border(2.dp, if (selected) AccentBlue else Color.Transparent)
                    .clickable { onPick(kb) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = line,
                    color = rowFg,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.height(padDp))
    }
}

@Composable
private fun CustomBytesDialog(
    selectedOption: ImageSizeOption,
    onApplyKb: (Int) -> Unit,
    onApplyBytes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var bytesMode by remember {
        mutableStateOf(selectedOption is ImageSizeOption.CustomBytes)
    }
    var text by remember {
        mutableStateOf(
            when (selectedOption) {
                is ImageSizeOption.CustomBytes -> selectedOption.bytes.toString()
                is ImageSizeOption.Custom -> selectedOption.kb.toString()
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
                text = "Pick a unit, type a number, then tap Apply.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF616161)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SizeSelectCard(
                    label = "Bytes",
                    selected = bytesMode,
                    onClick = {
                        focusManager.clearFocus()
                        bytesMode = true
                    },
                    modifier = Modifier.weight(1f)
                )
                SizeSelectCard(
                    label = "KB",
                    selected = bytesMode.not(),
                    onClick = {
                        focusManager.clearFocus()
                        bytesMode = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter(Char::isDigit).take(7)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(if (bytesMode) "Size in bytes" else "Size in KB")
                },
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
            Spacer(modifier = Modifier.height(8.dp))
            val amount = text.toIntOrNull()
            val helper = if (amount == null) {
                "Tap outside the box to hide the cursor."
            } else if (amount <= 0) {
                "Enter a number greater than zero."
            } else if (bytesMode) {
                "Imported images will be about $amount bytes."
            } else {
                "Imported images will be about $amount KB."
            }
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF616161)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val n = text.toIntOrNull()
                    if (n != null) {
                        if (n > 0) {
                            if (bytesMode) onApplyBytes(n) else onApplyKb(n)
                        }
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
