package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import onl.luka.grizzly.module.modules.utility.CheatDetector
import java.util.concurrent.ConcurrentLinkedQueue

internal sealed interface AntiCheatSignal {
    val receivedNanos: Long

    data class Spawn(
        val entityId: Int,
        val position: Vec3,
        val yaw: Float,
        val pitch: Float,
        override val receivedNanos: Long = System.nanoTime(),
    ) : AntiCheatSignal

    data class RelativeMove(
        val entityId: Int,
        val position: Vec3?,
        val yaw: Float?,
        val pitch: Float?,
        val onGround: Boolean,
        override val receivedNanos: Long = System.nanoTime(),
    ) : AntiCheatSignal

    data class AbsoluteMove(
        val entityId: Int,
        val position: Vec3,
        val yaw: Float,
        val pitch: Float,
        val onGround: Boolean,
        val teleport: Boolean,
        override val receivedNanos: Long = System.nanoTime(),
    ) : AntiCheatSignal

    data class Swing(
        val entityId: Int,
        val action: Int,
        override val receivedNanos: Long = System.nanoTime(),
    ) : AntiCheatSignal

    data class Damage(
        val victimId: Int,
        val sourceCauseId: Int,
        val sourceDirectId: Int,
        override val receivedNanos: Long = System.nanoTime(),
    ) : AntiCheatSignal

    data class Velocity(
        val entityId: Int,
        val movement: Vec3,
        override val receivedNanos: Long = System.nanoTime(),
    ) : AntiCheatSignal

    data class BlockChange(
        val position: BlockPos,
        val wasAir: Boolean,
        val isAir: Boolean,
        override val receivedNanos: Long = System.nanoTime(),
    ) : AntiCheatSignal

    data class Remove(
        val entityIds: IntArray,
        override val receivedNanos: Long = System.nanoTime(),
    ) : AntiCheatSignal
}

object AntiCheatPacketObserver {
    private val signals = ConcurrentLinkedQueue<AntiCheatSignal>()

    private fun emit(signal: AntiCheatSignal) {
        if (CheatDetector.isEnabled()) signals.offer(signal)
    }

    @JvmStatic
    fun onSpawn(entityId: Int, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        emit(AntiCheatSignal.Spawn(entityId, Vec3(x, y, z), yaw, pitch))
    }

    @JvmStatic
    fun onRelativeMove(
        entityId: Int,
        x: Double,
        y: Double,
        z: Double,
        hasPosition: Boolean,
        hasRotation: Boolean,
        yaw: Float,
        pitch: Float,
        onGround: Boolean,
    ) {
        emit(
            AntiCheatSignal.RelativeMove(
                entityId,
                Vec3(x, y, z).takeIf { hasPosition },
                yaw.takeIf { hasRotation },
                pitch.takeIf { hasRotation },
                onGround,
            ),
        )
    }

    @JvmStatic
    fun onAbsoluteMove(
        entityId: Int,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
        onGround: Boolean,
        teleport: Boolean,
    ) {
        emit(AntiCheatSignal.AbsoluteMove(entityId, Vec3(x, y, z), yaw, pitch, onGround, teleport))
    }

    @JvmStatic
    fun onSwing(entityId: Int, action: Int) {
        emit(AntiCheatSignal.Swing(entityId, action))
    }

    @JvmStatic
    fun onDamage(victimId: Int, sourceCauseId: Int, sourceDirectId: Int) {
        emit(AntiCheatSignal.Damage(victimId, sourceCauseId, sourceDirectId))
    }

    @JvmStatic
    fun onVelocity(entityId: Int, x: Double, y: Double, z: Double) {
        emit(AntiCheatSignal.Velocity(entityId, Vec3(x, y, z)))
    }

    @JvmStatic
    fun onBlockChange(position: BlockPos, wasAir: Boolean, isAir: Boolean) {
        emit(AntiCheatSignal.BlockChange(position.immutable(), wasAir, isAir))
    }

    @JvmStatic
    fun onRemove(entityIds: IntArray) {
        emit(AntiCheatSignal.Remove(entityIds.copyOf()))
    }

    internal fun drain(consumer: (AntiCheatSignal) -> Unit) {
        while (true) consumer(signals.poll() ?: break)
    }

    @JvmStatic
    fun clear() {
        signals.clear()
    }
}
