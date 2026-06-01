package com.xbox.launcher.data

data class GameInfo(
    val id: String, // package name for real apps, unique string for mock games
    val label: String,
    val developer: String = "Xbox Game Studios",
    val isGame: Boolean = true,
    val bannerUrl: String? = null, // Mock banner image or real application metadata
    val bannerDrawableRes: Int? = null, // Fallback built-in background images
    val lastPlayedMs: Long = 0,
    val playTimeMs: Long = 0,
    val isRealApp: Boolean = false,
    val featureGraphicUrl: String? = null // Cached Play Store feature graphic URL
)

data class Achievement(
    val id: String,
    val gameId: String,
    val title: String,
    val description: String,
    val gamerscore: Int,
    val isUnlocked: Boolean = false,
    val unlockDate: String? = null
)

data class FriendInfo(
    val gamertag: String,
    val motto: String,
    val gamerscore: Int,
    val isOnline: Boolean,
    val currentActivity: String = "Offline",
    val gamerpicRes: Int // local wallpaper drawable or standard icon
)
