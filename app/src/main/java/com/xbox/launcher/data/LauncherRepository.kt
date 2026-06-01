package com.xbox.launcher.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build

class LauncherRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("xbox_launcher_prefs", Context.MODE_PRIVATE)

    fun getManuallyAddedApps(): Set<String> {
        val serialized = prefs.getString("manually_added_apps_v1", "") ?: ""
        if (serialized.isBlank()) return emptySet()
        return serialized.split(";").toSet()
    }

    fun saveManuallyAddedApps(apps: Set<String>) {
        prefs.edit().putString("manually_added_apps_v1", apps.joinToString(";")).apply()
    }

    fun getCachedFeatureGraphic(packageName: String): String? {
        return prefs.getString("feature_graphic_$packageName", null)
    }

    fun cacheFeatureGraphic(packageName: String, url: String) {
        prefs.edit().putString("feature_graphic_$packageName", url).apply()
    }

    fun fetchFeatureGraphicFromStore(packageName: String): String? {
        try {
            val url = java.net.URL("https://play.google.com/store/apps/details?id=$packageName")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val reader = java.io.BufferedReader(java.io.InputStreamReader(conn.inputStream))
            val contentBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                contentBuilder.append(line)
                if (contentBuilder.length > 50000) break
            }
            reader.close()
            
            val content = contentBuilder.toString()
            val ogImageRegex = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""")
            val matchResult = ogImageRegex.find(content)
            if (matchResult != null) {
                return matchResult.groupValues[1]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Retrieves all installed applications and filters them
     */
    fun getGames(showAllApps: Boolean): List<GameInfo> {
        val realApps = mutableListOf<GameInfo>()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val manuallyAdded = getManuallyAddedApps()
        
        try {
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (info in resolveInfos) {
                val activityInfo = info.activityInfo ?: continue
                val packageName = activityInfo.packageName ?: continue
                
                // Avoid listing ourselves
                if (packageName == context.packageName) continue
                
                val label = info.loadLabel(pm).toString()
                val appInfo = activityInfo.applicationInfo
                
                val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    @Suppress("DEPRECATION")
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                }

                // Check persistent stats
                val lastPlayed = prefs.getLong("last_played_$packageName", 0)
                val playTime = prefs.getLong("play_time_$packageName", 0)

                realApps.add(
                    GameInfo(
                        id = packageName,
                        label = label,
                        developer = appInfo.packageName,
                        isGame = isGame,
                        lastPlayedMs = lastPlayed,
                        playTimeMs = playTime,
                        isRealApp = true,
                        featureGraphicUrl = getCachedFeatureGraphic(packageName)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (showAllApps) {
            // Sort by label
            realApps.sortedBy { it.label.lowercase() }
        } else {
            // Only games OR manually added apps
            realApps.filter { it.isGame || manuallyAdded.contains(it.id) }.sortedBy { it.label.lowercase() }
        }
    }

    /**
     * Launches a game: updates stats.
     */
    fun recordLaunch(gameId: String, isRealApp: Boolean) {
        val now = System.currentTimeMillis()
        prefs.edit().apply {
            putLong("last_played_$gameId", now)
            val prevPlayTime = prefs.getLong("play_time_$gameId", 0)
            putLong("play_time_$gameId", prevPlayTime + 120_000L) // Add 2 mock minutes to show dynamic changes
            apply()
        }
    }

    /**
     * Get or set pinned items. Serialization format: "package1,size1;package2,size2"
     */
    fun getPinnedItems(): List<Pair<String, Int>> {
        val defaultPins = "" // Empty by default now
        val serialized = prefs.getString("pinned_games_v2", defaultPins) ?: defaultPins
        if (serialized.isBlank()) return emptyList()
        
        return serialized.split(";").mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size == 2) {
                val id = parts[0]
                val size = parts[1].toIntOrNull() ?: 2
                Pair(id, size)
            } else null
        }
    }

    fun savePinnedItems(items: List<Pair<String, Int>>) {
        val serialized = items.joinToString(";") { "${it.first},${it.second}" }
        prefs.edit().putString("pinned_games_v2", serialized).apply()
    }
}
