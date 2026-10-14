package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

                SizeSelectCard(
                    label = stringResource(R.string.original_image_size_label),
                    selected = selectedOption is ImageSizeOption.Default,
                    prominent = true,
                    onClick = { onOptionSelected(ImageSizeOption.Default) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                ImageSizeOption.presetsKb.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { kb ->
                            val selected = selectedOption is ImageSizeOption.Preset &&
                                selectedOption.kb == kb
                            SizeSelectCard(
                                label = stringResource(R.string.image_size_kb_value, kb),
                                selected = selected,
                                onClick = { onOptionSelected(ImageSizeOption.Preset(kb)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(5 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                CustomSizeCard(
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
    prominent: Boolean = false
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
            maxLines = 1
        )
    }
}

@Composable
private fun CustomSizeCard(
    selectedOption: ImageSizeOption,
    onOptionSelected: (ImageSizeOption) -> Unit
) {
    var text by remember(selectedOption) {
        mutableStateOf((selectedOption as? ImageSizeOption.Custom)?.kb?.toString() ?: "")
    }
    val selected = selectedOption is ImageSizeOption.Custom
    val bg = if (selected) SelectedBg else Color.White
    val borderColor = if (selected) AccentBlue else CardBorder
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeMd)
            .background(bg, ShapeMd)
            .border(if (selected) 2.dp else 1.dp, borderColor, ShapeMd)
            .clickable {
                val kb = text.toIntOrNull()
                onOptionSelected(ImageSizeOption.Custom(if (kb != null && kb > 0) kb else 100))
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                text = stringResource(R.string.image_size_custom),
                color = if (selected) AccentBlue else Color.Black,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (selected) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    val filtered = input.filter(Char::isDigit).take(5)
                    text = filtered
                    val kb = filtered.toIntOrNull()
                    if (kb != null && kb > 0) {
                        onOptionSelected(ImageSizeOption.Custom(kb))
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.image_size_custom_dialog_hint)) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = ShapeMd,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = CardBorder,
                    focusedLabelColor = AccentBlue,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
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
