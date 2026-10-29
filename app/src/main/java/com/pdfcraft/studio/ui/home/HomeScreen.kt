package com.pdfcraft.studio.ui.home

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.AppIcons
import com.pdfcraft.studio.ui.components.PrimaryActionButton
import com.pdfcraft.studio.ui.theme.PDFCraftStudioTheme

@Composable
fun HomeScreen(
    onCreatePdfClick: () -> Unit,
    onMyProjectsClick: () -> Unit = {},
    onRecoverDraft: (String) -> Unit = {},
    onViewPdf: () -> Unit = {},
    onIconCenterClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("home_logo", Context.MODE_PRIVATE)
    }
    var logoSize by remember {
        mutableStateOf(prefs.getFloat("size_dp", HOME_ICON_SIZE_DEFAULT)) // fix41: point 9
    }
    // fix37: Home designs 1..30 (null = original look below)
    val homeDesign = currentHomeDesign()
    if (homeDesign != null) {
        HomeDesignedScreen(
            design = homeDesign,
            logoSize = logoSize,
            onLogoSizeChange = { value ->
                logoSize = value
                prefs.edit().putFloat("size_dp", value).apply()
            },
            onCreatePdfClick = onCreatePdfClick,
            onViewPdf = onViewPdf,
            onMyProjectsClick = onMyProjectsClick,
            onIconCenterClick = onIconCenterClick
        )
        return
    }
    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(logoSize.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    // fix38: new Circle.png icon with ICON_FIT (AppIconFit.kt)
                    AppCircleIconImage()
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF8A8A8A),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.widthIn(max = 280.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                PrimaryActionButton(
                    text = stringResource(R.string.create_pdf),
                    icon = Icons.Filled.Add,
                    onClick = onCreatePdfClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                PrimaryActionButton(
                    text = stringResource(R.string.view_pdf),
                    icon = AppIcons.Visibility,
                    onClick = onViewPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                PrimaryActionButton(
                    text = stringResource(R.string.my_projects),
                    icon = AppIcons.FileDocument,
                    onClick = onMyProjectsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                PrimaryActionButton(
                    text = stringResource(R.string.icon_center),
                    icon = Icons.Filled.Settings,
                    onClick = onIconCenterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.icon_size),
                    color = Color(0xFF8A8A8A),
                    fontSize = 13.sp
                )
                Slider(
                    value = logoSize,
                    onValueChange = { value ->
                        logoSize = value
                        prefs.edit().putFloat("size_dp", value).apply()
                    },
                    valueRange = 64f..200f,
                    steps = HOME_ICON_SIZE_STEPS, // fix40: points while sliding
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // fix37: draggable Home Design button
            HomeDesignFloatingButton(
                fabColor = com.pdfcraft.studio.ui.theme.BrandRed,
                fabFg = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PDFCraftStudioTheme {
        Surface {
            HomeScreen(onCreatePdfClick = {})
        }
    }
}
