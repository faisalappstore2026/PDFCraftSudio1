package com.pdfcraft.studio.ui.home


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.R
import com.pdfcraft.studio.ui.common.AppIcons
import com.pdfcraft.studio.ui.components.AppLogo
import com.pdfcraft.studio.ui.components.PrimaryActionButton
import com.pdfcraft.studio.ui.theme.PDFCraftStudioTheme

@Composable
fun HomeScreen(
    onCreatePdfClick: () -> Unit,
    onMyProjectsClick: () -> Unit = {},
    onRecoverDraft: (String) -> Unit = {},
    onViewPdf: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AppLogo(size = 108.dp)

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
                    text = stringResource(R.string.download_videos_url),
                    icon = AppIcons.Download,
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )
            }
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
