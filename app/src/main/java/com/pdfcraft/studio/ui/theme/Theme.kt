package com.pdfcraft.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PdfCraftColors = lightColorScheme(
    primary = BrandRed,
    onPrimary = BrandOnRed,
    secondary = BrandGrey,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = BrandPageSurface,
    error = BrandRedDark
)

@Composable
fun PDFCraftStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PdfCraftColors,
        typography = PdfCraftTypography,
        content = content
    )
}
