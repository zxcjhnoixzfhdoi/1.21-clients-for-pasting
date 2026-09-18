package onl.luka.grizzly.module.modules.anarchy

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.combat.AutoBlock.canUseAsBlock
import onl.luka.grizzly.module.modules.other.TargetFilter
import onl.luka.grizzly.util.InputUtil
import onl.luka.grizzly.util.RotationManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.tags.ItemTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.TridentItem
import kotlin.math.atan2
import kotlin.math.sqrt

object KillAura : Module(
    name = "Kill Aura",
    description = "Automatically attacks enemies in your range",
    category = Category.ANARCHY
) {
    enum class Mode {
        APS, SEQUENTIAL
    }

    enum class TargetMode {
        SINGLE, SWITCH, MULTI
    }

    private val mode = enum("mode", Mode.APS)
    private val targetMode = enum("targeting", TargetMode.SWITCH)

    private val range = float("range", 6.0f, 1.0f, 8.0f)
    private val aps = int("aps", 20, 1, 40).also {
        it.visibleWhen = { mode.value == Mode.APS }
    }
    private val rotate = boolean("rotate", true)
    private val playersOnly = boolean("players only", true)
    private val autoWeapon = boolean("auto weapon", false)
    private val autoBlock = boolean("auto block", false)

    private var target: LivingEntity? = null
    private var accumulator = 0.0f
    private var ownsUseKey = false

    override fun onEnabled() {
        accumulator = 0.0f
        target = null
        ownsUseKey = false
    }

    override fun onDisabled() {
        target = null
        releaseAutoBlock(Minecraft.getInstance())
    }

    init {
        ClientTickEvents.START_CLIENT_TICK.register {
            if (!isEnabled()) return@register
            val client = Minecraft.getInstance()
            val player = client.player
            val level = client.level
            if (player == null || level == null || client.gui.screen() != null) {
                target = null
                releaseAutoBlock(client)
                return@register
            }

            val maxRange = range.value.toDouble()

            val candidates = level.entitiesForRendering()
                .filterIsInstance<LivingEntity>()
                .filter { e ->
                    e !== player && !e.isDeadOrDying &&
                            !(playersOnly.value && e !is Player) &&
                            player.distanceTo(e) <= maxRange &&
                            TargetFilter.isValidTarget(player, e)
                }

            val bestTarget = candidates.minByOrNull { player.distanceTo(it) }

            if (targetMode.value == TargetMode.SINGLE && target != null && candidates.contains(target)) {
                // Keep target
            } else {
                target = bestTarget
            }

            if (target == null) {
                releaseAutoBlock(client)
                return@register
            }

            if (autoWeapon.value) {
                val bestSlot = findBestWeapon(player)
                if (bestSlot != -1 && player.inventory.selectedSlot != bestSlot) {
                    player.inventory.selectedSlot = bestSlot
                }
            }

            updateAutoBlock(client, player)

            when (mode.value) {
                Mode.APS -> {
                    accumulator += aps.value / 20.0f
                    while (accumulator >= 1.0f) {
                        accumulator -= 1.0f
                        val targetsThisAttack = if (targetMode.value == TargetMode.MULTI) {
                            candidates
                        } else {
                            listOf(target!!)
                        }
                        if (attackTargets(client, player, targetsThisAttack)) {
                            player.swing(InteractionHand.MAIN_HAND)
                        }
                    }
                }

                Mode.SEQUENTIAL -> {
                    if (player.getAttackStrengthScale(0.5f) >= 1.0f) {
                        if (attackTargets(client, player, listOf(target!!))) {
                            player.swing(InteractionHand.MAIN_HAND)
                        }
                    }
                }
            }
        }
    }

    private fun updateAutoBlock(client: Minecraft, player: LocalPlayer) {
        val canBlock = InteractionHand.entries.any { hand ->
            player.getItemInHand(hand).canUseAsBlock()
        }
        if (!autoBlock.value || !canBlock) {
            releaseAutoBlock(client)
            return
        }

        client.options.keyUse.setDown(true)
        ownsUseKey = true
    }

    private fun releaseAutoBlock(client: Minecraft) {
        if (!ownsUseKey) return
        client.options.keyUse.setDown(InputUtil.isPhysicalKeyDown(client.options.keyUse))
        ownsUseKey = false
    }

    private fun attackTargets(
        client: Minecraft,
        player: LocalPlayer,
        targets: List<LivingEntity>,
    ): Boolean {
        val gameMode = client.gameMode ?: return false
        val validTargets = targets.filterNot { it.isDeadOrDying }
        if (validTargets.isEmpty()) return false

        val restoreYaw = if (RotationManager.isActive()) {
            RotationManager.getCurrentYaw()
        } else {
            player.yRot
        }
        val restorePitch = if (RotationManager.isActive()) {
            RotationManager.getCurrentPitch()
        } else {
            player.xRot
        }

        try {
            for (attackTarget in validTargets) {
                if (rotate.value) {
                    val (yaw, pitch) = rotationTo(player, attackTarget)
                    sendLookPacket(player, yaw, pitch)
                }
                gameMode.attack(player, attackTarget)
            }
        } finally {
            if (rotate.value) {
                sendLookPacket(player, restoreYaw, restorePitch)
            }
        }

        return true
    }

    private fun rotationTo(player: Player, target: LivingEntity): Pair<Float, Float> {
        val aimY = target.y + target.bbHeight * 0.5
        val dx = target.x - player.x
        val dy = aimY - player.eyeY
        val dz = target.z - player.z
        val horizDist = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = (-Math.toDegrees(atan2(dy, horizDist))).toFloat()
        return yaw to pitch
    }

    private fun sendLookPacket(player: LocalPlayer, yaw: Float, pitch: Float) {
        player.connection.send(
            ServerboundMovePlayerPacket.Rot(
                yaw,
                pitch,
                player.onGround(),
                player.horizontalCollision,
            )
        )
    }

    private fun findBestWeapon(player: Player): Int {
        for (i in 0..8) {
            val itemStack = player.inventory.getItem(i)
            if (itemStack.isEmpty) continue
            if (itemStack.`is`(ItemTags.SWORDS) || itemStack.`is`(ItemTags.AXES) || itemStack.item is TridentItem) {
                return i
            }
        }
        return -1
    }

    override fun hudInfo(): String {
        return when (mode.value) {
            Mode.APS -> "${aps.value} aps"
            Mode.SEQUENTIAL -> "sequential"
        }
    }
}
