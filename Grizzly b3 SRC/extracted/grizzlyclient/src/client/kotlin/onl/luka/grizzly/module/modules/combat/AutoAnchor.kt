package onl.luka.grizzly.module.modules.combat

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RespawnAnchorBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.gui.helpers.itemCategories
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.NotificationManager
import onl.luka.grizzly.util.RotationManager
import java.util.Locale
import java.util.Random
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

object AutoAnchor : Module(
    name = "Auto Anchor",
    description = "Places, charges and detonates a respawn anchor",
    category = Category.COMBAT,
) {
    enum class Mode { ON_BIND, ON_PLACE }

    private enum class Stage {
        IDLE,
        FINDING_ITEMS,
        PLACING_ANCHOR,
        WAITING_FOR_ANCHOR,
        PLACING_SHIELD,
        CHARGING_ANCHOR,
        SWAPPING_ITEM,
        DETONATING,
        FINISH,
    }

    val mode = enum("mode", Mode.ON_BIND)
    val doubleAnchor = boolean("double anchor", false)
    val safeAnchor = boolean("safe anchor", false)
    val explosionItemSwap = boolean("explosion item swap", false)
    val explosionItems = itemList(
        "Explosion Item",
        listOf("totem_of_undying"),
        defaultMode = ItemListEntry.Mode.WHITELIST,
    ).also { it.visibleWhen = { explosionItemSwap.value } }
    val aimAssist = boolean("aim assist", true)
    val aimSpeed = float("aim speed", 12.0f, 1.0f, 15.0f).also {
        it.visibleWhen = { aimAssist.value }
    }
    val delay = intRange("delay (ms)", 50 to 100, 0, 500)

    private const val ROTATION_OWNER = "auto_anchor"
    private const val MAX_RETRIES = 4
    private const val MAX_LOOK_RETRIES = 20
    private const val SHIELD_MIN_COVERAGE = 0.15f

    private var stage = Stage.IDLE
    private var anchorPos: BlockPos? = null
    private var baseBlockPos: BlockPos? = null
    private var hitFacing: Direction? = null

    private var anchorSlot = -1
    private var glowstoneSlot = -1
    private var previousSlot = -1

    private var retries = 0
    private var waitRetries = 0
    private var chargeRetries = 0
    private var anchorPlaced = false
    private var doubleAnchorDone = false
    private var nextActionTime = 0L

    private var shieldStarted = false
    private var shieldPlaced = false
    private var shieldStep = 0
    private var shieldRetries = 0
    private var shieldBlock: BlockPos? = null
    private var shieldSupport: BlockPos? = null
    private var shieldFacing: Direction? = null

    override fun onEnabled() {
        resetState()
        stage = if (mode.value == Mode.ON_PLACE) Stage.IDLE else Stage.FINDING_ITEMS
    }

    override fun onDisabled() {
        releaseRotation()
        restorePreviousSlot()
        resetState()
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        val level = client.level ?: return

        if (stage == Stage.IDLE) {
            if (mode.value == Mode.ON_BIND) {
                disable()
                return
            }
            watchForManualAnchor(client, level)
            return
        }

        if (!aimAssist.value) releaseRotation()

        when (stage) {
            Stage.FINDING_ITEMS -> findItems(player, level)
            Stage.PLACING_ANCHOR -> placeAnchor(client, player, level)
            Stage.WAITING_FOR_ANCHOR -> waitForAnchor(level)
            Stage.PLACING_SHIELD -> placeShield(client, player, level)
            Stage.CHARGING_ANCHOR -> chargeAnchor(client, player, level)
            Stage.SWAPPING_ITEM -> swapToExplosionItem(client, player, level)
            Stage.DETONATING -> detonate(client, player, level)
            Stage.FINISH -> finishSequence()
            Stage.IDLE -> Unit
        }
    }

    private fun watchForManualAnchor(client: Minecraft, level: ClientLevel) {
        val hit = client.hitResult as? BlockHitResult ?: return
        if (hit.type != HitResult.Type.BLOCK) return
        if (!level.getBlockState(hit.blockPos).`is`(Blocks.RESPAWN_ANCHOR)) return
        if (chargeOf(level, hit.blockPos) >= RespawnAnchorBlock.MAX_CHARGES) return

        anchorPos = hit.blockPos
        baseBlockPos = hit.blockPos
        stage = Stage.FINDING_ITEMS
    }

    private fun findItems(player: LocalPlayer, level: ClientLevel) {
        glowstoneSlot = findHotbarSlot(player) { it.item === Items.GLOWSTONE }
        anchorSlot = findHotbarSlot(player) { it.item === Items.RESPAWN_ANCHOR }

        if (glowstoneSlot < 0) {
            NotificationManager.showError(name, "No glowstone in hotbar")
            finishSequence()
            return
        }
        val placedAlready = anchorPos?.let { isAnchorAt(level, it) } == true
        if (anchorSlot < 0 && !placedAlready) {
            NotificationManager.showError(name, "No respawn anchor in hotbar")
            finishSequence()
            return
        }

        previousSlot = player.inventory.selectedSlot
        retries = 0
        resetActionTimer()
        stage = if (placedAlready) chargeEntryStage() else Stage.PLACING_ANCHOR
    }

    private fun placeAnchor(client: Minecraft, player: LocalPlayer, level: ClientLevel) {
        if (anchorPos == null || baseBlockPos == null || hitFacing == null) {
            val hit = client.hitResult as? BlockHitResult
            if (hit != null && hit.type == HitResult.Type.BLOCK &&
                level.getBlockState(hit.blockPos).`is`(Blocks.RESPAWN_ANCHOR)
            ) {
                anchorPos = hit.blockPos
                baseBlockPos = hit.blockPos
                retries = 0
                resetActionTimer()
                stage = chargeEntryStage()
                return
            }
            if (hit == null || hit.type != HitResult.Type.BLOCK) {
                if (++retries > MAX_LOOK_RETRIES) stage = Stage.FINISH
                return
            }
            baseBlockPos = hit.blockPos
            hitFacing = hit.direction
            anchorPos = hit.blockPos.relative(hit.direction)
            retries = 0
        }

        aimAt(player, topCenterOf(baseBlockPos!!))

        if (!anchorPlaced && !isLookingAtPlacement(client)) {
            if (++retries > MAX_LOOK_RETRIES) stage = Stage.FINISH
            return
        }

        if (!anchorPlaced) {
            player.inventory.selectedSlot = anchorSlot
            useOnLookedBlock(client, player)
            anchorPlaced = true
            waitRetries = 0
            return
        }

        if (!isAnchorAt(level, anchorPos!!)) {
            if (++waitRetries <= MAX_RETRIES) return
            anchorPlaced = false
            waitRetries = 0
            if (++chargeRetries <= MAX_RETRIES) return
            stage = Stage.FINISH
            return
        }

        anchorPlaced = false
        waitRetries = 0
        chargeRetries = 0
        retries = 0
        resetActionTimer()
        stage = chargeEntryStage()
    }

    private fun waitForAnchor(level: ClientLevel) {
        val pos = anchorPos
        if (pos != null && isAnchorAt(level, pos)) {
            retries = 0
            resetActionTimer()
            stage = chargeEntryStage()
            return
        }
        if (++waitRetries > MAX_RETRIES) finishSequence()
    }

    private fun chargeAnchor(client: Minecraft, player: LocalPlayer, level: ClientLevel) {
        aimAt(player, anchorAimPoint(player, level))
        if (System.currentTimeMillis() < nextActionTime) return
        if (!isLookingAtAnchor(client, level)) {
            if (++retries > MAX_RETRIES) stage = Stage.FINISH
            return
        }

        player.inventory.selectedSlot = glowstoneSlot
        useOnLookedBlock(client, player)
        retries = 0
        resetActionTimer()
        stage = Stage.SWAPPING_ITEM
    }

    private fun swapToExplosionItem(client: Minecraft, player: LocalPlayer, level: ClientLevel) {
        aimAt(player, anchorAimPoint(player, level))
        if (System.currentTimeMillis() < nextActionTime) return
        if (!isLookingAtAnchor(client, level)) {
            if (++retries > MAX_RETRIES) stage = Stage.FINISH
            return
        }

        player.inventory.selectedSlot = explosionSlot(player)
        retries = 0
        resetActionTimer()
        stage = Stage.DETONATING
    }

    private fun detonate(client: Minecraft, player: LocalPlayer, level: ClientLevel) {
        aimAt(player, anchorAimPoint(player, level))
        if (System.currentTimeMillis() < nextActionTime) return
        if (!isLookingAtAnchor(client, level)) {
            if (++retries > MAX_RETRIES) stage = Stage.FINISH
            return
        }

        useOnLookedBlock(client, player)

        if (doubleAnchor.value && !doubleAnchorDone && anchorSlot >= 0) {
            player.inventory.selectedSlot = anchorSlot
            useOnLookedBlock(client, player)
            doubleAnchorDone = true
            retries = 0
            resetActionTimer()
            stage = Stage.CHARGING_ANCHOR
            return
        }
        stage = Stage.FINISH
    }

    private fun finishSequence() {
        releaseRotation()
        restorePreviousSlot()
        if (mode.value == Mode.ON_PLACE) {
            resetState()
            stage = Stage.IDLE
        } else {
            disable()
        }
    }

    private fun placeShield(client: Minecraft, player: LocalPlayer, level: ClientLevel) {
        if (!shieldStarted) {
            shieldStarted = true
            if (player.inventory.getItem(glowstoneSlot).count < 2) {
                skipShield()
                return
            }
            val candidate = findShieldCandidate(player, level)
            if (candidate == null) {
                skipShield()
                return
            }
            shieldBlock = candidate.block
            shieldSupport = candidate.support
            shieldFacing = candidate.facing
            shieldPlaced = false
            shieldRetries = 0
            shieldStep = 0
        }

        val support = shieldSupport
        val facing = shieldFacing
        if (shieldBlock == null || support == null || facing == null) {
            skipShield()
            return
        }

        aimAt(player, faceCenterOf(support, facing))
        if (System.currentTimeMillis() < nextActionTime) return

        if (shieldStep < 1) {
            shieldStep++
            return
        }

        if (!shieldPlaced) {
            val hit = client.hitResult as? BlockHitResult
            if (hit == null || hit.type != HitResult.Type.BLOCK) {
                if (++retries > MAX_RETRIES) skipShield()
                return
            }
            player.inventory.selectedSlot = glowstoneSlot
            useOnLookedBlock(client, player)
            shieldPlaced = true
            shieldRetries = 0
            return
        }

        if (level.getBlockState(shieldBlock!!).isAir) {
            if (++shieldRetries > MAX_RETRIES) skipShield()
            return
        }
        skipShield()
    }

    private fun skipShield() {
        retries = 0
        resetActionTimer()
        stage = Stage.CHARGING_ANCHOR
    }

    private class ShieldCandidate(val block: BlockPos, val support: BlockPos, val facing: Direction)

    private fun findShieldCandidate(player: LocalPlayer, level: ClientLevel): ShieldCandidate? {
        val anchor = anchorPos ?: return null
        val eye = Vec3(player.x, player.eyeY, player.z)
        val aim = bestAnchorFace(player, level, null) ?: return null

        val dx = aim.x - player.x
        val dy = aim.y - player.y
        val dz = aim.z - player.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        if (distance < 1.5) return null

        val seen = HashSet<BlockPos>()
        var best: ShieldCandidate? = null
        var bestCoverage = 0f

        var offset = 0.8
        while (offset < distance - 0.5) {
            val pos = BlockPos(
                floor(player.x + dx / distance * offset).toInt(),
                floor(player.y + dy / distance * offset).toInt(),
                floor(player.z + dz / distance * offset).toInt(),
            )
            offset += 0.4
            if (!seen.add(pos) || pos == anchor) continue
            if (!level.getBlockState(pos).canBeReplaced()) continue
            if (AABB(pos).intersects(player.boundingBox)) continue

            val support = findAdjacentSupport(level, pos) ?: continue
            if (isAnchorVisiblePast(level, eye, pos)) continue

            val shieldedAim = bestAnchorFace(player, level, pos) ?: continue
            val coverage = coverageRatio(shieldedAim, pos, player)
            if (coverage >= SHIELD_MIN_COVERAGE) return ShieldCandidate(pos, support.first, support.second)
            if (coverage <= bestCoverage) continue
            bestCoverage = coverage
            best = ShieldCandidate(pos, support.first, support.second)
        }
        return best
    }

    private fun findAdjacentSupport(level: ClientLevel, pos: BlockPos): Pair<BlockPos, Direction>? {
        for (direction in Direction.entries) {
            val neighbour = pos.relative(direction)
            if (level.getBlockState(neighbour).canBeReplaced()) continue
            return neighbour to direction.opposite
        }
        return null
    }

    private fun coverageRatio(viewOrigin: Vec3, shield: BlockPos, player: LocalPlayer): Float {
        val bounds = player.boundingBox
        val shieldBounds = AABB(shield)
        val xStep = 1.0 / ((bounds.maxX - bounds.minX) * 2.0 + 1.0)
        val yStep = 1.0 / ((bounds.maxY - bounds.minY) * 2.0 + 1.0)
        val zStep = 1.0 / ((bounds.maxZ - bounds.minZ) * 2.0 + 1.0)
        if (xStep < 0.0 || yStep < 0.0 || zStep < 0.0) return 0f

        val xOffset = (1.0 - floor(1.0 / xStep) * xStep) / 2.0
        val zOffset = (1.0 - floor(1.0 / zStep) * zStep) / 2.0
        var blocked = 0
        var total = 0

        var xf = 0.0
        while (xf <= 1.0) {
            var yf = 0.0
            while (yf <= 1.0) {
                var zf = 0.0
                while (zf <= 1.0) {
                    val sample = Vec3(
                        bounds.minX + (bounds.maxX - bounds.minX) * xf + xOffset,
                        bounds.minY + (bounds.maxY - bounds.minY) * yf,
                        bounds.minZ + (bounds.maxZ - bounds.minZ) * zf + zOffset,
                    )
                    if (shieldBounds.clip(viewOrigin, sample).isPresent) blocked++
                    total++
                    zf += zStep
                }
                yf += yStep
            }
            xf += xStep
        }
        return if (total == 0) 0f else blocked.toFloat() / total.toFloat()
    }

    private fun isAnchorVisiblePast(level: ClientLevel, eye: Vec3, shield: BlockPos): Boolean {
        val anchor = anchorPos ?: return true
        val shieldBounds = AABB(shield)
        val samples = listOf(
            Vec3(anchor.x + 0.5, anchor.y + 0.5, anchor.z + 0.5),
            Vec3(anchor.x + 0.5, anchor.y + 1.0, anchor.z + 0.5),
            Vec3(anchor.x + 0.5, anchor.y.toDouble(), anchor.z + 0.5),
        )
        return samples.all { shieldBounds.clip(eye, it).isEmpty }
    }

    private fun anchorAimPoint(player: LocalPlayer, level: ClientLevel): Vec3 {
        val anchor = anchorPos ?: return topCenterOf(baseBlockPos ?: player.blockPosition())
        return bestAnchorFace(player, level, shieldBlock) ?: centerOf(anchor)
    }

    private fun bestAnchorFace(player: LocalPlayer, level: ClientLevel, obstruction: BlockPos?): Vec3? {
        val anchor = anchorPos ?: return null
        val x = anchor.x.toDouble()
        val y = anchor.y.toDouble()
        val z = anchor.z.toDouble()
        val eye = Vec3(player.x, player.eyeY, player.z)
        val obstructionBounds = obstruction?.let { AABB(it) }

        val candidates = listOf(
            Vec3(x + 0.5, y + 1.0, z + 0.5),
            Vec3(x + 0.5, y, z + 0.5),
            Vec3(x, y + 0.5, z + 0.5),
            Vec3(x + 1.0, y + 0.5, z + 0.5),
            Vec3(x + 0.5, y + 0.5, z),
            Vec3(x + 0.5, y + 0.5, z + 1.0),
            Vec3(x + 0.25, y + 1.0, z + 0.5),
            Vec3(x + 0.75, y + 1.0, z + 0.5),
            Vec3(x, y + 0.25, z + 0.5),
            Vec3(x + 1.0, y + 0.75, z + 0.5),
            Vec3(x + 0.5, y + 0.25, z),
            Vec3(x + 0.5, y + 0.75, z + 1.0),
            Vec3(x + 0.25, y, z + 0.5),
            Vec3(x + 0.75, y, z + 0.5),
        )

        var best: Vec3? = null
        var bestAngle = Double.MAX_VALUE
        for (candidate in candidates) {
            if (obstructionBounds != null && obstructionBounds.clip(eye, candidate).isPresent) continue
            if (eye.distanceTo(candidate) <= 1.0E-4) continue

            val hit = level.clip(
                ClipContext(eye, candidate, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player)
            )
            if (hit.type != HitResult.Type.BLOCK || hit.blockPos != anchor) continue

            val (yaw, pitch) = anglesTo(player, candidate)
            val yawDelta = Mth.wrapDegrees(yaw - player.yRot)
            val pitchDelta = pitch - player.xRot
            val angle = sqrt((yawDelta * yawDelta + pitchDelta * pitchDelta).toDouble())
            if (angle >= bestAngle) continue
            bestAngle = angle
            best = candidate
        }
        return best
    }

    private fun isLookingAtAnchor(client: Minecraft, level: ClientLevel): Boolean {
        val hit = client.hitResult as? BlockHitResult ?: return false
        if (hit.type != HitResult.Type.BLOCK) return false
        return level.getBlockState(hit.blockPos).`is`(Blocks.RESPAWN_ANCHOR)
    }

    private fun isLookingAtPlacement(client: Minecraft): Boolean {
        val hit = client.hitResult as? BlockHitResult ?: return false
        if (hit.type != HitResult.Type.BLOCK) return false
        return hit.blockPos == baseBlockPos || hit.blockPos == anchorPos
    }

    private fun useOnLookedBlock(client: Minecraft, player: LocalPlayer) {
        val hit = client.hitResult as? BlockHitResult ?: return
        if (hit.type != HitResult.Type.BLOCK) return
        val result = client.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hit) ?: return
        if (result.consumesAction()) player.swing(InteractionHand.MAIN_HAND)
    }

    private fun isAnchorAt(level: ClientLevel, pos: BlockPos): Boolean =
        level.getBlockState(pos).`is`(Blocks.RESPAWN_ANCHOR)

    private fun chargeOf(level: ClientLevel, pos: BlockPos): Int {
        val state = level.getBlockState(pos)
        if (!state.`is`(Blocks.RESPAWN_ANCHOR)) return 0
        return state.getValue(RespawnAnchorBlock.CHARGE)
    }

    private fun chargeEntryStage(): Stage =
        if (safeAnchor.value) Stage.PLACING_SHIELD else Stage.CHARGING_ANCHOR

    private fun explosionSlot(player: LocalPlayer): Int {
        if (!explosionItemSwap.value) return anchorSlot.coerceAtLeast(0)
        val slot = findHotbarSlot(player) { matchesExplosionItem(it) }
        return if (slot >= 0) slot else anchorSlot.coerceAtLeast(0)
    }

    private fun matchesExplosionItem(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val itemName = stack.item.descriptionId.lowercase(Locale.ROOT)
        for (entry in explosionItems.value) {
            val token = entry.lowercase(Locale.ROOT).trim()
            if (token.isBlank()) continue
            if (token.endsWith("_category")) {
                val category = itemCategories.firstOrNull { it.id == token.removeSuffix("_category") }
                if (category != null && category.matches(itemName)) return true
            } else if (itemName.contains(token)) {
                return true
            }
        }
        return false
    }

    private inline fun findHotbarSlot(player: LocalPlayer, predicate: (ItemStack) -> Boolean): Int =
        (0..8).firstOrNull { predicate(player.inventory.getItem(it)) } ?: -1

    private fun aimAt(player: LocalPlayer, point: Vec3) {
        if (!aimAssist.value) return
        if (RotationManager.hasExternalRotation(ROTATION_OWNER)) return
        val (yaw, pitch) = anglesTo(player, point)
        RotationManager.perspective = true
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
        RotationManager.setTargetRotation(yaw, pitch, owner = ROTATION_OWNER)
        RotationManager.quickTick(aimSpeed.value)
    }

    private fun releaseRotation() {
        if (RotationManager.ownsRotation(ROTATION_OWNER)) RotationManager.clearRotation(ROTATION_OWNER)
    }

    private fun restorePreviousSlot() {
        val slot = previousSlot
        previousSlot = -1
        if (slot < 0) return
        Minecraft.getInstance().player?.inventory?.selectedSlot = slot
    }

    private fun resetActionTimer() {
        val (min, max) = delay.value
        val chosen = if (min >= max) min else min + Random().nextInt(max - min + 1)
        nextActionTime = System.currentTimeMillis() + chosen
    }

    private fun resetState() {
        stage = Stage.FINDING_ITEMS
        anchorPos = null
        baseBlockPos = null
        hitFacing = null
        anchorSlot = -1
        glowstoneSlot = -1
        retries = 0
        waitRetries = 0
        chargeRetries = 0
        anchorPlaced = false
        doubleAnchorDone = false
        nextActionTime = 0L
        shieldStarted = false
        shieldPlaced = false
        shieldStep = 0
        shieldRetries = 0
        shieldBlock = null
        shieldSupport = null
        shieldFacing = null
    }

    private fun centerOf(pos: BlockPos) = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)

    private fun topCenterOf(pos: BlockPos) = Vec3(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5)

    private fun faceCenterOf(pos: BlockPos, facing: Direction) = Vec3(
        pos.x + 0.5 + facing.stepX * 0.5,
        pos.y + 0.5 + facing.stepY * 0.5,
        pos.z + 0.5 + facing.stepZ * 0.5,
    )

    private fun anglesTo(player: LocalPlayer, point: Vec3): Pair<Float, Float> {
        val dx = point.x - player.x
        val dy = point.y - player.eyeY
        val dz = point.z - player.z
        val horizontal = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = (-Math.toDegrees(atan2(dy, horizontal))).toFloat()
        return yaw to pitch
    }

    override fun hudInfo(): String = stage.name.lowercase(Locale.ROOT).replace('_', ' ')
}