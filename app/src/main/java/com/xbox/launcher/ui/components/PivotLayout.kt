package com.xbox.launcher.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import com.xbox.launcher.ui.theme.ZuneTypography
import com.xbox.launcher.ui.theme.SegoeUILightFontFamily
import androidx.compose.ui.Alignment

@Composable
fun PivotLayout(
    title: String? = null,
    pages: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    isBlackBackground: Boolean = false,
    content: @Composable (Int) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (title != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2f),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(maxWidth = Int.MAX_VALUE)
                            )
                            layout(constraints.maxWidth, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                        .graphicsLayer {
                            val offset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                            translationX = -offset * 150f
                            clip = false
                        }
                        .padding(start = 24.dp)
                ) {
                    Text(
                        text = title,
                        style = ZuneTypography.h1.copy(
                            fontFamily = SegoeUILightFontFamily,
                            fontSize = 120.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-3).sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(end = 48.dp),
            modifier = Modifier
                .weight(1f)
                .statusBarsPadding()
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(page)
            }
        }
    }
}
