package onl.luka.grizzly.module.modules.skyblock

import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

object DojoHelper : Module(
    "Dojo Helper",
    "Ports the Control, Discipline, Force, and Mastery helpers",
    Category.SKYBLOCK,
) {
    private const val ROTATION_OWNER = "dojo-helper"
    private val DEFAULT_DOJO_SPAWN = BlockPos(-207, 100, -598)

    private enum class Challenge {
        NONE,
        CONTROL,
        DISCIPLINE,
        FORCE,
        MASTERY,
        SWIFTNESS,
        TENACITY,
    }

    private data class DojoState(
        val inArena: Boolean,
        val challenge: Challenge,
    )

    private val onlyInDojo = boolean("only in dojo", true)
    private val disciplineSwap = boolean("discipline sword swap", true)
    private val swapDelay = int("discipline swap delay ms", 50, 0, 175)
    private val forceESP = boolean("force target esp", true)
    private val blockWrongForceHits = boolean("block wrong force hits", true)
    private val controlAimbot = boolean("control aimbot", false)
    private val controlPrediction = float("control prediction", 2f, 0f, 3f)
    private val masteryESP = boolean("mastery target esp", true)
    private val masteryLock = boolean("mastery lock", true)
    private val masteryGreen = boolean("mastery aim at green", false)
    private val masteryYellow = boolean("mastery aim at yellow", true)
    private val masteryRed = boolean("mastery aim at red", true)
    private val rotationSpeed = float("rotation speed", 35f, 1f, 180f)

    private val forceColor = color("force color", Color(100, 100, 255, 180))
    private val masteryColor = color("mastery color", Color(255, 210, 50, 190))

    private val boxes = mutableListOf<SkyBlockBox>()
    private val masteryPillars = mutableListOf<BlockPos>()
    private var nextSwapAt = 0L
    private var activeChallenge = Challenge.NONE
    private var currentSpawn = DEFAULT_DOJO_SPAWN
    private var masteryPillarsDirty = true
    private var masteryLockedPos: BlockPos? = null
    private var controlTarget: WitherSkeleton? = null

    init {
        AttackEntityCallback.EVENT.register { _, _, _, entity, _ ->
            if (shouldBlockTarget(entity)) InteractionResult.FAIL else InteractionResult.PASS
        }
    }

    override fun onDisabled() {
        activeChallenge = Challenge.NONE
        boxes.clear()
        masteryLockedPos = null
        controlTarget = null
        RotationManager.clearRotation(ROTATION_OWNER)
    }

    @JvmStatic
    fun onPlayerPositioned(client: Minecraft) {
        val level = client.level ?: return
        val player = client.player ?: return
        if (SkyBlockUtils.scoreboardLines(client).none { it.contains("Dojo", ignoreCase = true) }) return

        val spawn = player.blockPosition()
        val floor = spawn.below()
        if (!level.hasChunkAt(floor.x, floor.z) ||
            !level.getBlockState(floor).`is`(Blocks.CHISELED_STONE_BRICKS)
        ) {
            return
        }

        currentSpawn = spawn.immutable()
        masteryPillarsDirty = true
        masteryLockedPos = null
    }

    override fun onTick(client: Minecraft) {
        val level = client.level ?: return
        val player = client.player ?: return

        val state = readDojoState(client)
        val challenge = state.challenge.takeIf { !onlyInDojo.value || state.inArena } ?: Challenge.NONE
        if (challenge == Challenge.NONE) {
            activeChallenge = Challenge.NONE
            boxes.clear()
            RotationManager.clearRotation(ROTATION_OWNER)
            return
        }
        if (challenge != activeChallenge) {
            activeChallenge = challenge
            boxes.clear()
            masteryLockedPos = null
            controlTarget = null
        }

        val controlAimTarget = if (challenge == Challenge.CONTROL && controlAimbot.value) {
            findControlTarget(client)
        } else {
            controlTarget = null
            null
        }

        if (challenge == Challenge.MASTERY && masteryPillarsDirty) {
            masteryPillarsDirty = !scanMasteryPillars(client)
        }
        val masteryTarget = if (challenge == Challenge.MASTERY && masteryLock.value) {
            masteryTarget(client)
        } else {
            masteryLockedPos = null
            null
        }
        val aim = controlAimTarget?.let(::predictedSkeletonPoint) ?: masteryTarget?.let(Vec3::atCenterOf)
        if (aim != null) {
            val rotation = SkyBlockUtils.rotationTo(player, aim)
            val pitch = if (masteryTarget != null) rotation.second - 3f else rotation.second
            SkyBlockUtils.preparePerspectiveRotation()
            RotationManager.setTargetRotation(rotation.first, pitch, ROTATION_OWNER)
            RotationManager.quickTick(if (controlAimTarget != null) 50f else rotationSpeed.value)
        } else {
            RotationManager.clearRotation(ROTATION_OWNER)
        }

        val next = mutableListOf<SkyBlockBox>()
        if (challenge == Challenge.MASTERY && masteryESP.value) {
            masteryPillars
                .asSequence()
                .filter { isSelectedMasteryWool(level.getBlockState(it).block) }
                .forEach { next += SkyBlockBox(AABB(it).deflate(0.004), masteryColor.value) }
        }
        boxes.clear()
        boxes += next
    }

    override fun onLevelRender(ctx: LevelRenderContext) {
        if (activeChallenge == Challenge.DISCIPLINE && disciplineSwap.value) {
            updateDisciplineSword(Minecraft.getInstance())
        }

        val renderBoxes = boxes.toMutableList()
        if (activeChallenge == Challenge.FORCE && forceESP.value) {
            val client = Minecraft.getInstance()
            val level = client.level
            if (level != null) {
                val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(true)
                level.entitiesForRendering()
                    .filterIsInstance<Zombie>()
                    .filter(::isCorrectForceTarget)
                    .forEach { entity ->
                        renderBoxes += SkyBlockBox(
                            interpolatedBoundingBox(entity, partialTick),
                            forceColor.value,
                        )
                    }
            }
        }
        renderSkyBlockBoxes(ctx, renderBoxes, 1.5f)
    }

    fun shouldBlockTarget(entity: Entity): Boolean {
        if (!isEnabled() || !blockWrongForceHits.value || entity !is Zombie) return false
        val state = readDojoState(Minecraft.getInstance())
        if (onlyInDojo.value && !state.inArena) return false
        return state.challenge == Challenge.FORCE && !isCorrectForceTarget(entity)
    }

    private fun isCorrectForceTarget(entity: Zombie): Boolean =
        entity.isAlive && entity.getItemBySlot(EquipmentSlot.HEAD).item != Items.LEATHER_HELMET

    private fun interpolatedBoundingBox(entity: Entity, partialTick: Float): AABB {
        val x = entity.xOld + (entity.x - entity.xOld) * partialTick
        val y = entity.yOld + (entity.y - entity.yOld) * partialTick
        val z = entity.zOld + (entity.z - entity.zOld) * partialTick
        return entity.boundingBox.move(x - entity.x, y - entity.y, z - entity.z)
    }

    private fun readDojoState(client: Minecraft): DojoState {
        val lines = SkyBlockUtils.scoreboardLines(client).map(::normalizeScoreboardText)
        val inArena = lines.any { it.contains("dojo", ignoreCase = true) }
        val challengeIndexes = lines.indices.filter {
            lines[it].contains("challenge", ignoreCase = true)
        }
        val challengeText = challengeIndexes
            .flatMap { index ->
                val start = (index - 2).coerceAtLeast(0)
                val end = (index + 2).coerceAtMost(lines.lastIndex)
                lines.subList(start, end + 1)
            }
            .joinToString(" ")
        val challenge = Challenge.entries.firstOrNull { candidate ->
            candidate != Challenge.NONE &&
                challengeText.contains(candidate.name, ignoreCase = true)
        } ?: Challenge.NONE
        return DojoState(inArena, challenge)
    }

    private fun normalizeScoreboardText(value: String): String =
        value
            .replace(LEGACY_FORMATTING, "")
            .replace('\u00A0', ' ')
            .replace(WHITESPACE, " ")
            .trim()

    private fun swordForHelmet(zombie: Zombie) =
        when (zombie.getItemBySlot(EquipmentSlot.HEAD).item) {
            Items.DIAMOND_HELMET -> Items.DIAMOND_SWORD
            Items.GOLDEN_HELMET -> Items.GOLDEN_SWORD
            Items.IRON_HELMET -> Items.IRON_SWORD
            Items.LEATHER_HELMET -> Items.WOODEN_SWORD
            else -> null
        }

    private fun updateDisciplineSword(client: Minecraft) {
        val player = client.player ?: return
        val now = System.currentTimeMillis()
        if (now < nextSwapAt) return

        val target = (client.hitResult as? EntityHitResult)?.entity ?: client.crosshairPickEntity
        if (target !is Zombie) return
        val sword = swordForHelmet(target) ?: return
        nextSwapAt = now + swapDelay.value

        val slot = (0..8).firstOrNull { player.inventory.getItem(it).item == sword } ?: return
        if (player.inventory.selectedSlot != slot) {
            player.inventory.setSelectedSlot(slot)
        }
    }

    private fun findControlTarget(client: Minecraft): WitherSkeleton? {
        val level = client.level ?: return null
        val player = client.player ?: return null

        controlTarget?.let { current ->
            if (isValidControlTarget(current)) return current
        }

        controlTarget = level.entitiesForRendering()
            .filterIsInstance<WitherSkeleton>()
            .filter(::isValidControlTarget)
            .minByOrNull(player::distanceToSqr)
        return controlTarget
    }

    private fun isValidControlTarget(entity: WitherSkeleton): Boolean =
        entity.isAlive &&
            !entity.isInvisible &&
            entity.x >= translatedX(-224) &&
            entity.x <= translatedX(-190) &&
            entity.z >= translatedZ(-615) &&
            entity.z <= translatedZ(-581)

    private fun predictedSkeletonPoint(entity: LivingEntity): Vec3 {
        val radians = Math.toRadians(entity.yRot.toDouble())
        return entity.position().add(
            -sin(radians) * controlPrediction.value,
            1.5,
            cos(radians) * controlPrediction.value,
        )
    }

    private fun masteryTarget(client: Minecraft): BlockPos? {
        val level = client.level ?: return null
        masteryLockedPos?.let { locked ->
            if (isSelectedMasteryWool(level.getBlockState(locked).block)) return locked
        }

        masteryLockedPos = masteryPillars.firstOrNull {
            isSelectedMasteryWool(level.getBlockState(it).block)
        }
        return masteryLockedPos
    }

    private fun isSelectedMasteryWool(block: net.minecraft.world.level.block.Block): Boolean =
        when (block) {
            Blocks.WOOL.lime() -> masteryGreen.value
            Blocks.WOOL.yellow() -> masteryYellow.value
            Blocks.WOOL.red() -> masteryRed.value
            else -> false
        }

    private fun scanMasteryPillars(client: Minecraft): Boolean {
        val level = client.level ?: return false
        val discovered = linkedSetOf<BlockPos>()
        for (x in -223..-192) {
            for (y in 98..106) {
                for (z in -614..-582) {
                    val block = dojoPos(x, y, z)
                    if (!level.hasChunkAt(block.x, block.z)) return false
                    if (!level.getBlockState(block).`is`(BlockTags.LOGS)) continue

                    var top = block
                    while (level.getBlockState(top.above()).`is`(BlockTags.LOGS)) {
                        top = top.above()
                    }
                    discovered += top.above().immutable()
                }
            }
        }

        masteryPillars.clear()
        masteryPillars += discovered
        return true
    }

    private fun dojoPos(
        x: Int,
        y: Int,
        z: Int,
        spawn: BlockPos = currentSpawn,
    ): BlockPos =
        BlockPos(
            x - DEFAULT_DOJO_SPAWN.x + spawn.x,
            y - DEFAULT_DOJO_SPAWN.y + spawn.y,
            z - DEFAULT_DOJO_SPAWN.z + spawn.z,
        )

    private fun translatedX(canonicalX: Int): Double =
        (canonicalX - DEFAULT_DOJO_SPAWN.x + currentSpawn.x).toDouble()

    private fun translatedZ(canonicalZ: Int): Double =
        (canonicalZ - DEFAULT_DOJO_SPAWN.z + currentSpawn.z).toDouble()

    private val LEGACY_FORMATTING = Regex("\u00A7.")
    private val WHITESPACE = Regex("\\s+")
}
