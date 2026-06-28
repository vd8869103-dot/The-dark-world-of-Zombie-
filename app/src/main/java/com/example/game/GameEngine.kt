package com.example.game

import android.graphics.PointF
import java.util.UUID
import kotlin.math.*

// Represents a weapon type with its unique visual and combat properties
enum class WeaponType(
    val key: String,
    val displayName: String,
    val baseDamage: Float,
    val range: Float,
    val staminaCost: Float,
    val description: String
) {
    SWORD("sword", "Vedic Iron Sword", 25f, 1.5f, 15f, "A close-range steel blade that slashes zombies with swift clean strikes."),
    BOW("bow", "Gandiva Bow", 20f, 8.0f, 10f, "A sacred bows that launches long-range piercing spiritual arrows."),
    CHAKRA("chakra", "Sudarshana Chakra", 35f, 5.0f, 20f, "A spinning cosmic disc that richochets and cuts through multiple zombies."),
    DAMRU("damru", "Shiva's Damru", 45f, 4.0f, 30f, "A holy drum emitting divine resonance waves damaging all zombies around."),
    TRIDENT("trident", "Trishula (Trident)", 65f, 2.5f, 35f, "An almighty three-pronged spear that thrusts forward dealing immense critical damage."),
    MAGIC("magic", "Astra Magic Powers", 50f, 6.0f, 25f, "Summons elemental purple fireballs that explode and burn targets.")
}

// Represents different types of zombies
enum class ZombieType(
    val displayName: String,
    val maxHealth: Float,
    val speed: Float,
    val damage: Float,
    val scoreReward: Int,
    val coinReward: Int,
    val scale: Float,
    val colorHex: String
) {
    WALKER("Crawler Zombie", 40f, 0.5f, 10f, 20, 10, 0.8f, "#4CAF50"),
    RUNNER("Furious Runner", 30f, 1.2f, 15f, 30, 15, 0.7f, "#FF5722"),
    BOSS_NECRO("Necromancer Boss", 200f, 0.4f, 25f, 150, 80, 1.3f, "#9C27B0"),
    BOSS_TITAN("Titan Golem Boss", 350f, 0.3f, 40f, 300, 150, 1.6f, "#E91E63")
}

// Representation of a live zombie in the game
data class Zombie(
    val id: String = UUID.randomUUID().toString(),
    val type: ZombieType,
    var x: Float,
    var y: Float,
    var health: Float,
    var isAlert: Boolean = false,
    var isAttacking: Boolean = false,
    var damageCooldown: Long = 0,
    var speedMultiplier: Float = 1.0f,
    var sizeBobbing: Float = 0f
)

// Dynamic visual effects in the game world (e.g. blood, magic sparkles, shockwaves)
data class GameParticle(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var z: Float = 0f, // vertical offset
    val dx: Float,
    val dy: Float,
    val dz: Float = 0f,
    val color: Long,
    val size: Float,
    var life: Float, // 1.0 down to 0.0
    val decay: Float = 0.05f
)

// Flying projectles (Arrows, Chakras, Fireballs)
data class Projectile(
    val id: String = UUID.randomUUID().toString(),
    val weaponType: WeaponType,
    var x: Float,
    var y: Float,
    val dx: Float,
    val dy: Float,
    val damage: Float,
    var life: Float = 1.0f
)

class GameEngine {
    // 16x16 Game Map Grid representing the dark city ruins
    // 0 = empty space, 1 = crumbling wall, 2 = metal barrier, 3 = zombie nest, 4 = safe beacon
    val mapWidth = 16
    val mapHeight = 16
    var currentLevel = 1

    val mapGrid = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 3, 3, 0, 0, 1, 0, 2, 2, 0, 0, 3, 3, 0, 1),
        intArrayOf(1, 0, 3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 1),
        intArrayOf(1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1),
        intArrayOf(1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1),
        intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1),
        intArrayOf(1, 0, 3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 1),
        intArrayOf(1, 0, 3, 3, 0, 0, 1, 0, 2, 2, 0, 0, 3, 3, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    // Player State
    var playerX = 8.5f
    var playerY = 8.5f
    var playerAngle = 0.0f // Looking directly East (in radians)

    var currentHealth = 100f
    var maxHealth = 100f
    var currentStamina = 100f
    var maxStamina = 100f

    // Score & Economy
    var coins = 0
    var xp = 0
    var score = 0
    var zombiesKilled = 0

    // Weapons
    var activeWeapon = WeaponType.SWORD
    var unlockedWeapons = mutableSetOf(WeaponType.SWORD)
    var weaponLevels = mutableMapOf<WeaponType, Int>().apply {
        WeaponType.values().forEach { put(it, 1) }
    }

    // Weapon Action states
    var attackCooldown = 0f
    var weaponSwingAnimation = 0f // 0 to 1 progress
    var damruVibration = 0f // screenshake magnitude
    var bowDraw = 0f // arrow pull back progress
    var magicCharge = 0f // glowing aura progress
    var isShieldActive = false
    var shieldEnergy = 100f

    // Live Actors & Projectiles
    val zombies = mutableListOf<Zombie>()
    val projectiles = mutableListOf<Projectile>()
    val particles = mutableListOf<GameParticle>()

    // Feedback signals for View
    var bloodOverlaySplash = 0f // damage red overlay alpha
    var lastHitDirectionAngle = 0f // for flash alerts
    var screenShakeOffset = PointF(0f, 0f)

    init {
        generateZombiesForLevel(1)
    }

    fun generateZombiesForLevel(level: Int) {
        currentLevel = level
        zombies.clear()
        projectiles.clear()
        particles.clear()

        // Reset player pos to center safe zone
        playerX = 8.5f
        playerY = 8.5f
        playerAngle = -PI.toFloat() / 2f // facing up

        val countMultiplier = 5 + level * 3
        var spawned = 0
        val random = java.util.Random()

        while (spawned < countMultiplier) {
            val rx = random.nextInt(14) + 1
            val ry = random.nextInt(14) + 1

            // Don't spawn inside walls or inside player starting safe area (7,7 to 9,9)
            if (mapGrid[ry][rx] == 0 && (rx < 6 || rx > 10 || ry < 6 || ry > 10)) {
                val roll = random.nextFloat()
                val type = when {
                    level >= 3 && roll < 0.10f -> ZombieType.BOSS_TITAN
                    level >= 2 && roll < 0.20f -> ZombieType.BOSS_NECRO
                    roll < 0.40f -> ZombieType.RUNNER
                    else -> ZombieType.WALKER
                }
                zombies.add(Zombie(type = type, x = rx + 0.5f, y = ry + 0.5f, health = type.maxHealth))
                spawned++
            }
        }

        // Always ensure at least one boss zombie from Level 2 onwards to create epic fights
        if (level >= 2) {
            val bossType = if (level % 2 == 0) ZombieType.BOSS_TITAN else ZombieType.BOSS_NECRO
            zombies.add(Zombie(type = bossType, x = 2.5f, y = 2.5f, health = bossType.maxHealth))
        }
    }

    // Update game state frame by frame (dt is in seconds)
    fun update(dt: Float) {
        // Regrow Stamina slowly
        if (currentStamina < maxStamina) {
            currentStamina = min(maxStamina, currentStamina + 15f * dt)
        }

        // Cool down overlays and actions
        if (bloodOverlaySplash > 0f) {
            bloodOverlaySplash = max(0f, bloodOverlaySplash - 2.5f * dt)
        }
        if (attackCooldown > 0f) {
            attackCooldown = max(0f, attackCooldown - dt)
        }
        if (weaponSwingAnimation > 0f) {
            weaponSwingAnimation = max(0f, weaponSwingAnimation - 4.0f * dt)
        }
        if (damruVibration > 0f) {
            damruVibration = max(0f, damruVibration - 3.0f * dt)
        }

        // Apply decay to screen shake
        screenShakeOffset.x *= 0.8f
        screenShakeOffset.y *= 0.8f

        // 1. Update Projectiles
        val projIterator = projectiles.iterator()
        while (projIterator.hasNext()) {
            val proj = projIterator.next()
            proj.x += proj.dx * 12f * dt
            proj.y += proj.dy * 12f * dt

            // Check grid boundary & wall hit
            val gx = proj.x.toInt()
            val gy = proj.y.toInt()
            if (gx < 0 || gx >= mapWidth || gy < 0 || gy >= mapHeight || mapGrid[gy][gx] != 0) {
                // Spawn small wall hit particle
                spawnSparkles(proj.x, proj.y, 0xFF795548, 5)
                projIterator.remove()
                continue
            }

            // Check zombie hit
            var hit = false
            for (zombie in zombies) {
                if (zombie.health > 0) {
                    val distSq = (zombie.x - proj.x).pow(2) + (zombie.y - proj.y).pow(2)
                    if (distSq < 0.25f) { // hit radius
                        damageZombie(zombie, proj.damage)
                        hit = true
                        break
                    }
                }
            }

            if (hit) {
                projIterator.remove()
            }
        }

        // 2. Update Zombie AI
        zombies.forEach { zombie ->
            if (zombie.health <= 0) return@forEach

            zombie.sizeBobbing += dt * 5f

            val dx = playerX - zombie.x
            val dy = playerY - zombie.y
            val dist = sqrt(dx * dx + dy * dy)

            // Alert radius
            if (dist < 7.5f) {
                zombie.isAlert = true
            }

            if (zombie.isAlert) {
                // Move towards player
                val moveSpeed = zombie.type.speed * zombie.speedMultiplier * dt
                val dirX = dx / dist
                val dirY = dy / dist

                val nextX = zombie.x + dirX * moveSpeed
                val nextY = zombie.y + dirY * moveSpeed

                // Collision with walls
                if (mapGrid[zombie.y.toInt()][nextX.toInt()] == 0) {
                    zombie.x = nextX
                }
                if (mapGrid[nextY.toInt()][zombie.x.toInt()] == 0) {
                    zombie.y = nextY
                }

                // Melee strike player if super close
                if (dist < 0.6f) {
                    zombie.isAttacking = true
                    val now = System.currentTimeMillis()
                    if (now - zombie.damageCooldown > 1500) {
                        damagePlayer(zombie.type.damage)
                        zombie.damageCooldown = now
                        // Screen flash and recoil shake
                        bloodOverlaySplash = 0.6f
                        screenShakeOffset.x = (Math.random() * 20 - 10).toFloat()
                        screenShakeOffset.y = (Math.random() * 20 - 10).toFloat()
                    }
                } else {
                    zombie.isAttacking = false
                }
            }
        }

        // 3. Update Particles
        val partIterator = particles.iterator()
        while (partIterator.hasNext()) {
            val part = partIterator.next()
            part.life -= part.decay
            if (part.life <= 0f) {
                partIterator.remove()
            } else {
                part.x += part.dx * dt
                part.y += part.dy * dt
                part.z += part.dz * dt
            }
        }
    }

    // Damage player, reduce health, handle shield barrier absorption
    fun damagePlayer(amount: Float) {
        if (isShieldActive && shieldEnergy > 10f) {
            shieldEnergy = max(0f, shieldEnergy - amount * 0.8f)
            spawnSparkles(playerX, playerY, 0xFF00E5FF, 6)
        } else {
            currentHealth = max(0f, currentHealth - amount)
            spawnSparkles(playerX, playerY, 0xFFFF0000, 8)
        }
    }

    // Inflict damage to a single zombie actor
    fun damageZombie(zombie: Zombie, damage: Float) {
        zombie.health = max(0f, zombie.health - damage)
        zombie.isAlert = true // Wakes up zombie

        // Spawn hit blood splash particles
        val colorHex = if (zombie.type == ZombieType.BOSS_TITAN || zombie.type == ZombieType.BOSS_NECRO) 0xFF4CAF50 else 0xFFFF1744
        spawnSparkles(zombie.x, zombie.y, colorHex, 10)

        // Deal blow-back push slightly
        val angle = atan2(zombie.y - playerY, zombie.x - playerX)
        val pushDist = 0.3f
        val newX = zombie.x + cos(angle) * pushDist
        val newY = zombie.y + sin(angle) * pushDist
        if (mapGrid[zombie.y.toInt()][newX.toInt()] == 0) zombie.x = newX
        if (mapGrid[newY.toInt()][zombie.x.toInt()] == 0) zombie.y = newY

        if (zombie.health <= 0f) {
            // Zombie dead!
            score += zombie.type.scoreReward
            coins += zombie.type.coinReward
            xp += zombie.type.scoreReward / 2
            zombiesKilled++

            // Splatter explosion
            spawnSparkles(zombie.x, zombie.y, colorHex, 20)
        }
    }

    // Trigger active weapon strike
    fun triggerAttack(): Boolean {
        if (attackCooldown > 0f) return false

        val cost = activeWeapon.staminaCost
        if (currentStamina < cost) return false // not enough stamina!

        currentStamina -= cost
        attackCooldown = when (activeWeapon) {
            WeaponType.SWORD -> 0.4f
            WeaponType.BOW -> 0.7f
            WeaponType.CHAKRA -> 0.6f
            WeaponType.DAMRU -> 1.0f
            WeaponType.TRIDENT -> 0.8f
            WeaponType.MAGIC -> 0.5f
        }

        // Trigger action animation states
        weaponSwingAnimation = 1.0f

        val dmgMult = 1.0f + 0.15f * (weaponLevels[activeWeapon] ?: 1)
        val damageDealt = activeWeapon.baseDamage * dmgMult

        when (activeWeapon) {
            WeaponType.SWORD -> {
                // Deal short-range wide sweeping damage
                strikeMeleeArc(45f, activeWeapon.range, damageDealt)
            }
            WeaponType.TRIDENT -> {
                // Thrust forward narrow piercing line damage
                strikeMeleeArc(15f, activeWeapon.range, damageDealt)
                screenShakeOffset.x = cos(playerAngle) * 15f
                screenShakeOffset.y = sin(playerAngle) * 15f
            }
            WeaponType.BOW -> {
                // Launch long range linear projectile
                val dx = cos(playerAngle)
                val dy = sin(playerAngle)
                projectiles.add(Projectile(weaponType = WeaponType.BOW, x = playerX, y = playerY, dx = dx, dy = dy, damage = damageDealt))
            }
            WeaponType.CHAKRA -> {
                // Launches spinning projectile that travels out and pierces
                val dx = cos(playerAngle)
                val dy = sin(playerAngle)
                projectiles.add(Projectile(weaponType = WeaponType.CHAKRA, x = playerX, y = playerY, dx = dx, dy = dy, damage = damageDealt))
            }
            WeaponType.MAGIC -> {
                // Cast elemental purple blast projectile
                val dx = cos(playerAngle)
                val dy = sin(playerAngle)
                projectiles.add(Projectile(weaponType = WeaponType.MAGIC, x = playerX, y = playerY, dx = dx, dy = dy, damage = damageDealt))
            }
            WeaponType.DAMRU -> {
                // Resonance shockwave hitting all nearby zombies in 360 circle
                damruVibration = 1.0f
                screenShakeOffset.x = (Math.random() * 25 - 12).toFloat()
                screenShakeOffset.y = (Math.random() * 25 - 12).toFloat()

                zombies.forEach { zombie ->
                    if (zombie.health > 0) {
                        val dx = zombie.x - playerX
                        val dy = zombie.y - playerY
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist <= activeWeapon.range) {
                            damageZombie(zombie, damageDealt)
                        }
                    }
                }
                // Sparkle ring around the player
                for (i in 0 until 18) {
                    val pAngle = (i * 20) * PI / 180f
                    val dx = cos(pAngle).toFloat() * 2f
                    val dy = sin(pAngle).toFloat() * 2f
                    particles.add(GameParticle(x = playerX, y = playerY, dx = dx, dy = dy, color = 0xFFFFD700, size = 12f, life = 1.0f, decay = 0.08f))
                }
            }
        }
        return true
    }

    private fun strikeMeleeArc(fovDegrees: Float, range: Float, damage: Float) {
        val fovRad = fovDegrees * PI / 180f
        zombies.forEach { zombie ->
            if (zombie.health <= 0) return@forEach

            val dx = zombie.x - playerX
            val dy = zombie.y - playerY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist <= range) {
                val zAngle = atan2(dy, dx)
                // Normalize angle difference to [-PI, PI]
                var diff = zAngle - playerAngle
                while (diff < -PI) diff += (2 * PI).toFloat()
                while (diff > PI) diff -= (2 * PI).toFloat()

                if (abs(diff) <= fovRad / 2) {
                    damageZombie(zombie, damage)
                }
            }
        }
    }

    private fun spawnSparkles(x: Float, y: Float, color: Long, count: Int) {
        val random = java.util.Random()
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2 * PI.toFloat()
            val speed = random.nextFloat() * 3f + 1f
            particles.add(
                GameParticle(
                    x = x,
                    y = y,
                    dx = cos(angle) * speed,
                    dy = sin(angle) * speed,
                    color = color,
                    size = random.nextFloat() * 10f + 4f,
                    life = 1.0f,
                    decay = random.nextFloat() * 0.05f + 0.03f
                )
            )
        }
    }

    // Moves the player, checking wall collisions properly
    fun movePlayer(moveX: Float, moveY: Float, dt: Float) {
        // Speed scaling
        val speed = 3.5f * dt
        val nextX = playerX + moveX * speed
        val nextY = playerY + moveY * speed

        // Sliding collision checks with some margin
        val margin = 0.25f
        val checkX = if (moveX > 0) nextX + margin else nextX - margin
        val checkY = if (moveY > 0) nextY + margin else nextY - margin

        if (mapGrid[playerY.toInt()][checkX.toInt()] == 0) {
            playerX = nextX
        }
        if (mapGrid[checkY.toInt()][playerX.toInt()] == 0) {
            playerY = nextY
        }
    }

    // Rotate player viewing angle
    fun rotatePlayer(angleOffset: Float) {
        playerAngle += angleOffset
        // wrap angle between -PI and PI
        while (playerAngle < -PI) playerAngle += (2 * PI).toFloat()
        while (playerAngle > PI) playerAngle -= (2 * PI).toFloat()
    }

    // Drink health potion, returning true if successful
    fun useHealthPotion(): Boolean {
        if (coins >= 30 && currentHealth < maxHealth) {
            coins -= 30
            currentHealth = min(maxHealth, currentHealth + 40f)
            spawnSparkles(playerX, playerY, 0xFF4CAF50, 15)
            return true
        }
        return false
    }

    // Restore stamina potion
    fun useStaminaPotion(): Boolean {
        if (coins >= 20 && currentStamina < maxStamina) {
            coins -= 20
            currentStamina = min(maxStamina, currentStamina + 50f)
            spawnSparkles(playerX, playerY, 0xFFFFEB3B, 15)
            return true
        }
        return false
    }
}
