package com.xbox.launcher.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.xbox.launcher.R
import com.xbox.launcher.data.GameInfo
import com.xbox.launcher.ui.components.PivotLayout
import com.xbox.launcher.ui.components.metroClickable
import com.xbox.launcher.ui.theme.*
import com.xbox.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.PagerState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LauncherHomeScreen(
    viewModel: LauncherViewModel,
    pagerState: PagerState
) {
    val context = LocalContext.current
    val pages = listOf("quick play", "my games", "store", "personalize")

    // State collections
    val pinnedGames by viewModel.pinnedGames.collectAsState()
    val recentGames by viewModel.recentGames.collectAsState()

    val wallpaper by viewModel.wallpaper.collectAsState()

    PivotLayout(
        title = "xbox games",
        pages = pages,
        pagerState = pagerState,
        isBlackBackground = wallpaper == 0
    ) { page ->
        when (page) {
            0 -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    QuickPlayTab(
                        recentGames = recentGames,
                        pinnedGames = pinnedGames,
                        onLaunch = { viewModel.launchGame(context, it) },
                        onUnpin = { viewModel.unpinGame(it) },
                        onCycleSize = { viewModel.cyclePinSize(it) },
                        onMove = { f, t -> viewModel.reorderPinned(f, t) },
                        onUninstall = { packageName ->
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = android.net.Uri.parse("package:$packageName")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        onLoadFeatureGraphic = { viewModel.loadFeatureGraphic(it) }
                    )
                }
            }
            1 -> Column(modifier = Modifier.fillMaxSize()) {
                SectionHeader("my games")
                Box(modifier = Modifier.weight(1f)) {
                    MyGamesTab(
                        viewModel = viewModel,
                        onLaunch = { viewModel.launchGame(context, it) },
                        onPin = { viewModel.pinGame(it) },
                        onUnpin = { viewModel.unpinGame(it) }
                    )
                }
            }
            2 -> Column(modifier = Modifier.fillMaxSize()) {
                SectionHeader("store")
                Box(modifier = Modifier.weight(1f)) {
                    XboxStoreTab()
                }
            }
            3 -> Column(modifier = Modifier.fillMaxSize()) {
                SectionHeader("personalize")
                Box(modifier = Modifier.weight(1f)) {
                    PersonalizeTab(viewModel)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = ZuneTypography.h2.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 2.sp
        ),
        color = Color.White,
        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
    )
}

// ==========================================
// TAB 1: QUICK PLAY (LIVE TILES GRID)
// ==========================================
@Composable
fun QuickPlayTab(
    recentGames: List<GameInfo>,
    pinnedGames: List<Pair<GameInfo, Int>>,
    onLaunch: (GameInfo) -> Unit,
    onUnpin: (String) -> Unit,
    onCycleSize: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onUninstall: (String) -> Unit,
    onLoadFeatureGraphic: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(bottom = 64.dp, end = 24.dp)
    ) {
        // Pinned Dashboard Grid Header
        Text(
            text = "pins",
            style = ZuneTypography.h1.copy(
                fontFamily = com.xbox.launcher.ui.theme.SegoeUILightFontFamily,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp
            ),
            color = Color.White,
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
        )
        
        Box(modifier = Modifier.fillMaxHeight()) {
            PinnedGrid(
                pinnedGames = pinnedGames,
                onLaunch = onLaunch,
                onUnpin = onUnpin,
                onCycleSize = onCycleSize,
                onMove = onMove,
                onUninstall = onUninstall,
                onLoadFeatureGraphic = onLoadFeatureGraphic
            )
        }
    }
}

@Composable
fun RecentGameCard(
    game: GameInfo,
    onClick: () -> Unit,
    onLoadFeatureGraphic: (String) -> Unit
) {
    if (game.isRealApp && game.featureGraphicUrl == null) {
        LaunchedEffect(game.id) {
            onLoadFeatureGraphic(game.id)
        }
    }

    Card(
        shape = RoundedCornerShape(0.dp), // metro is completely square
        backgroundColor = ZuneTileBackground,
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .metroClickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (game.featureGraphicUrl != null) {
                AsyncImage(
                    model = game.featureGraphicUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AppIcon(
                    packageName = game.id,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Text banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(6.dp)
                    .align(Alignment.BottomStart)
            ) {
                Text(
                    text = game.label,
                    style = ZuneTypography.body2.copy(color = ZuneTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PinnedGrid(
    pinnedGames: List<Pair<GameInfo, Int>>,
    onLaunch: (GameInfo) -> Unit,
    onUnpin: (String) -> Unit,
    onCycleSize: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onUninstall: (String) -> Unit,
    onLoadFeatureGraphic: (String) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var hoveredId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var pointerOffset by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }

    if (pinnedGames.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "no pinned games. long-press in 'my games' to pin!",
                style = ZuneTypography.body2,
                color = ZuneTextSecondary,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp)
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectTapGestures { isEditMode = false }
                }
            }
    ) {
        val columns = 4 // Windows Phone style: 4 unit columns, large tiles = 2x2, small = 1x1
        val horizontalSpacing = 6.dp
        val verticalSpacing = 6.dp
        val cellWidth = (maxWidth - horizontalSpacing * (columns - 1)) / columns
        val cellHeight = cellWidth

        // Optimized for 120Hz: cache layout calculations to avoid frame drops on recomposition
        val gridLayout = remember(pinnedGames) {
            val occupied = mutableSetOf<Pair<Int, Int>>()
            val placements = mutableMapOf<String, Rect>()

            for ((game, size) in pinnedGames) {
                // small=1x1, medium=2x2 (half width), large=4x2 (full width)
                val w = if (size == 4) 4 else if (size == 2) 2 else 1
                val h = if (size >= 2) 2 else 1
                var found = false
                var searchY = 0
                while (!found) {
                    for (searchX in 0..columns - w) {
                        var collision = false
                        for (dy in 0 until h) {
                            for (dx in 0 until w) {
                                if (occupied.contains(Pair(searchX + dx, searchY + dy))) {
                                    collision = true
                                    break
                                }
                            }
                            if (collision) break
                        }
                        if (!collision) {
                            for (dy in 0 until h) {
                                for (dx in 0 until w) {
                                    occupied.add(Pair(searchX + dx, searchY + dy))
                                }
                            }
                            placements[game.id] = Rect(
                                left = searchX.toFloat(),
                                top = searchY.toFloat(),
                                right = (searchX + w).toFloat(),
                                bottom = (searchY + h).toFloat()
                            )
                            found = true
                            break
                        }
                    }
                    if (!found) searchY++
                }
            }

            val maxY = occupied.maxOfOrNull { it.second }?.plus(1) ?: 0
            Pair(placements, maxY)
        }

        val placements = gridLayout.first
        val maxY = gridLayout.second
        val totalHeight = if (maxY > 0) {
            (cellHeight * maxY) + (verticalSpacing * (maxY - 1))
        } else 0.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            pinnedGames.forEach { (game, size) ->
                val rect = placements[game.id] ?: return@forEach
                val xOffset = (cellWidth * rect.left) + (horizontalSpacing * rect.left)
                val yOffset = (cellHeight * rect.top) + (verticalSpacing * rect.top)
                val width = (cellWidth * rect.width) + (horizontalSpacing * (rect.width - 1f))
                val height = (cellHeight * rect.height) + (verticalSpacing * (rect.height - 1f))

                val id = game.id
                val isDragged = draggedId == id
                val isHovered = hoveredId == id

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(width = width, height = height)
                        .onGloballyPositioned { coordinates ->
                            itemBounds[id] = coordinates.boundsInWindow()
                        }
                        .zIndex(if (isDragged) 10f else 1f)
                        .graphicsLayer {
                            if (isDragged) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.05f
                                scaleY = 1.05f
                                alpha = 0.9f
                            } else if (isEditMode) {
                                scaleX = if (isHovered) 0.85f else 0.92f
                                scaleY = if (isHovered) 0.85f else 0.92f
                                alpha = if (isHovered) 0.5f else 0.85f
                            }
                        }
                        .pointerInput(isEditMode, id) {
                            if (isEditMode) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        draggedId = id
                                        hoveredId = null
                                        dragOffset = Offset.Zero
                                        pointerOffset = offset
                                    },
                                    onDragEnd = {
                                        if (hoveredId != null && draggedId != null && hoveredId != draggedId) {
                                            val sIdx = pinnedGames.indexOfFirst { it.first.id == draggedId }
                                            val tIdx = pinnedGames.indexOfFirst { it.first.id == hoveredId }
                                            if (sIdx != -1 && tIdx != -1) {
                                                onMove(sIdx, tIdx)
                                            }
                                        }
                                        draggedId = null
                                        hoveredId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggedId = null
                                        hoveredId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                        val myBounds = itemBounds[id] ?: return@detectDragGestures
                                        val absoluteFinger = myBounds.topLeft + pointerOffset + dragOffset

                                        var newHovered: String? = null
                                        for ((targetId, bounds) in itemBounds) {
                                            if (targetId != id && bounds.contains(absoluteFinger)) {
                                                newHovered = targetId
                                                break
                                            }
                                        }
                                        hoveredId = newHovered
                                    }
                                )
                            } else {
                                detectTapGestures(
                                    onTap = { onLaunch(game) },
                                    onLongPress = { isEditMode = true }
                                )
                            }
                        }
                ) {
                    LiveTile(
                        game = game,
                        size = size,
                        isEditMode = isEditMode,
                        onUnpin = { onUnpin(id) },
                        onCycleSize = { onCycleSize(id) },
                        onUninstall = { onUninstall(id) },
                        onLoadFeatureGraphic = onLoadFeatureGraphic
                    )
                }
            }
        }
    }
}

@Composable
fun LiveTile(
    game: GameInfo,
    size: Int,
    isEditMode: Boolean,
    onUnpin: () -> Unit,
    onCycleSize: () -> Unit,
    onUninstall: () -> Unit,
    onLoadFeatureGraphic: (String) -> Unit
) {
    val accent = LocalZuneAccent.current

    if (game.isRealApp && game.featureGraphicUrl == null) {
        LaunchedEffect(game.id) {
            onLoadFeatureGraphic(game.id)
        }
    }

    var showAltContent by remember { mutableStateOf(false) }
    var frontFaceType by remember(game.featureGraphicUrl) { mutableStateOf(if (game.featureGraphicUrl != null) 1 else 0) }

    // Reset when entering edit mode
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            showAltContent = false
            frontFaceType = 0
        }
    }

    // Animated stats periodically flip randomly for any tile
    if (!isEditMode) {
        LaunchedEffect(game.id) {
            // Staggered trigger to decouple flips across multiple tiles
            delay((1000..6000).random().toLong())
            while (true) {
                delay((4000..8000).random().toLong())
                if ((0..1).random() == 0) {
                    val nextShowAlt = !showAltContent
                    if (!nextShowAlt && game.featureGraphicUrl != null) {
                        frontFaceType = (frontFaceType + 1) % 2
                    }
                    showAltContent = nextShowAlt
                }
            }
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (showAltContent) 180f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "TileFlipRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(accent)
            .graphicsLayer {
                rotationX = rotation
                cameraDistance = 8 * density
            }
    ) {
        // We negate the flip on contents to prevent text being backwards
        val cardContentModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (rotation > 90f) {
                    rotationX = 180f
                }
            }

        if (rotation <= 90f) {
            // FRONT SIDE: Game branding
            Box(modifier = cardContentModifier) {
                if (game.featureGraphicUrl != null && frontFaceType == 1) {
                    AsyncImage(
                        model = game.featureGraphicUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AppIcon(
                        packageName = game.id,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Removed Dim Overlay completely to keep tiles fully vibrant

                // Label at bottom only if feature graphic is showing
                if (game.featureGraphicUrl != null && frontFaceType == 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = game.label.lowercase(),
                            style = ZuneTypography.body1.copy(
                                fontSize = if (size == 1) 10.sp else 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ZuneTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            // BACK SIDE: Live Stats (Play time & Achievements)
            Box(
                modifier = cardContentModifier
                    .background(accent.darkenForLiveTile())
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = game.label,
                        style = ZuneTypography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = ZuneTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Column {
                        val hours = (game.playTimeMs / 3600000.0)
                        Text(
                            text = "played: ${String.format("%.1f", hours)} hr",
                            style = ZuneTypography.body2.copy(fontSize = 11.sp, color = ZuneTextPrimary)
                        )

                        // No mock achievements
                    }
                }
            }
        }

        // Edit Mode Overlays
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // Cycle Size button (bottom left)
            IconButton(
                onClick = onCycleSize,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Resize",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Unpin Button (top right)
            IconButton(
                onClick = onUnpin,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Unpin",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Uninstall Button (top left)
            IconButton(
                onClick = onUninstall,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Uninstall",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun Color.darkenForLiveTile(): Color {
    return this.copy(red = this.red * 0.7f, green = this.green * 0.7f, blue = this.blue * 0.7f)
}

// ==========================================
// TAB 2: MY GAMES (DIRECTORY & JUMP-LIST)
// ==========================================
@Composable
fun MyGamesTab(
    viewModel: LauncherViewModel,
    onLaunch: (GameInfo) -> Unit,
    onPin: (String) -> Unit,
    onUnpin: (String) -> Unit
) {
    val context = LocalContext.current
    val games by viewModel.games.collectAsState()
    val pinnedGames by viewModel.pinnedGames.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredGames = remember(games, searchQuery) {
        if (searchQuery.isBlank()) games
        else games.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    val listState = rememberLazyListState()
    var showManageAppsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("search games...", color = ZuneTextSecondary) },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = ZuneTileBackground,
                    textColor = ZuneTextPrimary,
                    cursorColor = LocalZuneAccent.current,
                    focusedIndicatorColor = LocalZuneAccent.current,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ZuneTextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(0.dp)
            )

            // Header and Manage Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredGames.size} apps",
                    style = ZuneTypography.body2,
                    color = ZuneTextSecondary
                )
                
                Text(
                    text = "+ manage apps",
                    style = ZuneTypography.body2.copy(fontWeight = FontWeight.Bold, color = LocalZuneAccent.current),
                    modifier = Modifier
                        .clickable { showManageAppsDialog = true }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }

            if (filteredGames.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("no games or apps found.", style = ZuneTypography.body2, color = ZuneTextSecondary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Games in a flat list
                    items(filteredGames) { game ->
                        val isPinned = pinnedGames.any { it.first.id == game.id }
                        var showContext by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .metroClickable { onLaunch(game) }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onLaunch(game) },
                                        onLongPress = { showContext = true }
                                    )
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(
                                    packageName = game.id,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = game.label,
                                    style = ZuneTypography.body1.copy(fontWeight = FontWeight.Normal),
                                    color = ZuneTextPrimary
                                )
                            }

                            // Custom Context Menu
                            DropdownMenu(
                                expanded = showContext,
                                onDismissRequest = { showContext = false },
                                modifier = Modifier.background(ZuneTileBackground)
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        if (isPinned) onUnpin(game.id) else onPin(game.id)
                                        showContext = false
                                    }
                                ) {
                                    Text(
                                        text = if (isPinned) "unpin from start" else "pin to start",
                                        color = Color.White,
                                        style = ZuneTypography.body2
                                    )
                                }
                                DropdownMenuItem(onClick = { onLaunch(game); showContext = false }) {
                                    Text(
                                        text = "launch game",
                                        color = Color.White,
                                        style = ZuneTypography.body2
                                    )
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        showContext = false
                                        val intent = Intent(Intent.ACTION_DELETE).apply {
                                            data = android.net.Uri.parse("package:${game.id}")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text(
                                        text = "uninstall app",
                                        color = Color.White,
                                        style = ZuneTypography.body2
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // ================= MANAGE APPS OVERLAY =================
        if (showManageAppsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.98f))
                    .pointerInput(Unit) {
                        detectTapGestures { /* consume clicks */ }
                    }
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "manage launcher apps".uppercase(),
                            style = ZuneTypography.h3.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = LocalZuneAccent.current
                        )
                        IconButton(onClick = { showManageAppsDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "select apps to show in your game directory list",
                        style = ZuneTypography.body2,
                        color = ZuneTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val allApps = remember { viewModel.getAllInstalledApps() }
                    val manuallyAdded = remember { mutableStateListOf<String>().apply { addAll(viewModel.getManuallyAddedApps()) } }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allApps) { app ->
                            val isGame = app.isGame
                            val isChecked = isGame || manuallyAdded.contains(app.id)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ZuneTileBackground)
                                    .clickable(enabled = !isGame) {
                                        if (isChecked) {
                                            viewModel.removeManualApp(app.id)
                                            manuallyAdded.remove(app.id)
                                        } else {
                                            viewModel.addManualApp(app.id)
                                            manuallyAdded.add(app.id)
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    packageName = app.id,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = ZuneTypography.body1.copy(fontWeight = FontWeight.Bold),
                                        color = ZuneTextPrimary
                                    )
                                    Text(
                                        text = if (isGame) "Game (Always Visible)" else app.id,
                                        style = ZuneTypography.body2.copy(fontSize = 11.sp),
                                        color = ZuneTextSecondary
                                    )
                                }
                                Checkbox(
                                    checked = isChecked,
                                    enabled = !isGame,
                                    onCheckedChange = { checked ->
                                        if (!checked) {
                                            viewModel.removeManualApp(app.id)
                                            manuallyAdded.remove(app.id)
                                        } else {
                                            viewModel.addManualApp(app.id)
                                            manuallyAdded.add(app.id)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = LocalZuneAccent.current,
                                        uncheckedColor = ZuneTextSecondary,
                                        disabledColor = LocalZuneAccent.current.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// removed XboxLiveTab

// ==========================================
// TAB 3: XBOX STORE
// ==========================================
@Composable
fun XboxStoreTab() {
    val context = LocalContext.current
    val accentColor = LocalZuneAccent.current

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "google play",
                style = ZuneTypography.subtitle1.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = ZuneTextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            StoreTile(
                text = "Get games",
                backgroundColor = accentColor,
                onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.books")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        val playStoreBooksIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/books")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(playStoreBooksIntent)
                        } catch (e: Exception) {
                            val vendingIntent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
                            if (vendingIntent != null) {
                                vendingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(vendingIntent)
                            }
                        }
                    }
                }
            )
        }

        item {
            StoreTile(
                text = "View achievements and leaderboards",
                backgroundColor = ZuneTileBackground,
                onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.play.games")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StoreTile(
    text: String,
    backgroundColor: Color = ZuneTileBackground,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(backgroundColor)
            .metroClickable(onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = text,
                style = ZuneTypography.h1.copy(
                    fontFamily = SegoeUILightFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 32.sp
                ),
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// TAB 4: PERSONALIZE (SETTINGS & ACCENTS)
// ==========================================
@Composable
fun PersonalizeTab(viewModel: LauncherViewModel) {
    val showAllApps by viewModel.showAllApps.collectAsState()
    val wallpaper by viewModel.wallpaper.collectAsState()

    val accent = LocalZuneAccent.current

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Show All Apps Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZuneTileBackground)
                .clickable {
                    viewModel.setShowAllApps(!showAllApps)
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "list non-game apps",
                    style = ZuneTypography.body1.copy(fontWeight = FontWeight.Bold),
                    color = ZuneTextPrimary
                )
                Text(
                    text = "Show standard Android launcher apps in directory list",
                    style = ZuneTypography.body2.copy(fontSize = 12.sp),
                    color = ZuneTextSecondary
                )
            }
            Checkbox(
                checked = showAllApps,
                onCheckedChange = { viewModel.setShowAllApps(it) },
                colors = CheckboxDefaults.colors(checkedColor = accent, uncheckedColor = ZuneTextSecondary)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Accent Color Section
        Text(
            text = "accent color selection",
            style = ZuneTypography.subtitle1.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = ZuneTextSecondary
        )
        
        val colors = listOf(
            Color(0xFF0078D7) to "mandala blue",
            Color(0xFF107C10) to "circuit green",
            Color(0xFF8E9EA5) to "controller grey",
            Color(0xFF00D2C4) to "aurora teal",
            Color(0xFFE25B1D) to "zune orange"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { (colorItem, name) ->
                val isSelected = accent == colorItem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ZuneTileBackground)
                        .clickable { viewModel.setAccentColor(colorItem) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(colorItem)
                            .then(
                                if (isSelected) Modifier.border(2.dp, Color.White)
                                else Modifier
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = name,
                        style = ZuneTypography.body1.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) accent else ZuneTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Parallax Wallpaper Section
        Text(
            text = "backdrop selection",
            style = ZuneTypography.subtitle1.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = ZuneTextSecondary
        )

        val wallpapers = listOf(
            0 to "pure black",
            R.drawable.bg_1 to "mandala logic",
            R.drawable.bg_2 to "circuit matrix",
            R.drawable.bg_3 to "gear shelf",
            R.drawable.bg_4 to "aurora rift"
        )

        wallpapers.forEach { (drawableRes, name) ->
            val isSelected = wallpaper == drawableRes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZuneTileBackground)
                    .clickable { viewModel.setWallpaper(drawableRes) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (drawableRes == 0) Color.Black else Color.Transparent)
                ) {
                    if (drawableRes != 0) {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = name,
                    style = ZuneTypography.body1.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) accent else ZuneTextPrimary
                )
            }
        }
    }
}

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalZuneAccent.current),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    if (icon != null) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && icon is android.graphics.drawable.AdaptiveIconDrawable) {
            Box(modifier = modifier.clip(RoundedCornerShape(0.dp))) {
                AsyncImage(
                    model = icon.background,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                )
                AsyncImage(
                    model = icon.foreground,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                )
            }
        } else {
            AsyncImage(
                model = icon,
                contentDescription = null,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    } else {
        Box(modifier = modifier) {
            fallback()
        }
    }
}
