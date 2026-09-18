package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import onl.luka.grizzly.module.modules.utility.CheatDetector
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

internal class ScaffoldFaceCheck : AntiCheatCheck {
    override val id = "scaffold"
    override val displayName = "Scaffold"

    private data class State(var failures: Int = 0, var lastFailureTick: Long = Long.MIN_VALUE)
    private val states = HashMap<UUID, State>()

    override fun onPlacement(client: Minecraft, event: PlacementEvent, globallyLagging: Boolean): Evidence? {
        if (!CheatDetector.scaffold.value || placementExempt(event, globallyLagging)) return null
        if (!isBridgingPlacement(event)) return null
        val state = states.getOrPut(event.player.uuid) { State() }
        if (hasPlausibleSupportAim(client, event)) {
            state.failures = (state.failures - 2).coerceAtLeast(0)
            return null
        }

        state.failures = if (state.lastFailureTick != Long.MIN_VALUE && event.tick - state.lastFailureTick <= 30) {
            state.failures + 1
        } else {
            1
        }
        state.lastFailureTick = event.tick
        if (state.failures < 3) return null
        state.failures = 1
        return Evidence(
            id,
            displayName,
            2.2,
            EvidenceConfidence.STRONG,
            "three attributed placements without plausible support-face aim",
        )
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }
}

internal class ScaffoldSnapCheck : AntiCheatCheck {
    override val id = "scaffold"
    override val displayName = "Scaffold"

    private data class PendingSnap(
        val createdTick: Long,
        val beforeYaw: Float,
        val beforePitch: Float,
        val magnitude: Double,
    )

    private data class State(
        var pending: PendingSnap? = null,
        var snapReturns: Int = 0,
        var lastSnapTick: Long = Long.MIN_VALUE,
    )

    private val states = HashMap<UUID, State>()

    override fun onPlacement(client: Minecraft, event: PlacementEvent, globallyLagging: Boolean): Evidence? {
        if (!CheatDetector.scaffold.value || placementExempt(event, globallyLagging)) return null
        if (!isBridgingPlacement(event)) return null
        val actionTick = event.actionTick
        val current = event.player.snapshotsBetween((actionTick - 1).coerceAtLeast(0), actionTick)
            .lastOrNull { it.rotated } ?: return null
        val previous = event.player.snapshotBefore(current.tick) ?: return null
        if (current.onGround) return null

        val wasAimed = AntiCheatGeometry.aimsAtPlacementSupport(
            client,
            event,
            previous.yaw,
            previous.pitch,
            previous.position,
        )
        val isAimed = AntiCheatGeometry.aimsAtPlacementSupport(
            client,
            event,
            current.yaw,
            current.pitch,
            current.position,
        )
        val magnitude = rotationDistance(current.yaw, current.pitch, previous.yaw, previous.pitch)
        if (wasAimed || !isAimed || magnitude < 35.0) return null

        val state = states.getOrPut(event.player.uuid) { State() }
        val alreadyReturned = event.player.snapshotsBetween(current.tick + 1, event.tick).firstOrNull {
            it.rotated && rotationDistance(it.yaw, it.pitch, previous.yaw, previous.pitch) <= 3.0
        }
        if (alreadyReturned != null) return recordSnapReturn(state, alreadyReturned.tick, magnitude)

        state.pending = PendingSnap(current.tick, previous.yaw, previous.pitch, magnitude)
        return null
    }

    override fun onTick(input: CheckTick): Evidence? {
        val state = states[input.tracked.uuid] ?: return null
        val pending = state.pending ?: return null
        val elapsed = input.tick - pending.createdTick
        if (!CheatDetector.scaffold.value || scaffoldTickExempt(input) || elapsed > 5) {
            state.pending = null
            return null
        }
        if (elapsed <= 0 || !input.snapshot.rotated) return null
        if (rotationDistance(input.snapshot.yaw, input.snapshot.pitch, pending.beforeYaw, pending.beforePitch) > 3.0) {
            return null
        }

        state.pending = null
        return recordSnapReturn(state, input.tick, pending.magnitude)
    }

    private fun recordSnapReturn(state: State, tick: Long, magnitude: Double): Evidence? {
        state.snapReturns = if (state.lastSnapTick != Long.MIN_VALUE && tick - state.lastSnapTick <= 80) {
            state.snapReturns + 1
        } else {
            1
        }
        state.lastSnapTick = tick
        if (state.snapReturns < 3) return null
        state.snapReturns = 1
        return Evidence(
            id,
            displayName,
            1.5,
            EvidenceConfidence.SUSPICIOUS,
            "three large airborne placement snaps with immediate returns (last ${magnitude.roundToInt()} degrees)",
        )
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }
}

internal class LegitScaffoldCheck : AntiCheatCheck {
    override val id = "legit_scaffold_a"
    override val displayName = "Legit Scaffold"

    private data class State(
        var initialized: Boolean = false,
        var wasSneaking: Boolean = false,
        var sneakStartTick: Long = Long.MIN_VALUE,
        var lastSneakEndTick: Long = Long.MIN_VALUE,
        var lastSneakDuration: Int = 0,
        val recentSneakDurations: ArrayDeque<Int> = ArrayDeque(),
        var matchedCycles: Int = 0,
        var lastMatchedEndTick: Long = Long.MIN_VALUE,
        var lastMatchTick: Long = Long.MIN_VALUE,
    )

    private val states = HashMap<UUID, State>()

    override fun onTick(input: CheckTick): Evidence? {
        if (!CheatDetector.legitScaffold.value || scaffoldTickExempt(input)) {
            states.remove(input.tracked.uuid)
            return null
        }

        val state = states.getOrPut(input.tracked.uuid) { State() }
        val sneaking = input.snapshot.sneaking
        if (!state.initialized) {
            state.initialized = true
            state.wasSneaking = sneaking
            if (sneaking) state.sneakStartTick = input.tick
            return null
        }

        if (sneaking && !state.wasSneaking) {
            state.sneakStartTick = input.tick
        } else if (!sneaking && state.wasSneaking && state.sneakStartTick != Long.MIN_VALUE) {
            val duration = (input.tick - state.sneakStartTick).toInt().coerceAtLeast(1)
            state.lastSneakEndTick = input.tick
            state.lastSneakDuration = duration
            state.recentSneakDurations.addLast(duration)
            while (state.recentSneakDurations.size > SNEAK_HISTORY_SIZE) {
                state.recentSneakDurations.removeFirst()
            }
        }
        state.wasSneaking = sneaking
        return null
    }

    override fun onPlacement(client: Minecraft, event: PlacementEvent, globallyLagging: Boolean): Evidence? {
        if (!CheatDetector.legitScaffold.value || placementExempt(event, globallyLagging)) return null
        if (!event.hadSwing || !isBridgingPlacement(event)) return null
        val actionTick = event.actionTick
        val action = event.player.snapshotAtOrBefore(actionTick) ?: return null
        if (!action.onGround || !action.holdingBlock || action.pitch < 60f) return decay(event.player.uuid)

        val state = states[event.player.uuid] ?: return null
        val releaseGap = actionTick - state.lastSneakEndTick
        val quickDurations = state.recentSneakDurations.takeLast(REQUIRED_QUICK_CYCLES)
        val repeatedQuickSneaks = quickDurations.size == REQUIRED_QUICK_CYCLES &&
            quickDurations.all { it in QUICK_SNEAK_TICKS }
        val swingAfterRelease = releaseGap in 0..MAX_RELEASE_TO_SWING_TICKS
        if (
            state.lastSneakDuration !in QUICK_SNEAK_TICKS || !repeatedQuickSneaks || !swingAfterRelease ||
            state.lastMatchedEndTick == state.lastSneakEndTick
        ) {
            return decay(event.player.uuid)
        }

        state.lastMatchedEndTick = state.lastSneakEndTick
        state.matchedCycles = if (
            state.lastMatchTick != Long.MIN_VALUE && event.tick - state.lastMatchTick <= MATCH_CHAIN_TICKS
        ) {
            state.matchedCycles + 1
        } else {
            1
        }
        state.lastMatchTick = event.tick
        if (state.matchedCycles < REQUIRED_QUICK_CYCLES) return null

        state.matchedCycles = 1
        return Evidence(
            id,
            displayName,
            1.5,
            EvidenceConfidence.SUSPICIOUS,
            "repeated 1-2 tick crouch-release placements",
        )
    }

    private fun decay(uuid: UUID): Evidence? {
        states[uuid]?.let { it.matchedCycles = (it.matchedCycles - 1).coerceAtLeast(0) }
        return null
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }

    companion object {
        private val QUICK_SNEAK_TICKS = 1..2
        private const val SNEAK_HISTORY_SIZE = 5
        private const val REQUIRED_QUICK_CYCLES = 3
        private const val MAX_RELEASE_TO_SWING_TICKS = 2L
        private const val MATCH_CHAIN_TICKS = 30L
    }
}

private fun hasPlausibleSupportAim(client: Minecraft, event: PlacementEvent): Boolean {
    val actionTick = event.actionTick
    val snapshots = event.player.snapshotsBetween((actionTick - 2).coerceAtLeast(0), actionTick)
    if (snapshots.isEmpty()) return true
    return snapshots.withIndex().any { (index, snapshot) ->
        val direct = AntiCheatGeometry.aimsAtPlacementSupport(
            client,
            event,
            snapshot.yaw,
            snapshot.pitch,
            snapshot.position,
        )
        if (direct || index == 0) return@any direct
        val previous = snapshots[index - 1]
        AntiCheatGeometry.aimsAtPlacementSupport(
            client,
            event,
            previous.yaw,
            snapshot.pitch,
            snapshot.position,
        )
    }
}

private fun rotationDistance(yaw: Float, pitch: Float, otherYaw: Float, otherPitch: Float): Double =
    hypot(abs(Mth.wrapDegrees(yaw - otherYaw)).toDouble(), abs(pitch - otherPitch).toDouble())

private fun placementExempt(event: PlacementEvent, globallyLagging: Boolean): Boolean {
    val snapshot = event.player.latest ?: return true
    return globallyLagging || event.player.isInGrace(event.tick, CheatDetector.joinGraceTicks.value) ||
        snapshot.passenger || snapshot.spectator || snapshot.creativeFlying ||
        event.attributionGap < if (event.hadSwing) 0.6 else 1.25
}

private fun scaffoldTickExempt(input: CheckTick): Boolean =
    input.exempt || input.snapshot.passenger || input.snapshot.spectator || input.snapshot.creativeFlying

private fun isBridgingPlacement(event: PlacementEvent): Boolean {
    val snapshot = event.player.snapshotAtOrBefore(event.actionTick) ?: return false
    val dx = event.position.x + 0.5 - snapshot.position.x
    val dz = event.position.z + 0.5 - snapshot.position.z
    return hypot(dx, dz) <= 2.25 && event.position.y <= floor(snapshot.position.y).toInt()
}
