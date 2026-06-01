package com.xbox.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xbox.launcher.ui.screens.LauncherHomeScreen
import com.xbox.launcher.ui.theme.ZuneTheme
import com.xbox.launcher.viewmodel.LauncherViewModel
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            LauncherApp()
        }
    }
}

@Composable
fun LauncherApp() {
    val viewModel: LauncherViewModel = viewModel()
    val context = LocalContext.current

    DisposableEffect(viewModel) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                viewModel.refreshData()
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_PACKAGE_ADDED)
            addAction(android.content.Intent.ACTION_PACKAGE_REMOVED)
            addAction(android.content.Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val accentColor by viewModel.accentColor.collectAsState()
    val wallpaper by viewModel.wallpaper.collectAsState()

    val pagerState = rememberPagerState { 4 }

    // Smooth accent transition
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 800)
    )

    ZuneTheme(dynamicAccent = animatedAccent) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            
            // Parallax Wallpaper Background
            if (wallpaper != 0) {
                val painter = painterResource(id = wallpaper)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (painter.intrinsicSize.width > 0 && painter.intrinsicSize.height > 0) {
                        val imgWidth = size.height * (painter.intrinsicSize.width / painter.intrinsicSize.height)
                        
                        // Read pagerState in draw phase to avoid recomposing parent
                        val offset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        val parallaxFactor = 0.25f
                        val scrollOffset = offset * parallaxFactor
                        
                        val centerOffset = (size.width - imgWidth) / 2f
                        val shift = -scrollOffset * 300f
                        val rawX = centerOffset + shift
                        
                        var xOffset = rawX % imgWidth
                        if (xOffset > 0f) {
                            xOffset -= imgWidth
                        }
                        
                        translate(left = xOffset) {
                            with(painter) {
                                draw(size = size.copy(width = imgWidth))
                            }
                        }
                        
                        translate(left = xOffset + imgWidth) {
                            with(painter) {
                                draw(size = size.copy(width = imgWidth))
                            }
                        }
                    }
                }

                // Elegant semi-transparent dim layer for Metro readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                )
            }

            // Main Launcher Pages Content
            Box(modifier = Modifier.fillMaxSize()) {
                LauncherHomeScreen(
                    viewModel = viewModel,
                    pagerState = pagerState
                )
            }



            // Removes Achievement Toast
        }
    }
}

// Removed AchievementUnlockedToast
