package com.pdfcraft.studio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pdfcraft.studio.R
import com.pdfcraft.studio.core.text.AppFont
import com.pdfcraft.studio.core.text.FontCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontToolsDialog(
    fonts: List<AppFont>,
    selectedFontId: String,
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isUnderlineActive: Boolean = false,
    isStrikeActive: Boolean = false,
    hasTextSelection: Boolean,
    onFontSelected: (AppFont) -> Unit,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit = {},
    onStrikeClick: () -> Unit = {},
    onImportFontClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.text_tool_font_title)) },
                    navigationIcon = {
                        Text(
                            text = "\u2715",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clickable(onClick = onDismiss)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.White
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.font_tools_instructions),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.font_style_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StyleIconButton("B", isBoldActive, hasTextSelection, onBoldClick, FontWeight.Bold)
                    StyleIconButton("I", isItalicActive, hasTextSelection, onItalicClick, fontStyle = FontStyle.Italic)
                    StyleIconButton("U", isUnderlineActive, hasTextSelection, onUnderlineClick, decoration = TextDecoration.Underline)
                    StyleIconButton("T", isStrikeActive, hasTextSelection, onStrikeClick, decoration = TextDecoration.LineThrough)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.font_list_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(10.dp))

                fonts.forEach { font ->
                    val family = FontCatalog.resolveComposeFontFamily(font)
                    val selected = font.id == selectedFontId
                    FontListRow(
                        font = font,
                        fontFamily = family,
                        selected = selected,
                        onClick = { onFontSelected(font) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onImportFontClick) {
                        Text(stringResource(R.string.import_font))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StyleToggleChip(
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    previewWeight: FontWeight,
    previewStyle: FontStyle,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        !enabled -> Color.LightGray
        active -> MaterialTheme.colorScheme.primary
        else -> Color.Gray
    }
    val bg = if (active && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White
    val textColor = when {
        !enabled -> Color.Gray
        active -> MaterialTheme.colorScheme.primary
        else -> Color.Black
    }

    Box(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = previewWeight,
            fontStyle = previewStyle,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun FontListRow(
    font: AppFont,
    fontFamily: FontFamily,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = font.displayName,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = fontFamily
            )
            if (font.isCustom) {
                Text(
                    text = stringResource(R.string.font_imported_label),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        if (selected) {
            Text(
                text = "\u2713",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}


@Composable
private fun StyleIconButton(
    glyph: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    decoration: TextDecoration = TextDecoration.None
) {
    val accent = Color(0xFF1976D2)
    val border = when {
        !enabled -> Color.LightGray
        active -> accent
        else -> Color(0xFFB0BEC5)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(if (active && enabled) accent.copy(alpha = 0.12f) else Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = if (!enabled) Color.Gray else if (active) accent else Color(0xFF455A64),
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textDecoration = decoration,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
