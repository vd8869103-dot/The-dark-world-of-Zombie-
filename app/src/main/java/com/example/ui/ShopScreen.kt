package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.game.WeaponType

@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.playerStats.collectAsState()
    val inventory by viewModel.inventory.collectAsState()

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo("main_menu") },
                        modifier = Modifier.testTag("back_to_menu")
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "VEDIC SHOP",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }

                // Coin Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0x3FFFEB3B), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.MonetizationOn, contentDescription = "Gold Coins", tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${stats?.coins ?: 0}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Weapon list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                val weaponsList = WeaponType.values()
                items(weaponsList) { weapon ->
                    val dbItem = inventory.find { it.itemKey == weapon.key }
                    val isUnlocked = dbItem?.isUnlocked ?: (weapon == WeaponType.SWORD)
                    val level = dbItem?.level ?: 1
                    val dmg = dbItem?.damage ?: weapon.baseDamage
                    val cost = dbItem?.upgradeCost ?: weapon.staminaCost.toInt() * 10

                    ShopWeaponCard(
                        weapon = weapon,
                        isUnlocked = isUnlocked,
                        level = level,
                        damage = dmg,
                        cost = cost,
                        playerCoins = stats?.coins ?: 0
                    ) {
                        viewModel.buyOrUpgradeWeapon(weapon.key)
                    }
                }
            }

            // Back button
            Button(
                onClick = { viewModel.navigateTo("main_menu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RETURN TO MAIN MENU", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun ShopWeaponCard(
    weapon: WeaponType,
    isUnlocked: Boolean,
    level: Int,
    damage: Float,
    cost: Int,
    playerCoins: Int,
    onActionClick: () -> Unit
) {
    val canAfford = playerCoins >= cost
    val accentColor = when (weapon) {
        WeaponType.SWORD -> Color(0xFFECEFF1)
        WeaponType.BOW -> Color(0xFF8D6E63)
        WeaponType.CHAKRA -> Color(0xFF00E5FF)
        WeaponType.DAMRU -> Color(0xFFFFEB3B)
        WeaponType.TRIDENT -> Color(0xFFFFA000)
        WeaponType.MAGIC -> Color(0xFFE040FB)
    }

    val weaponIcon: ImageVector = when (weapon) {
        WeaponType.SWORD -> Icons.Filled.Square // Representing steel
        WeaponType.BOW -> Icons.Filled.Gesture // Represents curvature
        WeaponType.CHAKRA -> Icons.Filled.Lens // Represents wheel
        WeaponType.DAMRU -> Icons.Filled.Audiotrack // Represents sound waves
        WeaponType.TRIDENT -> Icons.Filled.LocationOn // Represents three points
        WeaponType.MAGIC -> Icons.Filled.FlashOn // Represents magic blast
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isUnlocked) Color(0x3FFFFFFF) else accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF130F19)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Weapon Icon & Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(weaponIcon, contentDescription = weapon.displayName, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = weapon.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = weapon.description,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp, end = 10.dp)
                    )

                    // Stats indicators (Dmg & Level)
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "LVL $level",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            modifier = Modifier
                                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            "DMG: ${damage.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                    }
                }
            }

            // Action Button (Buy/Upgrade)
            Button(
                onClick = onActionClick,
                enabled = canAfford,
                modifier = Modifier
                    .width(110.dp)
                    .height(48.dp)
                    .testTag("shop_buy_${weapon.key}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUnlocked) Color(0xFF1565C0) else Color(0xFFC62828),
                    disabledContainerColor = Color(0xFF211D29)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isUnlocked) "UPGRADE" else "UNLOCK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (canAfford) Color.White else Color.DarkGray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = "Cost", tint = if (canAfford) Color(0xFFFFD54F) else Color.DarkGray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$cost",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (canAfford) Color.White else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}
