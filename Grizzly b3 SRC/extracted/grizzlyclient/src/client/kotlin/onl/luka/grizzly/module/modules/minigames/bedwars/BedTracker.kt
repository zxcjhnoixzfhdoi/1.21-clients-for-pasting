package onl.luka.grizzly.module.modules.minigames.bedwars

import onl.luka.grizzly.config.entry.BooleanEntry
import onl.luka.grizzly.config.entry.IntEntry
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.roundToInt

internal class BedTracker(
    private val settings: Settings,
    private val isEnemy: (Player, Player) -> Boolean,
    private val warning: (Player, Int) -> Unit,
) {
    data class Settings(
        val enabled: BooleanEntry,
        val showDistance: BooleanEntry,
        val enemyAlerts: BooleanEntry,
        val alertDistance: IntEntry,
        val alertCooldown: IntEntry,
        val scanRadius: IntEntry,
    )

    private var levelIdentity: ClientLevel? = null
    private var bedPos: BlockPos? = null
    private var scanOrigin: BlockPos? = null
    private var scanIndex = 0
    private var nearestBed: BlockPos? = null
    private var nearestDistance = Double.MAX_VALUE
    private var matchingBed: BlockPos? = null
    private var matchingDistance = Double.MAX_VALUE
    private var nextScanTick = 0
    private var lastEnemyCheckTick = 0
    private val enemyCooldowns = mutableMapOf<UUID, Long>()
    private val enemiesNearBed = mutableSetOf<UUID>()

    fun tick(client: Minecraft) {
        if (!settings.enabled.value) return
        val player = client.player ?: return
        val level = client.level ?: return

        if (levelIdentity !== level) {
            reset()
            levelIdentity = level
        }

        val tracked = bedPos
        if (tracked != null && level.getBlockState(tracked).block !is BedBlock) {
            bedPos = null
            nextScanTick = player.tickCount + 20
        }

        if (bedPos == null && player.tickCount >= nextScanTick) scanStep(level, player)
        if (settings.enemyAlerts.value && bedPos != null && player.tickCount - lastEnemyCheckTick >= 5) {
            lastEnemyCheckTick = player.tickCount
            checkEnemies(level, player)
        }
    }

    fun hudLine(client: Minecraft): String? {
        if (!settings.enabled.value || !settings.showDistance.value) return null
        val player = client.player ?: return null
        val bed = bedPos ?: return null
        return "Bed: ${player.position().distanceTo(Vec3.atCenterOf(bed)).roundToInt()}m"
    }

    fun hasHudLine(): Boolean = settings.enabled.value && settings.showDistance.value && bedPos != null

    private fun scanStep(level: ClientLevel, player: Player) {
        val currentOrigin = scanOrigin
        if (currentOrigin == null || player.blockPosition().distSqr(currentOrigin) > 64.0) {
            startScan(player.blockPosition())
        }
        val origin = scanOrigin ?: return
        val radius = settings.scanRadius.value
        val side = radius * 2 + 1
        val yRadius = 8
        val layerSize = side * side
        val total = layerSize * (yRadius * 2 + 1)
        val teamColor = teamBedColors(player)

        repeat(SCAN_BUDGET) {
            if (scanIndex >= total) {
                bedPos = matchingBed ?: nearestBed
                nextScanTick = player.tickCount + if (bedPos == null) 100 else 20
                clearScan()
                return
            }

            val index = scanIndex++
            val dy = index / layerSize - yRadius
            val inLayer = index % layerSize
            val dx = inLayer / side - radius
            val dz = inLayer % side - radius
            val pos = origin.offset(dx, dy, dz)
            val state = level.getBlockState(pos)
            val block = state.block as? BedBlock ?: return@repeat
            if (state.getValue(BedBlock.PART) != BedPart.HEAD) return@repeat

            val distance = player.distanceToSqr(Vec3.atCenterOf(pos))
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestBed = pos.immutable()
            }
            if (block.color in teamColor && distance < matchingDistance) {
                matchingDistance = distance
                matchingBed = pos.immutable()
            }
        }
    }

    private fun checkEnemies(level: ClientLevel, local: Player) {
        val bed = bedPos ?: return
        val maxDistance = settings.alertDistance.value
        val maxSqr = maxDistance.toDouble() * maxDistance.toDouble()
        val now = System.currentTimeMillis()
        val cooldown = settings.alertCooldown.value * 1000L
        for (player in level.players()) {
            if (!isEnemy(local, player)) continue
            val distanceSqr = player.distanceToSqr(Vec3.atCenterOf(bed))
            if (distanceSqr > maxSqr) {
                enemiesNearBed.remove(player.uuid)
                continue
            }
            if (player.uuid in enemiesNearBed) continue
            val previous = enemyCooldowns[player.uuid] ?: 0L
            if (now - previous < cooldown) continue
            enemiesNearBed.add(player.uuid)
            enemyCooldowns[player.uuid] = now
            warning(player, kotlin.math.sqrt(distanceSqr).roundToInt())
        }
    }

    private fun teamBedColors(player: Player): Set<DyeColor> {
        val color = player.team?.color?.orElse(null)?.serializedName?.lowercase() ?: return emptySet()
        return when (color) {
            "black" -> setOf(DyeColor.BLACK)
            "dark_blue", "blue" -> setOf(DyeColor.BLUE)
            "dark_green", "green" -> setOf(DyeColor.GREEN, DyeColor.LIME)
            "dark_aqua" -> setOf(DyeColor.CYAN)
            "dark_red", "red" -> setOf(DyeColor.RED)
            "dark_purple" -> setOf(DyeColor.PURPLE, DyeColor.MAGENTA)
            "gold" -> setOf(DyeColor.ORANGE, DyeColor.YELLOW)
            "gray", "dark_gray" -> setOf(DyeColor.GRAY, DyeColor.LIGHT_GRAY)
            "aqua" -> setOf(DyeColor.LIGHT_BLUE, DyeColor.CYAN)
            "light_purple" -> setOf(DyeColor.PINK, DyeColor.MAGENTA)
            "yellow" -> setOf(DyeColor.YELLOW)
            "white" -> setOf(DyeColor.WHITE)
            else -> emptySet()
        }
    }

    private fun startScan(origin: BlockPos) {
        scanOrigin = origin.immutable()
        scanIndex = 0
        nearestBed = null
        nearestDistance = Double.MAX_VALUE
        matchingBed = null
        matchingDistance = Double.MAX_VALUE
    }

    private fun clearScan() {
        scanOrigin = null
        scanIndex = 0
        nearestBed = null
        nearestDistance = Double.MAX_VALUE
        matchingBed = null
        matchingDistance = Double.MAX_VALUE
    }

    fun reset() {
        levelIdentity = null
        bedPos = null
        nextScanTick = 0
        lastEnemyCheckTick = 0
        enemyCooldowns.clear()
        enemiesNearBed.clear()
        clearScan()
    }

    private companion object {
        const val SCAN_BUDGET = 640
    }
}
