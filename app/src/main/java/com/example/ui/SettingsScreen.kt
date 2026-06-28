package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun SettingsScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val soundEnabled by viewModel.isSoundEnabled.collectAsState()
    val difficulty by viewModel.difficultyLevel.collectAsState()
    val graphics by viewModel.graphicDetails.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07040B)) // Deep dark violet black
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
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }

            // Setting Options list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Sound Settings Card
                SettingsCard(title = "AUDIO", icon = Icons.Filled.VolumeUp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mute Game Audio", color = Color.LightGray, fontSize = 14.sp)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { viewModel.isSoundEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFC62828),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF1E1C24)
                            )
                        )
                    }
                }

                // 2. Difficulty Selection Card
                SettingsCard(title = "SURVIVAL DIFFICULTY", icon = Icons.Filled.VolumeUp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Easy", "Normal", "Hard").forEach { mode ->
                            val isSelected = difficulty == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFC62828) else Color(0x3FCA3B3B),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFC62828) else Color(0x7F17131F))
                                    .clickable { viewModel.difficultyLevel.value = mode }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.uppercase(),
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Graphic Details Settings
                SettingsCard(title = "GRAPHICS QUALITY", icon = Icons.Filled.GraphicEq) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Medium", "High", "Ultra").forEach { level ->
                            val isSelected = graphics == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF1565C0) else Color(0x3F1565C0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF1565C0) else Color(0x7F17131F))
                                    .clickable { viewModel.graphicDetails.value = level }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level.uppercase(),
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Return to Main Menu
            Button(
                onClick = { viewModel.navigateTo("main_menu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "SAVE & RETURN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x23FFFFFF), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF130F19)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.LightGray,
                    letterSpacing = 1.sp
                )
            }
            content()
        }
    }
}
