package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.GameViewModel

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.playerStats.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0A14))
    ) {
        // 1. Full bleed background horror image
        Image(
            painter = painterResource(id = R.drawable.img_zombie_menu_bg),
            contentDescription = "Dark World of Zombies Key Art Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Dark vignette overlap to make text highly legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0x40000000),
                            Color(0xDF07040B)
                        )
                    )
                )
        )

        // 3. Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "DARK WORLD",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE53935), // Creepy Blood Red
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "OF ZOMBIES",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "THE VEDIC WARRIOR'S SURVIVAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300), // Sacred Gold
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Central Status Indicators
            stats?.let { player ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.dp, Color(0x3FFF1744), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xBE140C1F)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HERO LEVEL", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Lvl ${player.level}", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                        Divider(
                            modifier = Modifier
                                .height(30.dp)
                                .width(1.dp), color = Color(0x3FCA3B3B)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COINS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${player.coins}", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Divider(
                            modifier = Modifier
                                .height(30.dp)
                                .width(1.dp), color = Color(0x3FCA3B3B)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("STORY CHAPTER", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Ch ${player.storyProgress}", fontSize = 18.sp, color = Color(0xFFFF8F00), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PLAY BUTTON (Triggers latest story select)
                MenuButton(
                    text = "START CAMPAIGN",
                    icon = Icons.Filled.PlayArrow,
                    color = Color(0xFFC62828), // High-contrast Red
                    testTag = "start_campaign_button"
                ) {
                    val currentCh = stats?.storyProgress ?: 1
                    viewModel.startLevel(currentCh)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SHOP BUTTON
                    Box(modifier = Modifier.weight(1f)) {
                        MenuButton(
                            text = "SHOP",
                            icon = Icons.Filled.ShoppingCart,
                            color = Color(0xFF2E7D32), // Green
                            testTag = "shop_button"
                        ) {
                            viewModel.navigateTo("shop")
                        }
                    }

                    // PROFILE BUTTON
                    Box(modifier = Modifier.weight(1f)) {
                        MenuButton(
                            text = "PROFILE",
                            icon = Icons.Filled.Person,
                            color = Color(0xFF1565C0), // Blue
                            testTag = "profile_button"
                        ) {
                            viewModel.navigateTo("profile")
                        }
                    }
                }

                // SETTINGS BUTTON
                MenuButton(
                    text = "SETTINGS",
                    icon = Icons.Filled.Settings,
                    color = Color(0xFF424242), // Grey
                    testTag = "settings_button"
                ) {
                    viewModel.navigateTo("settings")
                }
            }
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(54.dp)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color(0xE00C0712))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
    }
}
