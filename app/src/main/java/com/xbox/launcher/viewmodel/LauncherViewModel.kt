package com.xbox.launcher.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xbox.launcher.data.GameInfo
import com.xbox.launcher.data.LauncherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application)
    
    // Core States
    private val _games = MutableStateFlow<List<GameInfo>>(emptyList())
    val games: StateFlow<List<GameInfo>> = _games.asStateFlow()

    private val _pinnedGames = MutableStateFlow<List<Pair<GameInfo, Int>>>(emptyList())
    val pinnedGames: StateFlow<List<Pair<GameInfo, Int>>> = _pinnedGames.asStateFlow()

    private val _recentGames = MutableStateFlow<List<GameInfo>>(emptyList())
    val recentGames: StateFlow<List<GameInfo>> = _recentGames.asStateFlow()

    // Preferences & Theme
    private val _showAllApps = MutableStateFlow(false)
    val showAllApps: StateFlow<Boolean> = _showAllApps.asStateFlow()

    private val _accentColor = MutableStateFlow(Color(0xFF107C10)) // Xbox Green default
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    private val _wallpaper = MutableStateFlow(0) // 0 = pure black
    val wallpaper: StateFlow<Int> = _wallpaper.asStateFlow()


    init {
        // Read theme settings from standard app preference storage to maintain parity
        val zunePrefs = application.getSharedPreferences("zune_prefs", Context.MODE_PRIVATE)
        _wallpaper.value = zunePrefs.getInt("bg_selection", 0)
        
        // Custom accent mapping
        val savedAccent = zunePrefs.getInt("accent_color_int", 0xFF107C10.toInt())
        _accentColor.value = Color(savedAccent)

        _showAllApps.value = zunePrefs.getBoolean("show_all_apps", true)

        refreshData()
    }

    fun refreshData() {
        val showAll = _showAllApps.value
        val allGames = repository.getGames(showAll)
        _games.value = allGames

        // Load pinned
        val pinnedConfigs = repository.getPinnedItems()
        val pinnedMapped = pinnedConfigs.mapNotNull { config ->
            val game = allGames.find { it.id == config.first }
            if (game != null) Pair(game, config.second) else null
        }
        _pinnedGames.value = pinnedMapped

        // Load recents
        _recentGames.value = allGames.filter { it.lastPlayedMs > 0 }
            .sortedByDescending { it.lastPlayedMs }
            .take(6)
    }

    // Pinned tiles interactions
    fun pinGame(gameId: String) {
        val current = repository.getPinnedItems().toMutableList()
        if (current.none { it.first == gameId }) {
            current.add(Pair(gameId, 2)) // default size: medium
            repository.savePinnedItems(current)
            refreshData()
        }
    }

    fun unpinGame(gameId: String) {
        val current = repository.getPinnedItems().toMutableList()
        val index = current.indexOfFirst { it.first == gameId }
        if (index != -1) {
            current.removeAt(index)
            repository.savePinnedItems(current)
            refreshData()
        }
    }

    fun cyclePinSize(gameId: String) {
        val current = repository.getPinnedItems().toMutableList()
        val index = current.indexOfFirst { it.first == gameId }
        if (index != -1) {
            val oldPair = current[index]
            val newSize = when (oldPair.second) {
                1 -> 2  // small -> medium
                2 -> 4  // medium -> wide
                else -> 1 // wide -> small
            }
            current[index] = Pair(gameId, newSize)
            repository.savePinnedItems(current)
            refreshData()
        }
    }

    fun reorderPinned(fromIndex: Int, toIndex: Int) {
        val current = repository.getPinnedItems().toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            repository.savePinnedItems(current)
            refreshData()
        }
    }

    // Launch operations
    fun launchGame(context: Context, game: GameInfo) {
        viewModelScope.launch {
            // Record game launch stats
            repository.recordLaunch(game.id, game.isRealApp)
            
            if (game.isRealApp) {
                // Real Android app launch
                val launchIntent = context.packageManager.getLaunchIntentForPackage(game.id)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            }

            refreshData()
        }
    }

    // Personalize settings
    fun setAccentColor(color: Color) {
        _accentColor.value = color
        val zunePrefs = getApplication<Application>().getSharedPreferences("zune_prefs", Context.MODE_PRIVATE)
        zunePrefs.edit().putInt("accent_color_int", color.hashCode()).apply()
    }

    fun setWallpaper(wallpaperRes: Int) {
        _wallpaper.value = wallpaperRes
        val zunePrefs = getApplication<Application>().getSharedPreferences("zune_prefs", Context.MODE_PRIVATE)
        zunePrefs.edit().putInt("bg_selection", wallpaperRes).apply()
    }

    fun setShowAllApps(showAll: Boolean) {
        _showAllApps.value = showAll
        val zunePrefs = getApplication<Application>().getSharedPreferences("zune_prefs", Context.MODE_PRIVATE)
        zunePrefs.edit().putBoolean("show_all_apps", showAll).apply()
        refreshData()
    }

    fun getAllInstalledApps(): List<GameInfo> {
        return repository.getGames(showAllApps = true)
    }

    fun getManuallyAddedApps(): Set<String> {
        return repository.getManuallyAddedApps()
    }

    fun addManualApp(packageName: String) {
        val current = repository.getManuallyAddedApps().toMutableSet()
        if (current.add(packageName)) {
            repository.saveManuallyAddedApps(current)
            refreshData()
        }
    }

    fun removeManualApp(packageName: String) {
        val current = repository.getManuallyAddedApps().toMutableSet()
        if (current.remove(packageName)) {
            repository.saveManuallyAddedApps(current)
            unpinGame(packageName)
            refreshData()
        }
    }

    fun loadFeatureGraphic(packageName: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cached = repository.getCachedFeatureGraphic(packageName)
            if (cached != null) return@launch
            
            val url = repository.fetchFeatureGraphicFromStore(packageName)
            if (url != null) {
                repository.cacheFeatureGraphic(packageName, url)
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    refreshData()
                }
            }
        }
    }
}
