package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.InventoryItem
import com.example.data.PlayerStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private val _playerStats = MutableStateFlow<PlayerStats?>(null)
    val playerStats: StateFlow<PlayerStats?> = _playerStats.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    // Active Slot (1, 2, or 3)
    private var activeSlotId = 1

    // Engine representation
    val gameEngine = GameEngine()
    private var gameLoopJob: Job? = null

    // UI Navigation State
    private val _currentScreen = MutableStateFlow("main_menu")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Active Game States
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _isVictory = MutableStateFlow(false)
    val isVictory: StateFlow<Boolean> = _isVictory.asStateFlow()

    // Game Settings state
    val isSoundEnabled = MutableStateFlow(true)
    val difficultyLevel = MutableStateFlow("Normal") // Easy, Normal, Hard
    val graphicDetails = MutableStateFlow("High") // Medium, High, Ultra

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        // Load default Slot 1 data on launch
        loadSlot(1)
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
        if (screen != "play") {
            stopGameLoop()
        }
    }

    // Set active database slot and read data
    fun loadSlot(slotId: Int) {
        activeSlotId = slotId
        viewModelScope.launch {
            // Read stats from DB
            repository.getPlayerStats(slotId).collect { stats ->
                if (stats == null) {
                    // Initialize default stats for empty slot
                    repository.initializeNewGame(slotId, "Warrior $slotId")
                } else {
                    _playerStats.value = stats
                    // Sync stats back into our gameEngine values
                    gameEngine.coins = stats.coins
                    gameEngine.xp = stats.xp
                    gameEngine.currentHealth = stats.currentHealth
                    gameEngine.maxHealth = stats.maxHealth
                    gameEngine.currentStamina = stats.currentStamina
                    gameEngine.maxStamina = stats.maxStamina
                    gameEngine.currentLevel = stats.storyProgress

                    // Select correct active weapon based on database value
                    val activeW = WeaponType.values().find { it.key == stats.activeWeapon } ?: WeaponType.SWORD
                    gameEngine.activeWeapon = activeW
                }
            }
        }

        viewModelScope.launch {
            // Read Inventory from DB
            repository.getInventoryItems(slotId).collect { items ->
                _inventory.value = items
                // Sync weapon levels & unlocked sets back to Engine
                items.forEach { item ->
                    val weapon = WeaponType.values().find { it.key == item.itemKey }
                    if (weapon != null) {
                        gameEngine.weaponLevels[weapon] = item.level
                        if (item.isUnlocked) {
                            gameEngine.unlockedWeapons.add(weapon)
                        } else {
                            gameEngine.unlockedWeapons.remove(weapon)
                        }
                    }
                }
            }
        }
    }

    fun startLevel(level: Int) {
        gameEngine.currentLevel = level
        gameEngine.generateZombiesForLevel(level)
        gameEngine.currentHealth = gameEngine.maxHealth
        gameEngine.currentStamina = gameEngine.maxStamina

        _isGameOver.value = false
        _isVictory.value = false
        _isPaused.value = false
        _isPlaying.value = true

        navigateTo("play")
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                if (!_isPaused.value && !_isGameOver.value && !_isVictory.value) {
                    val currentTime = System.currentTimeMillis()
                    val dt = (currentTime - lastTime) / 1000f
                    lastTime = currentTime

                    // Call Engine Update
                    gameEngine.update(if (dt > 0.1f) 0.016f else dt)

                    // Check Game Over
                    if (gameEngine.currentHealth <= 0f) {
                        _isGameOver.value = true
                        saveGameProgress()
                    }

                    // Check Victory (All zombies cleared in the level)
                    val activeZombies = gameEngine.zombies.count { it.health > 0 }
                    if (activeZombies == 0 && gameEngine.zombies.isNotEmpty()) {
                        _isVictory.value = true
                        // Progress to next level in DB
                        val currentStats = _playerStats.value
                        if (currentStats != null && gameEngine.currentLevel == currentStats.storyProgress) {
                            val nextLevel = currentStats.storyProgress + 1
                            repository.savePlayerStats(
                                currentStats.copy(
                                    storyProgress = nextLevel,
                                    coins = gameEngine.coins,
                                    xp = gameEngine.xp
                                )
                            )
                        }
                        saveGameProgress()
                    }
                } else {
                    lastTime = System.currentTimeMillis()
                }
                delay(16) // ~60FPS loop
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        _isPlaying.value = false
    }

    fun pauseGame() {
        _isPaused.value = true
    }

    fun resumeGame() {
        _isPaused.value = false
    }

    // Persist current score, health, stats, coins to SQLite DB
    fun saveGameProgress() {
        viewModelScope.launch {
            val stats = _playerStats.value ?: return@launch
            val updated = stats.copy(
                coins = gameEngine.coins,
                xp = gameEngine.xp,
                currentHealth = gameEngine.currentHealth,
                currentStamina = gameEngine.currentStamina,
                activeWeapon = gameEngine.activeWeapon.key
            )
            repository.savePlayerStats(updated)
        }
    }

    // Upgrade weapon inside the Shop
    fun buyOrUpgradeWeapon(itemKey: String) {
        viewModelScope.launch {
            val success = repository.buyOrUpgradeWeapon(activeSlotId, itemKey)
            if (success) {
                // Refresh local stats
                val stats = repository.getPlayerStatsSync(activeSlotId)
                if (stats != null) {
                    _playerStats.value = stats
                    gameEngine.coins = stats.coins
                }
            }
        }
    }

    // Select active weapon in player inventory
    fun selectWeapon(weapon: WeaponType) {
        if (gameEngine.unlockedWeapons.contains(weapon)) {
            gameEngine.activeWeapon = weapon
            saveGameProgress()
        }
    }

    // Completely wipe and reset slot
    fun wipeSaveSlot() {
        viewModelScope.launch {
            repository.resetGame(activeSlotId)
            loadSlot(activeSlotId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
