package onl.luka.grizzly.module.modules.combat

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.util.InputUtil
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TridentItem
import kotlin.random.Random

object Criticals : Module("Criticals", "Forces critical hits on attacks", Category.COMBAT) {
    enum class Mode {
        NO_GROUND, PACKET, JUMP, LAG, TIMER
    }

    enum class PacketMode {
        VANILLA, NCP, FALLING, LOW, DOWN, GRIM, BLOCKSMC, MOSPIXEL
    }

    val mode = enum("mode", Mode.NO_GROUND)
    
    val packetMode = enum("packet mode", PacketMode.NCP).also {
        it.visibleWhen = { mode.value == Mode.PACKET }
    }

    private val maximumDelay = int("maximum delay (ms)", 150, 0, 500).also {
        it.visibleWhen = { mode.value == Mode.LAG }
    }

    @JvmField
    val timerSpeed = float("timer speed", 0.5f, 0.1f, 1.0f).also {
        it.visibleWhen = { mode.value == Mode.TIMER }
    }

    private val chance = float("chance (%)", 100f, 0f, 100f).also {
        it.visibleWhen = { mode.value == Mode.LAG || mode.value == Mode.TIMER }
    }

    private val requireWeapon = boolean("holding weapon", true).also {
        it.visibleWhen = { mode.value == Mode.LAG || mode.value == Mode.TIMER }
    }

    private val requireMouse = boolean("mouse pressed", true).also {
        it.visibleWhen = { mode.value == Mode.LAG || mode.value == Mode.TIMER }
    }

    //val jumpHeight = float("jump height", 0.42f, 0.1f, 0.42f).also {
    //    it.visibleWhen = { mode.value == Mode.JUMP }
    //}

    val jumpRange = float("jump range", 4.0f, 1.0f, 6.0f).also {
        it.visibleWhen = { mode.value == Mode.JUMP }
    }

    @JvmField var cachedOnGround: Boolean = false
    private var lastCrit: Long = 0L
    private var adjustNextJump: Boolean = false
    private var delayedTarget: Entity? = null
    private var delayedAttackDeadline = 0L
    private var replayingDelayedAttack = false
    @Volatile private var timerAttemptActive = false
    private var timerAttemptRolled = false

    override fun hudInfo(): String = mode.value.name.lowercase().replace("_", " ")

    override fun onTick(client: Minecraft) {
        val player = client.player
        if (player == null || client.level == null || client.gui.screen() != null) {
            clearTransientState(client)
            return
        }

        updateDelayedAttack(client, player)
        updateTimerAttempt(client, player)

        if (mode.value != Mode.JUMP) {
            releaseForcedJump(client)
            return
        }

        val level = client.level ?: return

        val hasEnemy = level.entitiesForRendering()
            .filterIsInstance<net.minecraft.world.entity.LivingEntity>()
            .any { it != player && it.isAlive && player.distanceTo(it) <= jumpRange.value }

        if (hasEnemy) {
            client.options.keyJump.setDown(true)
            adjustNextJump = true
        } else releaseForcedJump(client)
    }

    override fun onEnabled() {
        clearTransientState(Minecraft.getInstance())
    }

    override fun onDisabled() {
        clearTransientState(Minecraft.getInstance())
    }

    /** Returns false when LAG mode has queued this attack for the falling part of a real jump. */
    fun beforeAttack(player: Player, target: Entity): Boolean {
        if (!isEnabled() || mode.value != Mode.LAG || replayingDelayedAttack) return true
        if (!legitConditionsMet(Minecraft.getInstance(), player)) return true
        if (!canCritical(player) || player.onGround() || isFalling(player)) return true
        if (Random.nextFloat() * 100f >= chance.value) return true

        if (delayedTarget == null) {
            delayedTarget = target
            delayedAttackDeadline = System.currentTimeMillis() + maximumDelay.value
        }
        return false
    }

    fun onAttack(target: Entity) {
        if (!isEnabled()) return
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        if (mode.value == Mode.LAG || mode.value == Mode.TIMER || mode.value == Mode.NO_GROUND) return

        if (!player.onGround()) return
        if (player.isInWater || player.isInLava || player.onClimbable()) return 

        if (System.currentTimeMillis() - lastCrit < 500 && mode.value != Mode.NO_GROUND) {
            return
        }

        val connection = player.connection
        val x = player.x
        val y = player.y
        val z = player.z
        val hc = player.horizontalCollision

        when (mode.value) {
            Mode.PACKET -> {
                val onGroundState = false // most default to false
                when (packetMode.value) {
                    PacketMode.VANILLA -> {
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.2, z, onGroundState, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.01, z, onGroundState, hc))
                    }
                    PacketMode.NCP -> {
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.11, z, onGroundState, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.1100013579, z, onGroundState, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.0000013579, z, onGroundState, hc))
                    }
                    PacketMode.FALLING -> {
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, onGroundState, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.0625013579, z, onGroundState, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.0000013579, z, onGroundState, hc))
                    }
                    PacketMode.LOW -> {
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 1e-9, z, onGroundState, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y, z, onGroundState, hc))
                    }
                    PacketMode.DOWN -> {
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y - 1e-9, z, onGroundState, hc))
                    }
                    PacketMode.GRIM -> {
                        if (!player.onGround()) {
                            connection.send(ServerboundMovePlayerPacket.Pos(x, y - 0.000001, z, onGroundState, hc))
                        }
                    }
                    PacketMode.MOSPIXEL -> {
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.05250000001304, z, true, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.00150000001304, z, false, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.01400000001304, z, false, hc))
                        connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.00150000001304, z, false, hc))
                    }
                    PacketMode.BLOCKSMC -> {
                        if (player.tickCount % 4 == 0) {
                            connection.send(ServerboundMovePlayerPacket.Pos(x, y + 0.0011, z, true, hc))
                            connection.send(ServerboundMovePlayerPacket.Pos(x, y, z, onGroundState, hc))
                        }
                    }
                }
                lastCrit = System.currentTimeMillis()
            }
            Mode.JUMP -> {
                if (player.onGround() && !adjustNextJump) {
                    client.options.keyJump.setDown(true)
                    adjustNextJump = true
                }
                lastCrit = System.currentTimeMillis()
            }
            Mode.NO_GROUND -> {
            }
            Mode.LAG, Mode.TIMER -> {
            }
        }
    }

    @JvmStatic
    fun timerSpeedMultiplier(): Float =
        if (isEnabled() && mode.value == Mode.TIMER && timerAttemptActive) timerSpeed.value else 1.0f

    private fun updateDelayedAttack(client: Minecraft, player: Player) {
        if (mode.value != Mode.LAG) {
            clearDelayedAttack()
            return
        }

        val target = delayedTarget ?: return
        val now = System.currentTimeMillis()
        val targetInvalid = target.isRemoved || !target.isAlive ||
            player.distanceTo(target) > player.entityInteractionRange() + 0.5
        if (targetInvalid) {
            clearDelayedAttack()
            return
        }

        if (!isFalling(player) && now < delayedAttackDeadline) return

        clearDelayedAttack()
        val gameMode = client.gameMode ?: return
        replayingDelayedAttack = true
        try {
            gameMode.attack(player, target)
        } finally {
            replayingDelayedAttack = false
        }
    }

    private fun updateTimerAttempt(client: Minecraft, player: Player) {
        if (mode.value != Mode.TIMER || !canCritical(player)) {
            timerAttemptActive = false
            timerAttemptRolled = false
            return
        }

        if (!isFalling(player)) {
            timerAttemptActive = false
            if (player.onGround()) timerAttemptRolled = false
            return
        }

        if (!timerAttemptRolled && legitConditionsMet(client, player)) {
            timerAttemptRolled = true
            timerAttemptActive = Random.nextFloat() * 100f < chance.value
        }

        if (timerAttemptActive && !legitConditionsMet(client, player)) {
            timerAttemptActive = false
        }
    }

    private fun legitConditionsMet(client: Minecraft, player: Player): Boolean {
        val attackInput = InputUtil.isPhysicalKeyDown(client.options.keyAttack) || SilentAura.isActivelyAttacking()
        if (requireMouse.value && !attackInput) return false
        if (requireWeapon.value && !isWeapon(player.mainHandItem)) return false
        return true
    }

    private fun isWeapon(stack: ItemStack): Boolean =
        stack.`is`(ItemTags.SWORDS) || stack.`is`(ItemTags.AXES) || stack.item is TridentItem

    private fun canCritical(player: Player): Boolean =
        !player.isInWater && !player.isInLava && !player.onClimbable() && !player.isPassenger

    private fun isFalling(player: Player): Boolean =
        !player.onGround() && player.deltaMovement.y < -0.01 && player.fallDistance > 0f

    private fun clearDelayedAttack() {
        delayedTarget = null
        delayedAttackDeadline = 0L
    }

    private fun releaseForcedJump(client: Minecraft) {
        if (!adjustNextJump) return
        client.options.keyJump.setDown(InputUtil.isPhysicalKeyDown(client.options.keyJump))
        adjustNextJump = false
    }

    private fun clearTransientState(client: Minecraft) {
        clearDelayedAttack()
        replayingDelayedAttack = false
        timerAttemptActive = false
        timerAttemptRolled = false
        releaseForcedJump(client)
    }
}
