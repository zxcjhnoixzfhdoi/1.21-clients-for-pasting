package onl.luka.grizzly.module.modules.combat

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.other.TargetFilter
import onl.luka.grizzly.util.ContainerActions
import onl.luka.grizzly.util.RotationManager
import onl.luka.grizzly.util.SilentScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.resources.ResourceKey
import net.minecraft.world.InteractionHand
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

object AutoMace : Module(
    name = "Auto Mace",
    description = "Aims a falling mace smash at a target and swings it the tick before landing",
    category = Category.COMBAT,
) {
    enum class Selection { MANUAL, AUTO }
    enum class MaceType { DENSITY, BREACH }

    private val selection = enum("selection", Selection.AUTO)
    private val maceType = enum("mace type", MaceType.DENSITY).also {
        it.visibleWhen = { selection.value == Selection.MANUAL }
    }
    private val smashOnly = boolean("smash only", true)
    private val autoSwap = boolean("auto swap", true)
    private val reach = float("reach", 3.0f, 2.0f, 6.0f)

    private val aim = boolean("aim", true)
    private val aimRange = float("aim range", 6.0f, 2.0f, 10.0f).also {
        it.visibleWhen = { aim.value }
    }
    private val aimSpeed = float("aim speed", 45.0f, 1.0f, 180.0f).also {
        it.visibleWhen = { aim.value }
    }

    private val attack = boolean("attack", true)
    private val extraDelay = int("extra delay", 0, -20, 20).also {
        it.visibleWhen = { attack.value }
    }
    private val stunSlam = boolean("stun slam", false)
    private val ignoreTargetFilter = boolean("ignore target filter", false)

    private val unequipElytra = boolean("unequip elytra", false)
    private val reequipElytra = boolean("re-equip elytra", true).also {
        it.visibleWhen = { unequipElytra.value }
    }
    private val openInventory = boolean("open inventory", false).also {
        it.visibleWhen = { unequipElytra.value }
    }
    private val armorDelay = intRange("armor delay (ms)", 50 to 100, 0, 1000).also {
        it.visibleWhen = { unequipElytra.value }
    }

    private const val ROTATION_OWNER = "auto_mace"
    private const val PREDICTION_TICKS = 20
    private const val MAX_AIM_IMPACT_TICK = 6
    private const val REACH_MARGIN = 0.9
    private const val MIN_SMASH_FALL = 1.5
    private const val GRAVITY = 0.08
    private const val VERTICAL_DRAG = 0.98
    private const val HORIZONTAL_DRAG = 0.91
    private const val MIN_ELYTRA_IMPACT_TICK = 3
    private const val CHEST_INVENTORY_SLOT = 38
    private const val CLOSE_DELAY_TICKS = 2

    private var aimPoint: Vec3? = null
    private var restoreSlot = -1
    private var swapCooldown = 0
    private var stunSlamPending = false
    private var pendingArmorSwap = -1
    private var openedByModule = false
    private var closeTicksLeft = 0
    private var awaitingReequip = false
    private var sawDownwardMotion = false
    private var nextArmorActionTime = 0L

    private class FallSample(
        val tick: Int,
        val x: Double,
        val y: Double,
        val z: Double,
        val fallDistance: Double,
        val landed: Boolean,
    )

    private class Impact(val tick: Int, val aim: Vec3)

    override fun onEnabled() {
        aimPoint = null
        restoreSlot = -1
        swapCooldown = 0
        stunSlamPending = false
        clearElytraState()
    }

    override fun onDisabled() {
        val client = Minecraft.getInstance()
        if (openedByModule && client.gui.screen() is SilentScreen) client.gui.setScreen(null)
        releaseRotation()
        restoreHeldSlot()
        aimPoint = null
        stunSlamPending = false
        clearElytraState()
    }

    private fun clearElytraState() {
        pendingArmorSwap = -1
        openedByModule = false
        closeTicksLeft = 0
        awaitingReequip = false
        sawDownwardMotion = false
    }

    override fun onTick(client: Minecraft) {
        val player = client.player ?: return
        val level = client.level ?: return
        if (client.gui.screen() != null) {
            releaseRotation()
            return
        }

        if (swapCooldown > 0) swapCooldown--

        if (updateElytra(client, player, level)) return

        if (!hasMaceInHotbar(player)) {
            releaseRotation()
            restoreHeldSlot()
            return
        }

        val target = findTarget(player, level)
        if (target == null) {
            releaseRotation()
            restoreHeldSlot()
            return
        }

        updateAim(player, level, target)
        updateSwap(player, target)
        updateAttack(client, player, target)
    }

    private fun updateElytra(client: Minecraft, player: LocalPlayer, level: ClientLevel): Boolean {
        val gameMode = client.gameMode ?: return false
        val screenOpen = client.gui.screen() is SilentScreen || client.gui.screen() is InventoryScreen

        if (pendingArmorSwap >= 0) {
            if (openInventory.value && !screenOpen) {
                pendingArmorSwap = -1
                return false
            }
            if (System.currentTimeMillis() < nextArmorActionTime) return true
            performArmorSwap(player, gameMode, pendingArmorSwap)
            pendingArmorSwap = -1
            closeTicksLeft = CLOSE_DELAY_TICKS
            setArmorDelay()
            return true
        }

        if (openedByModule) {
            if (!screenOpen) {
                openedByModule = false
                return false
            }
            if (closeTicksLeft > 0) {
                closeTicksLeft--
                return true
            }
            client.gui.setScreen(null)
            openedByModule = false
            return true
        }

        if (!unequipElytra.value) {
            awaitingReequip = false
            sawDownwardMotion = false
            return false
        }
        if (System.currentTimeMillis() < nextArmorActionTime) return false

        if (player.isFallFlying) {
            if (awaitingReequip || !isWearingGlider(player)) return false
            val chestplate = findChestplateSlot(player)
            if (chestplate < 0 || findElytraTarget(player, level) == null) return false

            awaitingReequip = true
            sawDownwardMotion = false
            return requestArmorSwap(client, player, gameMode, chestplate)
        }

        if (!awaitingReequip) return false
        if (!reequipElytra.value) {
            awaitingReequip = false
            return false
        }

        val vertical = player.y - player.yOld
        if (vertical < -0.05) sawDownwardMotion = true
        if (!sawDownwardMotion || vertical <= 0.05) return false

        val gliderSlot = findGliderSlot(player)
        if (gliderSlot < 0) {
            awaitingReequip = false
            return false
        }

        awaitingReequip = false
        sawDownwardMotion = false
        return requestArmorSwap(client, player, gameMode, gliderSlot)
    }

    private fun requestArmorSwap(
        client: Minecraft,
        player: LocalPlayer,
        gameMode: net.minecraft.client.multiplayer.MultiPlayerGameMode,
        hotbarSlot: Int,
    ): Boolean {
        val screen = client.gui.screen()
        if (screen is SilentScreen || screen is InventoryScreen) {
            pendingArmorSwap = hotbarSlot
            return true
        }
        if (screen != null) return false

        if (openInventory.value) {
            client.gui.setScreen(SilentScreen(InventoryScreen(player)))
            openedByModule = true
            pendingArmorSwap = hotbarSlot
            return true
        }

        val swapped = performArmorSwap(player, gameMode, hotbarSlot)
        if (swapped) setArmorDelay()
        return swapped
    }

    private fun performArmorSwap(
        player: LocalPlayer,
        gameMode: net.minecraft.client.multiplayer.MultiPlayerGameMode,
        hotbarSlot: Int,
    ): Boolean {
        if (player.containerMenu !== player.inventoryMenu) return false
        val chestSlot = ContainerActions.playerSlot(player.inventoryMenu, player, CHEST_INVENTORY_SLOT)
            ?: return false
        return ContainerActions.click(this, player, gameMode, chestSlot, hotbarSlot, ContainerInput.SWAP)
    }

    private fun findElytraTarget(player: LocalPlayer, level: ClientLevel): LivingEntity? {
        val immediate = simulateFall(player, level, 0)
        val delayed = simulateFall(player, level, 1)
        if (immediate.isEmpty() || delayed.isEmpty()) return null

        val reachDistance = reach.value.toDouble()
        val range = aimRange.value.toDouble()
        var best: LivingEntity? = null
        var bestTick = Int.MAX_VALUE

        for (entity in candidates(player, level, range)) {
            if (!withinHorizontalRange(immediate, entity, range) &&
                !withinHorizontalRange(delayed, entity, range)
            ) continue

            val immediateImpact = findImpact(immediate, entity, reachDistance) ?: continue
            val delayedImpact = findImpact(delayed, entity, reachDistance) ?: continue
            if (immediateImpact.tick < MIN_ELYTRA_IMPACT_TICK) continue
            if (delayedImpact.tick < MIN_ELYTRA_IMPACT_TICK) continue

            val tick = maxOf(immediateImpact.tick, delayedImpact.tick)
            if (tick >= bestTick) continue
            bestTick = tick
            best = entity
        }
        return best
    }

    private fun withinHorizontalRange(samples: List<FallSample>, target: LivingEntity, range: Double): Boolean {
        val box = target.boundingBox
        val motion = target.deltaMovement
        for (sample in samples) {
            val ticks = minOf(sample.tick, 5).toDouble()
            val minX = box.minX + motion.x * ticks
            val maxX = box.maxX + motion.x * ticks
            val minZ = box.minZ + motion.z * ticks
            val maxZ = box.maxZ + motion.z * ticks
            val dx = sample.x - clamp(sample.x, minX, maxX)
            val dz = sample.z - clamp(sample.z, minZ, maxZ)
            if (sqrt(dx * dx + dz * dz) <= range) return true
        }
        return false
    }

    private fun isWearingGlider(player: LocalPlayer): Boolean =
        player.getItemBySlot(EquipmentSlot.CHEST).get(DataComponents.GLIDER) != null

    private fun findChestplateSlot(player: LocalPlayer): Int = (0..8).firstOrNull { slot ->
        val stack = player.inventory.getItem(slot)
        !stack.isEmpty &&
            stack.get(DataComponents.GLIDER) == null &&
            stack.get(DataComponents.EQUIPPABLE)?.slot() == EquipmentSlot.CHEST
    } ?: -1

    private fun findGliderSlot(player: LocalPlayer): Int = (0..8).firstOrNull { slot ->
        player.inventory.getItem(slot).get(DataComponents.GLIDER) != null
    } ?: -1

    private fun setArmorDelay() {
        val (min, max) = armorDelay.value
        val delay = if (min >= max) min else min + java.util.Random().nextInt(max - min + 1)
        nextArmorActionTime = System.currentTimeMillis() + delay
    }

    private fun updateAim(player: LocalPlayer, level: ClientLevel, target: LivingEntity) {
        if (!aim.value || !isFalling(player)) {
            releaseRotation()
            return
        }
        if (RotationManager.hasExternalRotation(ROTATION_OWNER)) return

        val reachDistance = reach.value.toDouble()
        val eye = Vec3(player.x, player.eyeY, player.z)
        val point = if (distanceToBox(eye, target.boundingBox) <= reachDistance) {
            aimPointOn(target.boundingBox, Vec3.ZERO)
        } else {
            val impact = findImpact(simulateFall(player, level), target, reachDistance)
            if (impact == null || impact.tick > MAX_AIM_IMPACT_TICK) {
                releaseRotation()
                return
            }
            impact.aim
        }

        aimPoint = point
        val (yaw, pitch) = anglesTo(player, point)
        RotationManager.perspective = true
        RotationManager.movementMode = RotationManager.MovementMode.CLIENT
        RotationManager.rotationMode = RotationManager.RotationMode.CLIENT
        RotationManager.setTargetRotation(yaw, pitch, owner = ROTATION_OWNER)
        RotationManager.quickTick(aimSpeed.value)
    }

    private fun updateSwap(player: LocalPlayer, target: LivingEntity) {
        if (!autoSwap.value || swapCooldown > 0) return
        if (stunSlam.value && stunSlamPending) return

        val best = findBestMaceSlot(player, target)
        if (best < 0 || best == player.inventory.selectedSlot) return

        if (restoreSlot < 0) restoreSlot = player.inventory.selectedSlot
        player.inventory.selectedSlot = best
        swapCooldown = 2
    }

    private fun updateAttack(client: Minecraft, player: LocalPlayer, target: LivingEntity) {
        if (!attack.value) return
        if (player.isUsingItem || player.isFallFlying) return

        val hit = client.hitResult
        if (hit !is EntityHitResult || hit.entity !== target) return
        if (player.getAttackStrengthScale(extraDelay.value.toFloat()) < 1.0f) return

        if (stunSlam.value && !stunSlamPending && target.isBlocking && isAxe(player.mainHandItem)) {
            swing(client, player, target)
            stunSlamPending = true
            swapCooldown = 0
            return
        }

        if (!isMace(player.mainHandItem)) return
        if (smashOnly.value && !isFalling(player)) return

        swing(client, player, target)
        stunSlamPending = false
        restoreHeldSlot()
    }

    private fun swing(client: Minecraft, player: LocalPlayer, target: LivingEntity) {
        client.gameMode?.attack(player, target)
        player.swing(InteractionHand.MAIN_HAND)
    }

    private fun simulateFall(player: LocalPlayer, level: ClientLevel, glideTicks: Int = 0): List<FallSample> {
        val samples = ArrayList<FallSample>(PREDICTION_TICKS)
        var vx = player.deltaMovement.x
        var vy = player.deltaMovement.y
        var vz = player.deltaMovement.z
        var x = player.x
        var y = player.y
        var z = player.z
        var fall = player.fallDistance

        for (tick in 1..PREDICTION_TICKS) {
            val gliding = tick <= glideTicks
            if (!gliding) vy = (vy - GRAVITY) * VERTICAL_DRAG
            x += vx
            y += vy
            z += vz
            if (!gliding) {
                vx *= HORIZONTAL_DRAG
                vz *= HORIZONTAL_DRAG
                if (vy < 0.0) fall += -vy
            }

            val box = player.boundingBox.move(x - player.x, y - player.y, z - player.z)
            val landed = !level.noCollision(player, box)
            samples.add(FallSample(tick, x, y + player.eyeHeight, z, fall, landed))
            if (landed) break
        }
        return samples
    }

    private fun findImpact(samples: List<FallSample>, target: LivingEntity, reachDistance: Double): Impact? {
        val motion = target.deltaMovement
        val grounded = target.onGround()

        for (sample in samples) {
            if (sample.landed || sample.fallDistance <= MIN_SMASH_FALL) continue

            val horizontalTicks = minOf(sample.tick, 5).toDouble()
            val offset = Vec3(
                motion.x * horizontalTicks,
                if (grounded) 0.0 else motion.y * minOf(sample.tick, 3).toDouble(),
                motion.z * horizontalTicks,
            )

            val from = Vec3(sample.x, sample.y, sample.z)
            if (distanceToBox(from, target.boundingBox.move(offset)) > reachDistance * REACH_MARGIN) continue

            return Impact(sample.tick, aimPointOn(target.boundingBox, offset))
        }
        return null
    }

    private fun aimPointOn(box: AABB, offset: Vec3): Vec3 = Vec3(
        (box.minX + box.maxX) * 0.5 + offset.x,
        box.minY + offset.y + (box.maxY - box.minY) * 0.75,
        (box.minZ + box.maxZ) * 0.5 + offset.z,
    )

    private fun distanceToBox(point: Vec3, box: AABB): Double {
        val dx = point.x - clamp(point.x, box.minX, box.maxX)
        val dy = point.y - clamp(point.y, box.minY, box.maxY)
        val dz = point.z - clamp(point.z, box.minZ, box.maxZ)
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun findTarget(player: LocalPlayer, level: ClientLevel): LivingEntity? {
        val range = if (aim.value) aimRange.value.toDouble() else reach.value.toDouble()
        return candidates(player, level, range).minByOrNull { entity -> player.distanceToSqr(entity) }
    }

    private fun candidates(player: LocalPlayer, level: ClientLevel, range: Double): List<LivingEntity> =
        level.entitiesForRendering()
            .filterIsInstance<LivingEntity>()
            .filter { entity -> entity !== player && entity.isAlive && !entity.isSpectator }
            .filter { entity -> horizontalDistance(player, entity) <= range }
            .filter { entity ->
                ignoreTargetFilter.value || entity !is Player || TargetFilter.isValidTarget(player, entity)
            }

    private fun horizontalDistance(player: LocalPlayer, target: LivingEntity): Double {
        val dx = target.x - player.x
        val dz = target.z - player.z
        return sqrt(dx * dx + dz * dz)
    }

    private fun findBestMaceSlot(player: LocalPlayer, target: LivingEntity): Int {
        var bestSlot = -1
        var bestScore = -1.0
        for (slot in 0..8) {
            val score = scoreMace(player, player.inventory.getItem(slot), target)
            if (score < 0.0) continue
            if (selection.value == Selection.MANUAL) return slot
            if (score <= bestScore) continue
            bestScore = score
            bestSlot = slot
        }
        return bestSlot
    }

    private fun scoreMace(player: LocalPlayer, stack: ItemStack, target: LivingEntity): Double {
        if (!isMace(stack)) return -1.0
        if (smashOnly.value && !isFalling(player)) return -1.0

        val density = enchantLevel(stack, Enchantments.DENSITY)
        val breach = enchantLevel(stack, Enchantments.BREACH)

        if (selection.value == Selection.MANUAL) {
            val wanted = if (maceType.value == MaceType.DENSITY) density else breach
            return if (wanted > 0) 0.0 else -1.0
        }

        if (player.onGround()) return if (breach > 0) 0.0 else -1.0

        var score = -1.0
        if (density > 0) score = estimateDamage(player.fallDistance, target, density, 0)
        if (breach > 0) score = maxOf(score, estimateDamage(player.fallDistance, target, 0, breach))
        return score
    }

    private fun estimateDamage(fallDistance: Double, target: LivingEntity, density: Int, breach: Int): Double {
        val damage = 6.0 + smashBonus(fallDistance) + 0.5 * density * fallDistance
        val armor = target.getAttributeValue(Attributes.ARMOR)
        val toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
        val effectiveArmor = clamp(armor - damage / (2.0 + toughness / 4.0), armor * 0.2, 20.0)
        val reduction = clamp(effectiveArmor / 25.0 - 0.15 * breach, 0.0, 1.0)
        return damage * (1.0 - reduction)
    }

    private fun smashBonus(fallDistance: Double): Double = when {
        fallDistance <= 3.0 -> 4.0 * fallDistance
        fallDistance <= 8.0 -> 12.0 + 2.0 * (fallDistance - 3.0)
        else -> 22.0 + fallDistance - 8.0
    }

    private fun isFalling(player: LocalPlayer): Boolean =
        !player.onGround() && player.deltaMovement.y < 0.0 && !player.isFallFlying

    private fun hasMaceInHotbar(player: LocalPlayer): Boolean =
        (0..8).any { isMace(player.inventory.getItem(it)) }

    private fun isMace(stack: ItemStack): Boolean = !stack.isEmpty && stack.item === Items.MACE

    private fun isAxe(stack: ItemStack): Boolean = !stack.isEmpty && stack.item is AxeItem

    private fun enchantLevel(stack: ItemStack, key: ResourceKey<Enchantment>): Int {
        val level = Minecraft.getInstance().level ?: return 0
        return try {
            stack.enchantments.getLevel(level.registryAccess().getOrThrow(key))
        } catch (_: Exception) {
            0
        }
    }

    private fun restoreHeldSlot() {
        val slot = restoreSlot
        restoreSlot = -1
        if (slot < 0) return
        Minecraft.getInstance().player?.inventory?.selectedSlot = slot
    }

    private fun releaseRotation() {
        aimPoint = null
        if (RotationManager.ownsRotation(ROTATION_OWNER)) RotationManager.clearRotation(ROTATION_OWNER)
    }

    private fun anglesTo(player: Player, point: Vec3): Pair<Float, Float> {
        val dx = point.x - player.x
        val dy = point.y - player.eyeY
        val dz = point.z - player.z
        val horizontal = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = (-Math.toDegrees(atan2(dy, horizontal))).toFloat()
        return yaw to pitch
    }

    private fun clamp(value: Double, min: Double, max: Double): Double =
        maxOf(min, minOf(max, value))

    override fun hudInfo(): String =
        if (selection.value == Selection.AUTO) "auto" else maceType.value.name.lowercase()
}
