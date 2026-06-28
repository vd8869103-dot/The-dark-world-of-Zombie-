package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.game.RaycasterView
import com.example.game.WeaponType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PlayScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val engine = viewModel.gameEngine
    val stats by viewModel.playerStats.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()
    val isVictory by viewModel.isVictory.collectAsState()

    // Trigger local recompositions on tick changes
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            tick++
            delay(33) // ~30FPS UI state poll
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. THE 3D RAYCASTING GAME SCREEN RENDERING
        RaycasterView(
            engine = engine,
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD OVERLAYS (Top screen data)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Player HP and SP progress meters
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Health bar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, contentDescription = "HP", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(12.dp)
                                .background(Color(0x3F000000), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFE53935), RoundedCornerShape(4.dp))
                        ) {
                            val percent = engine.currentHealth / engine.maxHealth
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percent.coerceIn(0f, 1f))
                                    .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${engine.currentHealth.toInt()}/${engine.maxHealth.toInt()}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Stamina bar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FlashOn, contentDescription = "SP", tint = Color(0xFFFFEB3B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(12.dp)
                                .background(Color(0x3F000000), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFFFEB3B), RoundedCornerShape(4.dp))
                        ) {
                            val percent = engine.currentStamina / engine.maxStamina
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percent.coerceIn(0f, 1f))
                                    .background(Color(0xFFFFEB3B), RoundedCornerShape(4.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${engine.currentStamina.toInt()}/${engine.maxStamina.toInt()}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // MINIMAP CARD IN TOP RIGHT CORNER
                Card(
                    modifier = Modifier
                        .size(110.dp)
                        .border(1.dp, Color(0xFFCA3B3B).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x7F0E0715))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MiniMap(engine = engine)
                    }
                }
            }

            // LEVEL PROGRESS & ECONOMY ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coins Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0x8F140C1D), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${engine.coins} GOLD", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Black)
                }

                // Zombies Remaining
                val activeZombies = engine.zombies.count { it.health > 0 }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0x8F3E2723), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF5722), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "Zombies", tint = Color(0xFFFF5722), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ZOMBIES: $activeZombies", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Black)
                }

                // Pause Button
                IconButton(
                    onClick = { viewModel.pauseGame() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x7F0E0715), CircleShape)
                        .testTag("pause_game_button")
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause Game", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 3. GAME CONTROLS (Virtual D-pad left, Turn controls right, Strike action bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT SIDE: Virtual D-pad for Movement (Forward, Back, Strafe)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Forward
                    ControlButton(icon = Icons.Filled.KeyboardArrowUp, testTag = "move_forward") {
                        // Move forward relative to look angle
                        val dx = cos(engine.playerAngle)
                        val dy = sin(engine.playerAngle)
                        engine.movePlayer(dx, dy, 0.05f)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Strafe Left
                        ControlButton(icon = Icons.Filled.KeyboardArrowLeft, testTag = "strafe_left") {
                            val dx = cos(engine.playerAngle - Math.PI / 2).toFloat()
                            val dy = sin(engine.playerAngle - Math.PI / 2).toFloat()
                            engine.movePlayer(dx, dy, 0.05f)
                        }

                        // Backward
                        ControlButton(icon = Icons.Filled.KeyboardArrowDown, testTag = "move_backward") {
                            val dx = -cos(engine.playerAngle)
                            val dy = -sin(engine.playerAngle)
                            engine.movePlayer(dx, dy, 0.05f)
                        }

                        // Strafe Right
                        ControlButton(icon = Icons.Filled.KeyboardArrowRight, testTag = "strafe_right") {
                            val dx = cos(engine.playerAngle + Math.PI / 2).toFloat()
                            val dy = sin(engine.playerAngle + Math.PI / 2).toFloat()
                            engine.movePlayer(dx, dy, 0.05f)
                        }
                    }
                }

                // CENTER: WEAPON SELECTION & SLASHER CORE TRIGGER
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Potion Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PotionButton(icon = Icons.Filled.Healing, cost = 30, color = Color(0xFF4CAF50), label = "HP POT") {
                            engine.useHealthPotion()
                        }
                        PotionButton(icon = Icons.Filled.FlashOn, cost = 20, color = Color(0xFFFFEB3B), label = "STAM POT") {
                            engine.useStaminaPotion()
                        }
                    }

                    // Weapon select bar
                    Row(
                        modifier = Modifier
                            .background(Color(0xCC0C0712), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x3FFFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeaponType.values().forEach { weapon ->
                            val isUnlocked = engine.unlockedWeapons.contains(weapon)
                            if (isUnlocked) {
                                val isSelected = engine.activeWeapon == weapon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isSelected) Color(0xFFC62828) else Color(0x3F000000),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White else Color.DarkGray,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectWeapon(weapon) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (weapon) {
                                            WeaponType.SWORD -> Icons.Filled.Square
                                            WeaponType.BOW -> Icons.Filled.Gesture
                                            WeaponType.CHAKRA -> Icons.Filled.Lens
                                            WeaponType.DAMRU -> Icons.Filled.Audiotrack
                                            WeaponType.TRIDENT -> Icons.Filled.LocationOn
                                            WeaponType.MAGIC -> Icons.Filled.FlashOn
                                        },
                                        contentDescription = weapon.displayName,
                                        tint = if (isSelected) Color.White else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT SIDE: Look controls (turn angle) & STRIKE ATTACK ACTION
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Turn left / right buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ControlButton(icon = Icons.Filled.RotateLeft, testTag = "turn_left") {
                            engine.rotatePlayer(-0.1f)
                        }
                        ControlButton(icon = Icons.Filled.RotateRight, testTag = "turn_right") {
                            engine.rotatePlayer(0.1f)
                        }
                    }

                    // HUGE WEAPON STRIKE TRIGGER BUTTON
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(Color(0xFFC62828), CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .clickable { engine.triggerAttack() }
                            .testTag("weapon_strike_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.FlashOn,
                            contentDescription = "STRIKE ATTACK",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // 4. FLOATING OVERLAYS (PAUSE, GAME OVER, VICTORY)
        if (isPaused) {
            GameOverlayDialog(title = "GAME PAUSED", color = Color(0xFF1565C0)) {
                Button(
                    onClick = { viewModel.resumeGame() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("RESUME BATTLE", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.saveGameProgress()
                        viewModel.navigateTo("main_menu")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("SAVE & QUIT")
                }
            }
        }

        if (isGameOver) {
            GameOverlayDialog(title = "HERO DIED", color = Color(0xFFC62828)) {
                Text(
                    text = "The zombies devoured you in the dark city ruins.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { viewModel.startLevel(engine.currentLevel) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("REVIVE & RETRY", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.navigateTo("main_menu") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("MAIN MENU")
                }
            }
        }

        if (isVictory) {
            GameOverlayDialog(title = "VICTORY CLEARED!", color = Color(0xFF2E7D32)) {
                Text(
                    text = "Vedic Warrior! You purged the zombie scourge from this area of the dark city ruins.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        viewModel.startLevel(engine.currentLevel + 1)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("NEXT CHAPTER", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.navigateTo("main_menu") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("MAIN MENU")
                }
            }
        }
    }
}

@Composable
fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(Color(0xB31C1A24), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x3FFFFFFF), RoundedCornerShape(8.dp))
            .clickable { onPress() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun PotionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cost: Int,
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(44.dp)
            .background(Color(0xB31C1A24), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
            Text("$cost G", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GameOverlayDialog(
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.5.dp, color, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF130F19)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                content()
            }
        }
    }
}

// Draw elegant visual minimap of mapGrid
@Composable
fun MiniMap(engine: com.example.game.GameEngine) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val cellW = w / 16f
        val cellH = h / 16f

        // Draw Map Walls
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val cell = engine.mapGrid[y][x]
                if (cell > 0) {
                    drawRect(
                        color = when (cell) {
                            1 -> Color(0xFF5E35B1) // wall
                            2 -> Color(0xFF2E7D32) // zombie nest
                            3 -> Color(0xFF455A64) // iron gate
                            4 -> Color(0xFF00E5FF) // safe beacon
                            else -> Color.DarkGray
                        }.copy(alpha = 0.6f),
                        topLeft = Offset(x * cellW, y * cellH),
                        size = Size(cellW - 0.5f, cellH - 0.5f)
                    )
                }
            }
        }

        // Draw Player location (golden dot) & direction ray
        val px = engine.playerX * cellW
        val py = engine.playerY * cellH
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = 4f,
            center = Offset(px, py)
        )
        // Draw look direction pointer
        val lookX = px + cos(engine.playerAngle) * 12f
        val lookY = py + sin(engine.playerAngle) * 12f
        drawLine(
            color = Color(0xFFFFD54F),
            start = Offset(px, py),
            end = Offset(lookX, lookY),
            strokeWidth = 2f
        )

        // Draw Live Zombies as blinking red dots
        engine.zombies.forEach { zombie ->
            if (zombie.health > 0) {
                drawCircle(
                    color = Color.Red,
                    radius = 3f,
                    center = Offset(zombie.x * cellW, zombie.y * cellH)
                )
            }
        }
    }
}
