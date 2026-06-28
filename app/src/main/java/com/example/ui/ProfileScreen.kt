package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel

@Composable
fun ProfileScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.playerStats.collectAsState()
    var selectedSlot by remember { mutableStateOf(1) }
    var showResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07040B))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("main_menu") },
                    modifier = Modifier.testTag("back_to_menu")
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HERO PROFILE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }

            // Save Slots & Stats Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Save Slots Selector
                Text(
                    "SAVE GAME SLOTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(1, 2, 3).forEach { slot ->
                        val isActive = selectedSlot == slot
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isActive) Color(0xFF1565C0) else Color(0x3F1565C0),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) Color(0x281565C0) else Color(0xFF130F19))
                                .clickable {
                                    selectedSlot = slot
                                    viewModel.loadSlot(slot)
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SLOT 0$slot",
                                    color = if (isActive) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isActive) "ACTIVE" else "LOAD SAVE",
                                    color = if (isActive) Color(0xFF00E5FF) else Color.DarkGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Stats Dashboard Card
                stats?.let { player ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF130F19)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(player.playerName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Level ${player.level} Vedic Warrior", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFF8F00).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFFF8F00), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("STORY CHAPTER ${player.storyProgress}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF8F00))
                                }
                            }

                            Divider(color = Color(0x12FFFFFF))

                            // Stats Grid items
                            StatRow(label = "MAX HEALTH POINTS", value = "${player.maxHealth.toInt()} HP", icon = Icons.Filled.Favorite, iconColor = Color(0xFFE53935))
                            StatRow(label = "MAX STAMINA POINTS", value = "${player.maxStamina.toInt()} SP", icon = Icons.Filled.FlashOn, iconColor = Color(0xFFFFEB3B))
                            StatRow(label = "VARYING COINS EARNED", value = "${player.coins} GOLD", icon = Icons.Filled.MonetizationOn, iconColor = Color(0xFFFFD54F))
                            StatRow(label = "TOTAL XP EARNED", value = "${player.xp} / ${player.level * 100} XP", icon = Icons.Filled.Stars, iconColor = Color(0xFF00E5FF))
                        }
                    }
                }
            }

            // Wipe / Danger zone section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("wipe_save_button"),
                    border = BorderStroke(1.dp, Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DELETE & WIPE SLOT", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Button(
                    onClick = { viewModel.navigateTo("main_menu") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("BACK TO MAIN MENU", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }

    // Confirmation Wipe Save Slot dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("WIPE SLOT PROGRESS?", fontWeight = FontWeight.Black, color = Color.White) },
            text = { Text("This will completely reset SLOT 0$selectedSlot and wipe all level story progression, inventory unlocks, coins, and levels permanently! Do you proceed?", color = Color.LightGray) },
            containerColor = Color(0xFF181420),
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.wipeSaveSlot()
                        showResetDialog = false
                    }
                ) {
                    Text("WIPE DATA", color = Color(0xFFC62828), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
        Text(value, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
    }
}
