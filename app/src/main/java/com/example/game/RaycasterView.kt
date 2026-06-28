package com.example.game

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun RaycasterView(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070509)) // Dark ambient horror black
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Apply screen shake offsets
        val shakeX = engine.screenShakeOffset.x
        val shakeY = engine.screenShakeOffset.y

        // Draw Ceiling & Floor gradients (ambient lighting)
        drawCeilingAndFloor(width, height, centerY, shakeY)

        // RAYCASTING WALLS
        val numColumns = 120 // Good balance of fidelity and speed
        val columnWidth = width / numColumns.toFloat()
        val fov = (60f * PI / 180f).toFloat() // 60 degree Field of View

        // To store wall distances for sprite depth-sorting (Z-buffer)
        val zBuffer = FloatArray(numColumns) { 16.0f }

        for (col in 0 until numColumns) {
            val relativeAngle = (col.toFloat() / numColumns.toFloat() - 0.5f) * fov
            val rayAngle = engine.playerAngle + relativeAngle

            // Digital Differential Analysis/Stepwise casting
            var distance = 0f
            val step = 0.04f
            var hitWall = 0
            var wallX = 0f
            var wallY = 0f

            val cosA = cos(rayAngle)
            val sinA = sin(rayAngle)

            while (distance < 16f) {
                distance += step
                val rx = engine.playerX + cosA * distance
                val ry = engine.playerY + sinA * distance

                val gx = rx.toInt()
                val gy = ry.toInt()

                if (gx in 0 until engine.mapWidth && gy in 0 until engine.mapHeight) {
                    val cell = engine.mapGrid[gy][gx]
                    if (cell > 0) {
                        hitWall = cell
                        wallX = rx
                        wallY = ry
                        break
                    }
                } else {
                    break // Out of bounds
                }
            }

            // Correct fisheye distortion
            val correctedDistance = distance * cos(relativeAngle)
            zBuffer[col] = correctedDistance

            // Calculate height of wall projection
            val wallScale = 0.85f // adjustment factor
            val projectedHeight = (height / correctedDistance) * wallScale

            // Draw wall slice
            val top = centerY - projectedHeight / 2f + shakeY
            val bottom = centerY + projectedHeight / 2f + shakeY

            // Ambient Fog/Shading factor (deeper darkness at distance)
            val maxFog = 12f
            val shade = max(0f, 1f - correctedDistance / maxFog)

            // Dynamic colors based on wall type
            val wallColor = when (hitWall) {
                1 -> Color(0xFF5E35B1) // Ancient Purple Ruined Fortress Wall
                2 -> Color(0xFF1B5E20) // Glowing Green Zombie Breeding Nest
                3 -> Color(0xFF37474F) // Creepy Steel Gate
                4 -> Color(0xFF00E5FF) // Safe Beacon Gate
                else -> Color(0xFF263238)
            }

            // Dark shadow/side lighting depending on horizontal vs vertical hits
            val isVertical = abs(wallX - wallX.roundToInt()) < abs(wallY - wallY.roundToInt())
            val sideIntensity = if (isVertical) 0.8f else 1.0f
            val finalColor = Color(
                red = wallColor.red * shade * sideIntensity,
                green = wallColor.green * shade * sideIntensity,
                blue = wallColor.blue * shade * sideIntensity,
                alpha = 1.0f
            )

            // Draw wall column slice
            drawRect(
                color = finalColor,
                topLeft = Offset(col * columnWidth + shakeX, top),
                size = Size(columnWidth + 0.5f, projectedHeight)
            )

            // Draw brick texture highlights or zombie nest organic details
            if (shade > 0.3f) {
                if (hitWall == 2) {
                    // Draw organic glowing slime spots
                    val spotY = top + (projectedHeight * 0.4f)
                    val spotSize = projectedHeight * 0.15f
                    drawCircle(
                        color = Color(0xFF00FF00).copy(alpha = 0.5f * shade),
                        radius = spotSize / 2f,
                        center = Offset(col * columnWidth + columnWidth / 2f + shakeX, spotY)
                    )
                } else if (hitWall == 4) {
                    // Safe zone holy blue energy lines
                    drawLine(
                        color = Color(0xFF80DEEA).copy(alpha = shade),
                        start = Offset(col * columnWidth + shakeX, top),
                        end = Offset(col * columnWidth + shakeX, bottom),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // SPRITES RENDERING (Zombies)
        val sortedZombies = engine.zombies
            .filter { it.health > 0 }
            .map { zombie ->
                val dx = zombie.x - engine.playerX
                val dy = zombie.y - engine.playerY
                val dist = sqrt(dx * dx + dy * dy)
                zombie to dist
            }
            .sortedByDescending { it.second } // Painter's Algorithm: Draw back-to-front

        sortedZombies.forEach { (zombie, dist) ->
            if (dist < 0.2f || dist > 14f) return@forEach // too close or too far away

            // Angle of sprite relative to player look angle
            val dx = zombie.x - engine.playerX
            val dy = zombie.y - engine.playerY
            val spriteAngle = atan2(dy, dx)

            var diffAngle = spriteAngle - engine.playerAngle
            while (diffAngle < -PI) diffAngle += (2 * PI).toFloat()
            while (diffAngle > PI) diffAngle -= (2 * PI).toFloat()

            // Is it within screen Field of View (plus some tolerance on sides)?
            val fovTolerance = 0.8f
            if (abs(diffAngle) < (fov / 2f) + fovTolerance) {
                // Projection calculation
                val screenX = (width / 2f) + (tan(diffAngle) * (width / 2f))

                // Scale sprite height and width based on distance
                val spriteScale = zombie.type.scale
                val spriteHeight = (height / dist) * spriteScale * 0.8f
                val spriteWidth = spriteHeight * 0.8f

                val spriteTop = centerY - spriteHeight / 2f + shakeY + (sin(zombie.sizeBobbing) * 8f) // idle bobbing
                val spriteLeft = screenX - spriteWidth / 2f

                // Z-buffer occlusion check: only draw column if not blocked by a wall
                val colIndex = (screenX / columnWidth).toInt()
                if (colIndex in 0 until numColumns && dist > zBuffer[colIndex]) {
                    // Blocked by a closer wall, skip or clip!
                    // In a simple system, we just skip drawing if center is occluded
                    return@forEach
                }

                // Shading factor for sprites
                val shade = max(0f, 1f - dist / 12f)
                val zombieColor = Color(android.graphics.Color.parseColor(zombie.type.colorHex))

                // DRAW THE ZOMBIE SPRITE ON CANVAS
                drawZombieSprite(
                    zombie = zombie,
                    left = spriteLeft + shakeX,
                    top = spriteTop,
                    width = spriteWidth,
                    height = spriteHeight,
                    color = zombieColor,
                    shade = shade
                )
            }
        }

        // PROJECTILES RENDERING
        engine.projectiles.forEach { proj ->
            val dx = proj.x - engine.playerX
            val dy = proj.y - engine.playerY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > 0.3f && dist < 14f) {
                val projAngle = atan2(dy, dx)
                var diffAngle = projAngle - engine.playerAngle
                while (diffAngle < -PI) diffAngle += (2 * PI).toFloat()
                while (diffAngle > PI) diffAngle -= (2 * PI).toFloat()

                if (abs(diffAngle) < (fov / 2f) + 0.5f) {
                    val screenX = (width / 2f) + (tan(diffAngle) * (width / 2f))
                    val pSize = (height / dist) * 0.12f

                    val colIndex = (screenX / columnWidth).toInt()
                    if (colIndex in 0 until numColumns && dist > zBuffer[colIndex]) return@forEach

                    // Projectile specific colors
                    val pColor = when (proj.weaponType) {
                        WeaponType.BOW -> Color(0xFFFFD54F) // Spiritual Golden Arrow
                        WeaponType.CHAKRA -> Color(0xFF00E5FF) // Blue Glowing Chakra
                        WeaponType.MAGIC -> Color(0xFFE040FB) // Violet Astra Fireball
                        else -> Color.White
                    }

                    // Draw projectile as glowing circular sphere
                    drawCircle(
                        color = pColor,
                        radius = pSize / 2f,
                        center = Offset(screenX + shakeX, centerY + shakeY)
                    )
                    // Draw outer energy ring
                    drawCircle(
                        color = pColor.copy(alpha = 0.4f),
                        radius = pSize * 0.9f,
                        center = Offset(screenX + shakeX, centerY + shakeY),
                        style = Stroke(width = pSize * 0.15f)
                    )
                }
            }
        }

        // PARTICLES RENDERING
        engine.particles.forEach { part ->
            val dx = part.x - engine.playerX
            val dy = part.y - engine.playerY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > 0.2f && dist < 14f) {
                val pAngle = atan2(dy, dx)
                var diffAngle = pAngle - engine.playerAngle
                while (diffAngle < -PI) diffAngle += (2 * PI).toFloat()
                while (diffAngle > PI) diffAngle -= (2 * PI).toFloat()

                if (abs(diffAngle) < (fov / 2f) + 0.3f) {
                    val screenX = (width / 2f) + (tan(diffAngle) * (width / 2f))

                    val colIndex = (screenX / columnWidth).toInt()
                    if (colIndex in 0 until numColumns && dist > zBuffer[colIndex]) return@forEach

                    val verticalOffset = (part.z * (height / dist) * 0.5f)
                    val pSize = (part.size * (height / dist) * 0.02f) * part.life

                    drawCircle(
                        color = Color(part.color).copy(alpha = part.life),
                        radius = max(2f, pSize / 2f),
                        center = Offset(screenX + shakeX, centerY - verticalOffset + shakeY)
                    )
                }
            }
        }

        // PLAYER HUD WEAPON OVERLAY (Render current held weapon)
        drawPlayerWeapon(
            weaponType = engine.activeWeapon,
            width = width,
            height = height,
            swingAnimation = engine.weaponSwingAnimation,
            damruVibration = engine.damruVibration
        )

        // BLOOD OVERLAY ON DAMAGE
        if (engine.bloodOverlaySplash > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF720000).copy(alpha = engine.bloodOverlaySplash)),
                    center = Offset(width / 2f, height / 2f),
                    radius = width * 0.8f
                ),
                size = size
            )
        }
    }
}

// Draw realistic sky (with fog) and deep horror floor
private fun DrawScope.drawCeilingAndFloor(width: Float, height: Float, centerY: Float, shakeY: Float) {
    // Upper Ceiling: Dark gradient from black down to horror indigo
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF030105), Color(0xFF140B1E)),
            startY = 0f,
            endY = centerY + shakeY
        ),
        topLeft = Offset(0f, 0f),
        size = Size(width, centerY + shakeY)
    )

    // Floor: Indigo/grey stone tile down to total darkness
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D0A14), Color(0xFF040206)),
            startY = centerY + shakeY,
            endY = height
        ),
        topLeft = Offset(0f, centerY + shakeY),
        size = Size(width, height - (centerY + shakeY))
    )

    // Draw horizon dust fog line
    drawLine(
        color = Color(0xFF1A1225).copy(alpha = 0.5f),
        start = Offset(0f, centerY + shakeY),
        end = Offset(width, centerY + shakeY),
        strokeWidth = 3f
    )
}

// Renders a high-quality stylized Zombie sprite using basic geometry & styling
private fun DrawScope.drawZombieSprite(
    zombie: Zombie,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color,
    shade: Float
) {
    val alpha = shade
    val headRadius = width * 0.22f
    val headCenterX = left + width / 2f
    val headCenterY = top + headRadius + 10f

    // Draw Zombie Body (Ripped Cloak)
    val bodyPath = Path().apply {
        moveTo(headCenterX - headRadius * 1.5f, top + height * 0.75f)
        lineTo(headCenterX - headRadius * 0.8f, headCenterY + headRadius * 0.8f)
        lineTo(headCenterX + headRadius * 0.8f, headCenterY + headRadius * 0.8f)
        lineTo(headCenterX + headRadius * 1.5f, top + height * 0.75f)
        lineTo(headCenterX + headRadius * 1.8f, top + height)
        lineTo(headCenterX - headRadius * 1.8f, top + height)
        close()
    }
    drawPath(
        path = bodyPath,
        color = Color(0xFF37474F).copy(alpha = alpha), // moldy dark green/grey cloak
    )

    // Draw Zombie Head (Glowing rotting skin)
    drawCircle(
        color = Color(0xFF43A047).copy(alpha = alpha),
        radius = headRadius,
        center = Offset(headCenterX, headCenterY)
    )

    // Glowing creepy Red Eyes
    val eyeOffset = headRadius * 0.35f
    val eyeSize = headRadius * 0.2f
    drawCircle(
        color = Color.Red.copy(alpha = alpha),
        radius = eyeSize,
        center = Offset(headCenterX - eyeOffset, headCenterY - eyeOffset)
    )
    drawCircle(
        color = Color.Red.copy(alpha = alpha),
        radius = eyeSize,
        center = Offset(headCenterX + eyeOffset, headCenterY - eyeOffset)
    )

    // Ripped rotten arms reaching for player
    val armWidth = headRadius * 0.4f
    val armLength = width * 0.4f
    val armY = headCenterY + headRadius * 1.1f

    // Left arm extending forward
    drawRoundRect(
        color = Color(0xFF2E7D32).copy(alpha = alpha),
        topLeft = Offset(headCenterX - headRadius * 1.5f - armLength / 2f, armY),
        size = Size(armLength, armWidth),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Right arm
    drawRoundRect(
        color = Color(0xFF2E7D32).copy(alpha = alpha),
        topLeft = Offset(headCenterX + headRadius * 0.5f, armY),
        size = Size(armLength, armWidth),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Boss Crown / Accessories if it is a Boss Zombie
    if (zombie.type == ZombieType.BOSS_NECRO) {
        // Draw Purple Magical crown
        val crownPath = Path().apply {
            moveTo(headCenterX - headRadius * 0.9f, headCenterY - headRadius * 0.8f)
            lineTo(headCenterX - headRadius * 0.6f, headCenterY - headRadius * 1.6f)
            lineTo(headCenterX - headRadius * 0.2f, headCenterY - headRadius * 1.1f)
            lineTo(headCenterX, headCenterY - headRadius * 1.9f)
            lineTo(headCenterX + headRadius * 0.2f, headCenterY - headRadius * 1.1f)
            lineTo(headCenterX + headRadius * 0.6f, headCenterY - headRadius * 1.6f)
            lineTo(headCenterX + headRadius * 0.9f, headCenterY - headRadius * 0.8f)
            close()
        }
        drawPath(path = crownPath, color = Color(0xFF8E24AA).copy(alpha = alpha))
    } else if (zombie.type == ZombieType.BOSS_TITAN) {
        // Massive stone armor / spikes
        drawRect(
            color = Color(0xFF4E342E).copy(alpha = alpha),
            topLeft = Offset(headCenterX - headRadius * 1.4f, headCenterY - headRadius * 1.4f),
            size = Size(headRadius * 2.8f, headRadius * 0.5f)
        )
    }

    // Health Bar above zombie
    if (zombie.health > 0) {
        val barW = width * 0.7f
        val barH = max(4f, height * 0.03f)
        val barL = headCenterX - barW / 2f
        val barT = top - 15f

        // Dark background bar
        drawRect(
            color = Color(0x7F212121),
            topLeft = Offset(barL, barT),
            size = Size(barW, barH)
        )
        // Red filled health
        val healthPercent = zombie.health / zombie.type.maxHealth
        drawRect(
            color = if (zombie.type == ZombieType.BOSS_TITAN || zombie.type == ZombieType.BOSS_NECRO) Color(0xFFFFEB3B) else Color(0xFFFF3D00),
            topLeft = Offset(barL, barT),
            size = Size(barW * healthPercent, barH)
        )
    }
}

// Renders player's held weapon models with rich dynamic overlays (e.g. glowing energy, swinging paths)
private fun DrawScope.drawPlayerWeapon(
    weaponType: WeaponType,
    width: Float,
    height: Float,
    swingAnimation: Float, // ranges from 1.0 down to 0f on strike
    damruVibration: Float
) {
    val weaponCenterX = width / 2f
    val weaponBottomY = height

    when (weaponType) {
        WeaponType.SWORD -> {
            // Vedic Iron Sword model
            // Animate swing (moving x and rotating angle)
            val swingOffset = swingAnimation * 200f
            val rotationAngle = swingAnimation * 60f // degrees

            val swordX = weaponCenterX + 120f - swingOffset
            val swordY = weaponBottomY - 100f + (swingAnimation * 100f)

            // Draw Sword Blade with glowing light-blue edge
            val swordPath = Path().apply {
                moveTo(swordX - 15f, swordY) // Hilt top
                lineTo(swordX + 25f - (swingAnimation * 30f), swordY - 450f) // Blade tip
                lineTo(swordX + 45f, swordY) // Right side
                close()
            }
            drawPath(
                path = swordPath,
                color = Color(0xFFECEFF1) // Polished Iron Blade
            )
            // Glowing sword edge
            drawPath(
                path = swordPath,
                color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                style = Stroke(width = 4f)
            )

            // Sword Guard & Hilt
            drawRoundRect(
                color = Color(0xFFFFD54F), // Gold Guard
                topLeft = Offset(swordX - 45f, swordY - 15f),
                size = Size(110f, 25f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRect(
                color = Color(0xFF5D4037), // Leather Handle
                topLeft = Offset(swordX + 2f, swordY),
                size = Size(20f, 90f)
            )

            // Draw swinging blue slashing trail when attacking
            if (swingAnimation > 0.05f) {
                val trailPath = Path().apply {
                    moveTo(swordX - 250f, swordY - 200f)
                    quadraticTo(
                        swordX, swordY - 350f,
                        swordX + 150f, swordY - 150f
                    )
                }
                drawPath(
                    path = trailPath,
                    color = Color(0xFFE0F7FA).copy(alpha = swingAnimation),
                    style = Stroke(width = 30f * swingAnimation, cap = StrokeCap.Round)
                )
            }
        }

        WeaponType.TRIDENT -> {
            // Trishula (Trident) divine three-pronged spear
            // Thrusts directly forward/upward on swing
            val thrustOffset = swingAnimation * 250f
            val tridX = weaponCenterX
            val tridY = weaponBottomY - 150f - thrustOffset

            // Draw gold staff
            drawRect(
                color = Color(0xFFD84315),
                topLeft = Offset(tridX - 10f, tridY),
                size = Size(20f, 500f)
            )

            // Draw Gold trident body
            val tridentBody = Path().apply {
                moveTo(tridX - 40f, tridY - 50f)
                quadraticTo(tridX, tridY, tridX + 40f, tridY - 50f)
                lineTo(tridX + 12f, tridY)
                lineTo(tridX - 12f, tridY)
                close()
            }
            drawPath(path = tridentBody, color = Color(0xFFFFC107))

            // Center long spike
            val centerProng = Path().apply {
                moveTo(tridX - 8f, tridY - 45f)
                lineTo(tridX, tridY - 220f)
                lineTo(tridX + 8f, tridY - 45f)
                close()
            }
            drawPath(path = centerProng, color = Color(0xFFFFD54F))

            // Left prong spike
            val leftProng = Path().apply {
                moveTo(tridX - 42f, tridY - 45f)
                lineTo(tridX - 52f, tridY - 160f)
                lineTo(tridX - 30f, tridY - 45f)
                close()
            }
            drawPath(path = leftProng, color = Color(0xFFFFA000))

            // Right prong spike
            val rightProng = Path().apply {
                moveTo(tridX + 30f, tridY - 45f)
                lineTo(tridX + 52f, tridY - 160f)
                lineTo(tridX + 42f, tridY - 45f)
                close()
            }
            drawPath(path = rightProng, color = Color(0xFFFFA000))

            // Sacred golden halo energy around trident
            drawCircle(
                color = Color(0xFFFFEB3B).copy(alpha = 0.25f + 0.3f * swingAnimation),
                radius = 110f + swingAnimation * 60f,
                center = Offset(tridX, tridY - 100f)
            )
        }

        WeaponType.BOW -> {
            // Gandiva Bow
            // Pulling animation
            val pullAmount = swingAnimation * 70f
            val bowX = weaponCenterX
            val bowY = weaponBottomY - 150f

            // Draw curved wooden bow structure
            val bowPath = Path().apply {
                moveTo(bowX - 180f, bowY)
                quadraticTo(bowX, bowY - 100f, bowX + 180f, bowY)
            }
            drawPath(
                path = bowPath,
                color = Color(0xFF8D6E63),
                style = Stroke(width = 15f)
            )

            // Draw string
            val stringPath = Path().apply {
                moveTo(bowX - 180f, bowY)
                lineTo(bowX, bowY + 80f - pullAmount)
                lineTo(bowX + 180f, bowY)
            }
            drawPath(
                path = stringPath,
                color = Color(0xFFEEEEEE),
                style = Stroke(width = 2.5f)
            )

            // Draw Golden Arrow pointing forward
            val arrowY = bowY + 80f - pullAmount
            val arrowPath = Path().apply {
                moveTo(bowX, arrowY)
                lineTo(bowX, arrowY - 280f)
            }
            drawPath(
                path = arrowPath,
                color = Color(0xFFFFD54F),
                style = Stroke(width = 6f)
            )

            // Arrow Head
            val tipPath = Path().apply {
                moveTo(bowX - 12f, arrowY - 265f)
                lineTo(bowX, arrowY - 305f)
                lineTo(bowX + 12f, arrowY - 265f)
                close()
            }
            drawPath(path = tipPath, color = Color(0xFFFF9100))
        }

        WeaponType.CHAKRA -> {
            // Sudarshana Chakra: Spinning cosmic gold/blue disc
            val angleOfSpin = (System.currentTimeMillis() % 1000) / 1000f * 2 * PI.toFloat()
            val chakraRadius = 140f + (swingAnimation * 50f)
            val chakX = weaponCenterX
            val chakY = weaponBottomY - 140f

            // Outer rings
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                radius = chakraRadius * 1.2f,
                center = Offset(chakX, chakY),
                style = Stroke(width = 12f)
            )
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = chakraRadius,
                center = Offset(chakX, chakY),
                style = Stroke(width = 16f)
            )

            // Draw 6 spinning spikes
            for (i in 0 until 6) {
                val currentSpikeAngle = angleOfSpin + (i * PI.toFloat() / 3f)
                val sx = chakX + cos(currentSpikeAngle) * chakraRadius
                val sy = chakY + sin(currentSpikeAngle) * chakraRadius

                val tipX = sx + cos(currentSpikeAngle) * 45f
                val tipY = sy + sin(currentSpikeAngle) * 45f

                val spikePath = Path().apply {
                    moveTo(sx - sin(currentSpikeAngle) * 15f, sy + cos(currentSpikeAngle) * 15f)
                    lineTo(tipX, tipY)
                    lineTo(sx + sin(currentSpikeAngle) * 15f, sy - cos(currentSpikeAngle) * 15f)
                    close()
                }
                drawPath(path = spikePath, color = Color(0xFFFF9100))
            }

            // Inner glowing core
            drawCircle(
                color = Color.White,
                radius = chakraRadius * 0.4f,
                center = Offset(chakX, chakY)
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                radius = chakraRadius * 0.7f,
                center = Offset(chakX, chakY)
            )
        }

        WeaponType.DAMRU -> {
            // Shiva's Sacred Damru drum
            // Vibrates strongly when active
            val vibrationX = sin(damruVibration * 45f) * 20f
            val vibrationY = cos(damruVibration * 45f) * 15f

            val damruX = weaponCenterX + vibrationX
            val damruY = weaponBottomY - 170f + vibrationY

            // Left/Right cone drum shells (gourds)
            val leftCone = Path().apply {
                moveTo(damruX - 85f, damruY - 65f)
                lineTo(damruX, damruY)
                lineTo(damruX - 85f, damruY + 65f)
                close()
            }
            drawPath(path = leftCone, color = Color(0xFF8D6E63)) // Wood shell

            val rightCone = Path().apply {
                moveTo(damruX + 85f, damruY - 65f)
                lineTo(damruX, damruY)
                lineTo(damruX + 85f, damruY + 65f)
                close()
            }
            drawPath(path = rightCone, color = Color(0xFF8D6E63))

            // Drum leather skin highlights
            drawOval(
                color = Color(0xFFFFECB3),
                topLeft = Offset(damruX - 95f, damruY - 65f),
                size = Size(20f, 130f)
            )
            drawOval(
                color = Color(0xFFFFECB3),
                topLeft = Offset(damruX + 75f, damruY - 65f),
                size = Size(20f, 130f)
            )

            // Golden middle ring binding the cords
            drawCircle(
                color = Color(0xFFFFC107),
                radius = 15f,
                center = Offset(damruX, damruY)
            )

            // Cords & Hanging Beads that swing around
            val swingBeadOffset = sin(System.currentTimeMillis() * 0.012f) * 50f
            drawLine(
                color = Color.Black,
                start = Offset(damruX, damruY),
                end = Offset(damruX - swingBeadOffset, damruY + 110f),
                strokeWidth = 3f
            )
            drawCircle(
                color = Color(0xFFD50000), // Rudraksha / red bead
                radius = 12f,
                center = Offset(damruX - swingBeadOffset, damruY + 110f)
            )

            // Golden Sound Waves emitting from Damru when hit
            if (damruVibration > 0.05f) {
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = damruVibration),
                    radius = 140f + (1f - damruVibration) * 300f,
                    center = Offset(damruX, damruY),
                    style = Stroke(width = 8f)
                )
            }
        }

        WeaponType.MAGIC -> {
            // Astra Magic Powers (Swirling glowing orbs around hands)
            val time = System.currentTimeMillis() * 0.005f
            val pulse = abs(sin(time)) * 15f

            val leftHandX = weaponCenterX - 220f
            val rightHandX = weaponCenterX + 220f
            val handY = weaponBottomY - 120f

            // Draw glowing circles (fireballs in hands)
            val purpleFlame = Color(0xFFE040FB)
            val pinkFlame = Color(0xFFFF4081)

            // Left magic flame
            drawCircle(
                color = purpleFlame.copy(alpha = 0.2f),
                radius = 70f + pulse,
                center = Offset(leftHandX, handY)
            )
            drawCircle(
                color = pinkFlame.copy(alpha = 0.6f),
                radius = 45f + pulse * 0.6f,
                center = Offset(leftHandX, handY)
            )
            drawCircle(
                color = Color.White,
                radius = 20f,
                center = Offset(leftHandX, handY)
            )

            // Right magic flame
            drawCircle(
                color = purpleFlame.copy(alpha = 0.2f),
                radius = 70f + pulse,
                center = Offset(rightHandX, handY)
            )
            drawCircle(
                color = pinkFlame.copy(alpha = 0.6f),
                radius = 45f + pulse * 0.6f,
                center = Offset(rightHandX, handY)
            )
            drawCircle(
                color = Color.White,
                radius = 20f,
                center = Offset(rightHandX, handY)
            )

            // Swirling elemental energy lines
            for (i in 0 until 4) {
                val offsetAngle = time + (i * PI / 2f)
                val lineX = leftHandX + cos(offsetAngle) * 55f
                val lineY = handY + sin(offsetAngle) * 35f
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 4f,
                    center = Offset(lineX.toFloat(), lineY.toFloat())
                )
            }
        }
    }
}
