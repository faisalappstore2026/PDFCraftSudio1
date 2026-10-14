package com.pdfcraft.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfcraft.studio.ui.theme.BrandRed

/**
 * Brand mark matching home hero: red rounded tile, white page, yellow "1" badge.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val corner = size * 0.22f
    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(BrandRed),
        contentAlignment = Alignment.Center
    ) {
        // White document card
        Box(
            modifier = Modifier
                .size(width = size * 0.42f, height = size * 0.52f)
                .clip(RoundedCornerShape(size * 0.06f))
                .background(Color.White)
                .padding(horizontal = size * 0.08f, vertical = size * 0.1f)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
            ) {
                // top accent line (red)
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .size(width = size * 0.22f, height = size * 0.035f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BrandRed)
                )
                // gray text lines
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(y = size * 0.1f)
                        .size(width = size * 0.26f, height = size * 0.028f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFD0D0D0))
                )
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(y = size * 0.16f)
                        .size(width = size * 0.2f, height = size * 0.028f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFD0D0D0))
                )
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(y = size * 0.22f)
                        .size(width = size * 0.24f, height = size * 0.028f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFD0D0D0))
                )
            }
        }
        // Yellow badge "1"
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-size * 0.06f), y = (-size * 0.06f))
                .size(size * 0.28f)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFFFD54F)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "1",
                color = Color(0xFF5D4037),
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.14f).sp
            )
        }
    }
}
