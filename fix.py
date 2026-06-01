import sys
path = r"c:\Users\Sreenivishal\Documents\Xbox\app\src\main\java\com\xbox\launcher\ui\screens\LauncherHomeScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    lines = f.readlines()

new_content = """// ==========================================
// TAB 3: XBOX STORE
// ==========================================
@Composable
fun XboxStoreTab() {
    val context = LocalContext.current

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
                title = "Play Store",
                subtitle = "Download new apps and games",
                iconRes = Icons.Default.ShoppingCart,
                onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            )
        }

        item {
            StoreTile(
                title = "Play Games",
                subtitle = "View achievements and leaderboards",
                iconRes = Icons.Default.Games,
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
    title: String,
    subtitle: String,
    iconRes: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(ZuneTileBackground)
            .metroClickable(onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = iconRes,
                contentDescription = null,
                tint = LocalZuneAccent.current,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = ZuneTypography.h4.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = ZuneTextPrimary
            )
            Text(
                text = subtitle,
                style = ZuneTypography.body2.copy(fontSize = 12.sp),
                color = ZuneTextSecondary
            )
        }
    }
}

// ==========================================
// TAB 4: PERSONALIZE (SETTINGS & ACCENTS)
// ==========================================
@Composable
fun PersonalizeTab(viewModel: LauncherViewModel) {
    val context = LocalContext.current
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
"""

start_idx = -1
end_idx = -1
for i, line in enumerate(lines):
    if line.startswith("// TAB 3: XBOX LIVE"):
        start_idx = i - 1
    if "Text(\"save profile changes\"" in line:
        end_idx = i + 3

if start_idx != -1 and end_idx != -1:
    lines = lines[:start_idx] + [new_content] + lines[end_idx:]
    with open(path, "w", encoding="utf-8") as f:
        f.writelines(lines)
    print("Replaced successfully")
else:
    print(f"Failed to find indices. Start: {start_idx}, End: {end_idx}")
