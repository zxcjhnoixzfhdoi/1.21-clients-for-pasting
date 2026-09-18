package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.phys.Vec3
import onl.luka.grizzly.module.modules.utility.CheatDetector
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal class AutoBlockCheck : AntiCheatCheck {
    override val id = "autoblock_a"
    override val displayName = "Auto Block"

    private data class State(var overlap: Int = 0, var lastTick: Long = Long.MIN_VALUE)
    private val states = HashMap<UUID, State>()

    override fun onAttack(client: Minecraft, event: AttackEvent, globallyLagging: Boolean): Evidence? {
        if (!CheatDetector.autoBlock.value || combatExempt(event, globallyLagging) || !event.hadSwing) return null
        val snapshot = event.attacker.snapshotAtOrBefore(event.actionTick) ?: return null
        val blocking = snapshot.blocking ||
            (snapshot.usingItem && snapshot.useAnimation == ItemUseAnimation.BLOCK)
        if (!blocking) {
            states.remove(event.attacker.uuid)
            return null
        }

        val state = states.getOrPut(event.attacker.uuid) { State() }
        state.overlap = if (state.lastTick != Long.MIN_VALUE && event.actionTick - state.lastTick <= 8) {
            state.overlap + 1
        } else {
            1
        }
        state.lastTick = event.actionTick

        // Under 1.8 rules block-hitting is a normal manual technique, so only a
        // long unbroken run of it means anything. Modern rules make the overlap
        // much rarer, but shield-and-axe play still produces it, and this
        // version lets a server put `blocks_attacks` on any item - so it is
        // still repetition, not a single hit, that carries the signal.
        val legacy = CheatDetector.usesLegacyCombatRules()
        val required = if (legacy) 6 else 4
        if (state.overlap < required) return null

        state.overlap = required / 2
        return Evidence(
            id,
            displayName,
            if (legacy) 1.25 else 1.7,
            EvidenceConfidence.SUSPICIOUS,
            "$required consecutive attacks while blocking (${if (legacy) "legacy" else "modern"} rules)",
        )
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }
}

internal class KillAuraCheck : AntiCheatCheck {
    override val id = "killaura_a"
    override val displayName = "Kill Aura"

    private data class RecentAttack(val tick: Long, val targetId: Int, val swingTick: Long)

    private data class State(
        var invalidAim: Int = 0,
        var blindHits: Int = 0,
        var swinglessHits: Int = 0,
        val recentAttacks: ArrayDeque<RecentAttack> = ArrayDeque(),
        val yawChanges: ArrayDeque<Float> = ArrayDeque(),
        var robotizedWindows: Int = 0,
        var lastCombatTick: Long = Long.MIN_VALUE,
    )

    private val states = HashMap<UUID, State>()

    override fun onAttack(client: Minecraft, event: AttackEvent, globallyLagging: Boolean): Evidence? {
        if (!CheatDetector.killAura.value || combatExempt(event, globallyLagging)) return null
        val state = states.getOrPut(event.attacker.uuid) { State() }
        val action = event.attacker.snapshotAtOrBefore(event.actionTick) ?: event.attacker.latest ?: return null

        // A dropped or reordered animate packet looks exactly like a swingless
        // hit, so this needs to happen twice before it counts.
        if (event.directSource && !event.hadSwing) {
            state.swinglessHits++
            if (state.swinglessHits < 2) return null
            state.swinglessHits = 0
            return Evidence(id, displayName, 2.8, EvidenceConfidence.STRONG, "damage without a recent arm swing")
        }
        state.swinglessHits = (state.swinglessHits - 1).coerceAtLeast(0)

        if (state.lastCombatTick != Long.MIN_VALUE && event.tick - state.lastCombatTick > COMBAT_ROTATION_WINDOW) {
            state.yawChanges.clear()
            state.robotizedWindows = 0
        }
        state.lastCombatTick = event.tick
        state.recentAttacks.addLast(RecentAttack(event.tick, event.victim.entityId, event.actionTick))
        while (state.recentAttacks.firstOrNull()?.let { event.tick - it.tick > MULTI_TARGET_WINDOW } == true) {
            state.recentAttacks.removeFirst()
        }
        val distinctTargets = state.recentAttacks.map { it.targetId }.distinct().size
        val distinctSwings = state.recentAttacks.map { it.swingTick }.distinct().size
        if (distinctTargets >= 3 && distinctSwings >= 2) {
            state.recentAttacks.clear()
            return Evidence(id, displayName, 3.0, EvidenceConfidence.STRONG, "three primary targets inside five ticks")
        }

        // Attack distance is owned by ReachCheck; a single wall-hit is usually a
        // desynced corner, so line-of-sight failures have to repeat.
        if (event.directSource && !AntiCheatGeometry.hasLineOfSight(client, event)) {
            state.blindHits++
            if (state.blindHits >= 3) {
                state.blindHits = 1
                return Evidence(id, displayName, 2.6, EvidenceConfidence.STRONG, "repeated attacks through blocks")
            }
        } else {
            state.blindHits = (state.blindHits - 1).coerceAtLeast(0)
        }

        val aimed = attackAimedDuringLatencyWindow(event)
        state.invalidAim = if (!aimed) state.invalidAim + 1 else (state.invalidAim - 2).coerceAtLeast(0)
        val error = AntiCheatGeometry.rotationError(
            event.attacker,
            event.attackerEntity,
            event.victim,
            event.victimEntity,
            action.yaw,
            action.pitch,
        )

        if (state.invalidAim >= 3 && event.directSource) {
            state.invalidAim = 0
            return Evidence(
                id,
                displayName,
                2.2,
                EvidenceConfidence.STRONG,
                "attack outside hitbox (${"%.1f".format(error)} deg)",
            )
        }
        if (action.usingItem && action.useAnimation in CONSUME_ACTIONS) {
            return Evidence(id, displayName, 1.0, EvidenceConfidence.SUSPICIOUS, "attack while consuming")
        }
        return null
    }

    override fun onTick(input: CheckTick): Evidence? {
        val state = states[input.tracked.uuid] ?: return null
        if (!CheatDetector.killAura.value || input.exempt) {
            state.yawChanges.clear()
            state.robotizedWindows = 0
            return null
        }
        if (state.lastCombatTick == Long.MIN_VALUE) return null
        val elapsed = input.tick - state.lastCombatTick
        if (elapsed > COMBAT_SESSION_RESET) {
            state.yawChanges.clear()
            state.recentAttacks.clear()
            state.robotizedWindows = 0
            state.lastCombatTick = Long.MIN_VALUE
            return null
        }
        if (elapsed > COMBAT_ROTATION_WINDOW || !input.snapshot.rotated) return null

        val change = abs(input.snapshot.yawDelta)
        if (change < 0.01f) return null
        state.yawChanges.addLast(change)
        if (state.yawChanges.size < ROTATION_WINDOW_SIZE) return null

        val window = state.yawChanges.toList()
        state.yawChanges.clear()
        val first = window.first()
        val robotized = window.count {
            abs(it - first) < ROTATION_QUANTUM * 1.5f && it > ROTATION_QUANTUM * 2f
        }
        val constant = window.count {
            abs(it - first) < ROTATION_QUANTUM * 0.65f && it > ROTATION_QUANTUM * 2.5f
        }
        val suspicious = robotized >= 9 || constant >= 8
        state.robotizedWindows = if (suspicious) {
            state.robotizedWindows + 1
        } else {
            (state.robotizedWindows - 1).coerceAtLeast(0)
        }
        if (state.robotizedWindows < 3) return null

        state.robotizedWindows = 1
        return Evidence(
            id,
            displayName,
            1.35,
            EvidenceConfidence.SUSPICIOUS,
            "robotized combat rotations ($robotized/$ROTATION_WINDOW_SIZE)",
        )
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }

    companion object {
        private val CONSUME_ACTIONS = setOf(ItemUseAnimation.EAT, ItemUseAnimation.DRINK)
        private const val MULTI_TARGET_WINDOW = 5L
        private const val COMBAT_ROTATION_WINDOW = 70L
        private const val COMBAT_SESSION_RESET = 140L
        private const val ROTATION_WINDOW_SIZE = 10
        private const val ROTATION_QUANTUM = 1.40625f
    }
}

internal class SilentAimCheck : AntiCheatCheck {
    override val id = "silentaim_a"
    override val displayName = "Silent Aim"

    private data class PendingReturn(
        val createdTick: Long,
        val beforeYaw: Float,
        val beforePitch: Float,
        val snapMagnitude: Double,
    )

    private data class State(
        var pending: PendingReturn? = null,
        var attackSnaps: Int = 0,
        var lastSnapTick: Long = Long.MIN_VALUE,
        var snapReturns: Int = 0,
        var lastReturnTick: Long = Long.MIN_VALUE,
        var combatUntilTick: Long = Long.MIN_VALUE,
        var targetId: Int = -1,
        var lastTargetBearing: Float = Float.NaN,
        var trackSamples: Int = 0,
        var trackHits: Int = 0,
        var lastVelocityX: Double = 0.0,
        var lastVelocityZ: Double = 0.0,
        var hasVelocity: Boolean = false,
        var movementSamples: Int = 0,
        var movementResidual: Double = 0.0,
        var sprintDesync: Int = 0,
    )

    private data class SnapCandidate(
        val previous: PlayerSnapshot,
        val current: PlayerSnapshot,
        val magnitude: Double,
    )

    private val states = HashMap<UUID, State>()

    override fun onAttack(client: Minecraft, event: AttackEvent, globallyLagging: Boolean): Evidence? {
        if (!CheatDetector.silentAim.value || combatExempt(event, globallyLagging) || !event.hadSwing) return null
        val state = states.getOrPut(event.attacker.uuid) { State() }
        state.combatUntilTick = maxOf(state.combatUntilTick, event.tick + COMBAT_WINDOW_TICKS)
        if (state.targetId != event.victim.entityId) {
            state.targetId = event.victim.entityId
            resetTracking(state)
            resetMovement(state)
        }

        val snap = findAttackSnap(event) ?: return null
        val alreadyReturned = event.attacker
            .snapshotsBetween(snap.current.tick + 1, event.tick)
            .any { it.rotated && rotationDistance(it.yaw, it.pitch, snap.previous.yaw, snap.previous.pitch) <= RETURN_TOLERANCE }

        state.attackSnaps = if (state.lastSnapTick != Long.MIN_VALUE && event.actionTick - state.lastSnapTick <= 30) {
            state.attackSnaps + 1
        } else {
            1
        }
        state.lastSnapTick = event.actionTick
        if (alreadyReturned) {
            state.pending = null
            recordReturn(state, event.tick, snap.magnitude)?.let { return it }
        } else {
            state.pending = PendingReturn(event.tick, snap.previous.yaw, snap.previous.pitch, snap.magnitude)
        }

        if (state.attackSnaps < 3) return null
        state.attackSnaps = 1
        return Evidence(
            id,
            displayName,
            1.5,
            EvidenceConfidence.SUSPICIOUS,
            "three target-landing combat snaps",
        )
    }

    override fun onTick(input: CheckTick): Evidence? {
        val state = states[input.tracked.uuid] ?: return null
        if (!CheatDetector.silentAim.value || input.exempt) {
            resetAnalysis(state)
            return null
        }

        state.pending?.let { pending ->
            val elapsed = input.tick - pending.createdTick
            if (elapsed > RETURN_WINDOW_TICKS) {
                state.pending = null
            } else if (
                elapsed > 0 && input.snapshot.rotated &&
                rotationDistance(input.snapshot.yaw, input.snapshot.pitch, pending.beforeYaw, pending.beforePitch) <= RETURN_TOLERANCE
            ) {
                state.pending = null
                recordReturn(state, input.tick, pending.snapMagnitude)?.let { return it }
            }
        }

        if (input.tick > state.combatUntilTick) {
            resetTracking(state)
            resetMovement(state)
            return null
        }

        analyzeTargetLock(input, state)?.let { return it }
        return analyzeMovementFix(input, state)
    }

    private fun findAttackSnap(event: AttackEvent): SnapCandidate? {
        val firstTick = (event.actionTick - SNAP_LOOKBACK_TICKS).coerceAtLeast(0)
        val samples = buildList {
            event.attacker.snapshotBefore(firstTick)?.let(::add)
            addAll(event.attacker.snapshotsBetween(firstTick, event.tick))
        }.distinctBy { it.tick }
        if (samples.size < 2) return null

        val victimPositions = victimPositions(event, firstTick, event.tick)
        return samples.zipWithNext().mapNotNull { (previous, current) ->
            if (!current.rotated) return@mapNotNull null
            val magnitude = rotationDistance(current.yaw, current.pitch, previous.yaw, previous.pitch)
            if (magnitude < SNAP_MIN_MAGNITUDE) return@mapNotNull null
            val wasAimed = aimsAtVictim(event, previous, victimPositions)
            val nowAimed = aimsAtVictim(event, current, victimPositions)
            if (wasAimed || !nowAimed) null else SnapCandidate(previous, current, magnitude)
        }.maxByOrNull { it.magnitude }
    }

    private fun recordReturn(state: State, tick: Long, magnitude: Double): Evidence? {
        state.snapReturns = if (state.lastReturnTick != Long.MIN_VALUE && tick - state.lastReturnTick <= 80) {
            state.snapReturns + 1
        } else {
            1
        }
        state.lastReturnTick = tick
        if (state.snapReturns < 2) return null
        state.snapReturns = 0
        return Evidence(
            id,
            displayName,
            2.8,
            EvidenceConfidence.STRONG,
            "repeated ${"%.1f".format(magnitude)} deg snap-and-return pairs",
        )
    }

    private fun analyzeTargetLock(input: CheckTick, state: State): Evidence? {
        val target = input.client.level?.getEntity(state.targetId) as? Player ?: run {
            resetTracking(state)
            return null
        }
        val dx = target.x - input.snapshot.position.x
        val dz = target.z - input.snapshot.position.z
        val distance = hypot(dx, dz)
        if (distance < TRACK_MIN_DISTANCE) return null

        val bearing = Math.toDegrees(atan2(-dx, dz)).toFloat()
        if (!state.lastTargetBearing.isNaN()) {
            val bearingDelta = abs(Mth.wrapDegrees(bearing - state.lastTargetBearing))
            if (bearingDelta in TRACK_BEARING_MIN..TRACK_BEARING_MAX && input.snapshot.rotated) {
                state.trackSamples++
                if (
                    AntiCheatGeometry.aimsAt(
                        input.player,
                        input.snapshot.position,
                        target,
                        target.position(),
                        input.snapshot.yaw,
                        input.snapshot.pitch,
                        0.08,
                    )
                ) {
                    state.trackHits++
                }
                if (state.trackSamples >= TRACK_WINDOW_SIZE) {
                    val hits = state.trackHits
                    val samples = state.trackSamples
                    state.trackSamples = 0
                    state.trackHits = 0
                    if (hits.toDouble() / samples >= TRACK_RATIO) {
                        return Evidence(
                            id,
                            displayName,
                            1.8,
                            EvidenceConfidence.SUSPICIOUS,
                            "target lock while line-of-sight moved ($hits/$samples)",
                        )
                    }
                }
            }
        }
        state.lastTargetBearing = bearing
        return null
    }

    private fun analyzeMovementFix(input: CheckTick, state: State): Evidence? {
        val snapshot = input.snapshot
        if (!snapshot.moved) return null
        val tickDelta = snapshot.movementTickDelta.coerceAtLeast(1).toDouble()
        val velocityX = snapshot.delta.x / tickDelta
        val velocityZ = snapshot.delta.z / tickDelta
        val velocityY = snapshot.delta.y / tickDelta
        val speed = hypot(velocityX, velocityZ)
        if (!state.hasVelocity) {
            state.lastVelocityX = velocityX
            state.lastVelocityZ = velocityZ
            state.hasVelocity = true
            return null
        }

        val acceleration = hypot(velocityX - state.lastVelocityX, velocityZ - state.lastVelocityZ)
        state.lastVelocityX = velocityX
        state.lastVelocityZ = velocityZ
        val usable = snapshot.onGround && abs(velocityY) < MOVE_FLAT_Y && snapshot.hurtTime == 0 &&
            !snapshot.surfaceIce && !snapshot.inWater && !snapshot.inLava &&
            speed in MOVE_MIN_SPEED..MOVE_MAX_SPEED
        if (!usable) {
            state.sprintDesync = (state.sprintDesync - 1).coerceAtLeast(0)
            state.movementSamples = 0
            state.movementResidual = 0.0
            return null
        }

        val movementBearing = Math.toDegrees(atan2(-velocityX, velocityZ)).toFloat()
        val offset = Mth.wrapDegrees(movementBearing - snapshot.yaw)
        if (
            snapshot.sprinting && speed > SPRINT_MIN_SPEED && acceleration < SPRINT_MAX_ACCELERATION &&
            abs(offset) > SPRINT_MAX_OFFSET
        ) {
            state.sprintDesync++
            if (state.sprintDesync >= 4) {
                state.sprintDesync = 1
                return Evidence(
                    id,
                    displayName,
                    2.2,
                    EvidenceConfidence.STRONG,
                    "sprint movement ${abs(offset).toInt()} degrees from sent yaw",
                )
            }
        } else {
            state.sprintDesync = (state.sprintDesync - 1).coerceAtLeast(0)
        }

        if (acceleration > MOVE_MAX_ACCELERATION) return null
        val nearestInputAngle = 45f * (offset / 45f).roundToInt()
        val residual = abs(Mth.wrapDegrees(offset - nearestInputAngle)).toDouble()
        state.movementSamples++
        state.movementResidual += residual
        if (state.movementSamples < MOVE_WINDOW_SIZE) return null

        val mean = state.movementResidual / state.movementSamples
        state.movementSamples = 0
        state.movementResidual = 0.0
        if (mean <= MOVE_MEAN_RESIDUAL) return null
        return Evidence(
            id,
            displayName,
            1.7,
            EvidenceConfidence.SUSPICIOUS,
            "movement-fix residual ${"%.1f".format(mean)} degrees",
        )
    }

    private fun resetAnalysis(state: State) {
        state.pending = null
        state.attackSnaps = 0
        state.snapReturns = 0
        state.combatUntilTick = Long.MIN_VALUE
        resetTracking(state)
        resetMovement(state)
    }

    private fun resetTracking(state: State) {
        state.lastTargetBearing = Float.NaN
        state.trackSamples = 0
        state.trackHits = 0
    }

    private fun resetMovement(state: State) {
        state.hasVelocity = false
        state.movementSamples = 0
        state.movementResidual = 0.0
        state.sprintDesync = 0
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }

    companion object {
        private const val COMBAT_WINDOW_TICKS = 70L
        private const val SNAP_LOOKBACK_TICKS = 4L
        private const val SNAP_MIN_MAGNITUDE = 12.0
        private const val RETURN_WINDOW_TICKS = 8L
        private const val RETURN_TOLERANCE = 4.0
        private const val TRACK_MIN_DISTANCE = 2.2
        private const val TRACK_BEARING_MIN = 2.5f
        private const val TRACK_BEARING_MAX = 45f
        private const val TRACK_WINDOW_SIZE = 20
        private const val TRACK_RATIO = 0.88
        private const val MOVE_MIN_SPEED = 0.15
        private const val MOVE_MAX_SPEED = 0.45
        private const val MOVE_FLAT_Y = 0.003
        private const val MOVE_MAX_ACCELERATION = 0.024
        private const val MOVE_WINDOW_SIZE = 12
        private const val MOVE_MEAN_RESIDUAL = 7.5
        private const val SPRINT_MIN_SPEED = 0.25
        private const val SPRINT_MAX_ACCELERATION = 0.08
        private const val SPRINT_MAX_OFFSET = 62f
    }
}

// Attack distance, taken as the closest pair across the latency window so desync cannot fake a hit.
internal class ReachCheck : AntiCheatCheck {
    override val id = "reach_a"
    override val displayName = "Reach"

    private data class State(var buffer: Double = 0.0, var lastTick: Long = Long.MIN_VALUE)
    private val states = HashMap<UUID, State>()

    override fun onAttack(client: Minecraft, event: AttackEvent, globallyLagging: Boolean): Evidence? {
        if (!CheatDetector.reach.value || combatExempt(event, globallyLagging) || !event.directSource) return null
        val distance = closestAttackDistance(event) ?: return null
        val limit = event.attackerEntity.entityInteractionRange().coerceIn(2.5, 6.0) + REACH_TOLERANCE
        val state = states.getOrPut(event.attacker.uuid) { State() }
        if (state.lastTick != Long.MIN_VALUE && event.tick - state.lastTick > RESET_TICKS) state.buffer = 0.0
        state.lastTick = event.tick

        val excess = distance - limit
        if (excess <= 0.0) {
            state.buffer = (state.buffer - 0.5).coerceAtLeast(0.0)
            return null
        }
        state.buffer += when {
            excess > 0.75 -> 2.0
            excess > 0.35 -> 1.5
            else -> 1.0
        }
        if (state.buffer < 3.0 * CheatDetector.bufferScale()) return null

        state.buffer = 1.0
        return Evidence(
            id,
            displayName,
            (1.5 + excess * 1.5).coerceAtMost(3.5),
            if (excess > 0.5) EvidenceConfidence.STRONG else EvidenceConfidence.SUSPICIOUS,
            "${"%.2f".format(distance)} blocks (limit ${"%.2f".format(limit)})",
        )
    }

    private fun closestAttackDistance(event: AttackEvent): Double? {
        val firstTick = (event.actionTick - LATENCY_TICKS).coerceAtLeast(0)
        val attackerSnapshots = event.attacker.snapshotsBetween(firstTick, event.tick).ifEmpty {
            listOfNotNull(event.attacker.snapshotAtOrBefore(event.actionTick))
        }
        if (attackerSnapshots.isEmpty()) return null
        val victimPositions = victimPositions(event, firstTick, event.tick)
        var closestSqr = Double.MAX_VALUE
        for (attacker in attackerSnapshots) {
            val eye = AntiCheatGeometry.eyePosition(event.attackerEntity, attacker.position)
            for (victimPosition in victimPositions) {
                val box = AntiCheatGeometry.serverBox(event.victimEntity, victimPosition)
                closestSqr = minOf(closestSqr, box.distanceToSqr(eye))
            }
        }
        return if (closestSqr == Double.MAX_VALUE) null else sqrt(closestSqr)
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }

    companion object {
        private const val REACH_TOLERANCE = 0.3
        private const val LATENCY_TICKS = 3L
        private const val RESET_TICKS = 60L
    }
}

// Click timing from arm-swing packet arrival. Combat only, since mining swings are periodic too.
internal class AutoClickerCheck : AntiCheatCheck {
    override val id = "autoclicker_a"
    override val displayName = "Auto Clicker"

    private data class State(
        var combatUntilTick: Long = Long.MIN_VALUE,
        var lastEvaluatedNanos: Long = Long.MIN_VALUE,
        var lastEvaluatedTick: Long = Long.MIN_VALUE,
        var suspiciousWindows: Int = 0,
    )

    private val states = HashMap<UUID, State>()

    override fun onAttack(client: Minecraft, event: AttackEvent, globallyLagging: Boolean): Evidence? {
        if (!CheatDetector.autoClicker.value || combatExempt(event, globallyLagging)) return null
        states.getOrPut(event.attacker.uuid) { State() }.combatUntilTick = event.tick + COMBAT_WINDOW_TICKS
        return null
    }

    override fun onTick(input: CheckTick): Evidence? {
        val state = states[input.tracked.uuid] ?: return null
        if (!CheatDetector.autoClicker.value || input.exempt) return null
        if (input.tick > state.combatUntilTick) {
            state.suspiciousWindows = 0
            return null
        }

        val times = input.tracked.swingTimes()
        if (times.size < SAMPLE_SIZE + 1) return null
        if (times.last() == state.lastEvaluatedNanos) return null
        // Consecutive windows share all but one sample, so they are re-scored on
        // a cooldown rather than on every incoming swing.
        if (input.tick - state.lastEvaluatedTick < EVALUATION_COOLDOWN_TICKS) return null
        state.lastEvaluatedNanos = times.last()
        state.lastEvaluatedTick = input.tick

        val intervals = times.zipWithNext { first, second -> (second - first) / 1_000_000.0 }.takeLast(SAMPLE_SIZE)
        // A pause anywhere in the window means this was not one continuous burst.
        if (intervals.any { it > MAX_GAP_MS || it <= 0.0 }) return null

        val mean = intervals.average()
        val cps = 1000.0 / mean
        val deviation = sqrt(intervals.sumOf { (it - mean) * (it - mean) } / intervals.size)

        if (cps > IMPOSSIBLE_CPS) {
            state.suspiciousWindows = 0
            return Evidence(
                id,
                displayName,
                2.6,
                EvidenceConfidence.STRONG,
                "${"%.1f".format(cps)} cps sustained over $SAMPLE_SIZE clicks",
            )
        }
        if (cps < MIN_ANALYSED_CPS || deviation > MAX_DEVIATION_MS) {
            state.suspiciousWindows = (state.suspiciousWindows - 1).coerceAtLeast(0)
            return null
        }

        state.suspiciousWindows++
        if (state.suspiciousWindows < 2) return null
        state.suspiciousWindows = 1
        return Evidence(
            id,
            displayName,
            1.6,
            EvidenceConfidence.SUSPICIOUS,
            "${"%.1f".format(cps)} cps with ${"%.1f".format(deviation)} ms jitter",
        )
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }

    companion object {
        private const val COMBAT_WINDOW_TICKS = 60L
        private const val SAMPLE_SIZE = 24
        private const val EVALUATION_COOLDOWN_TICKS = 20L
        private const val MAX_GAP_MS = 400.0
        private const val IMPOSSIBLE_CPS = 21.0
        private const val MIN_ANALYSED_CPS = 7.0

        // Human clicking scatters by tens of milliseconds; this is network jitter territory
        private const val MAX_DEVIATION_MS = 7.0
    }
}

// Vanilla clamps pitch to +/-90 before it reaches the network.
internal class InvalidRotationCheck : AntiCheatCheck {
    override val id = "invalidrot_a"
    override val displayName = "Invalid Rotation"

    private val lastFlagTick = HashMap<UUID, Long>()

    override fun onTick(input: CheckTick): Evidence? {
        if (!CheatDetector.invalidRotation.value || !input.snapshot.rotated) return null
        val snapshot = input.snapshot
        val invalidPitch = !snapshot.pitch.isFinite() || abs(snapshot.pitch) > MAX_PITCH
        val invalidYaw = !snapshot.yaw.isFinite()
        if (!invalidPitch && !invalidYaw) return null

        val previous = lastFlagTick[input.tracked.uuid]
        if (previous != null && input.tick - previous < FLAG_COOLDOWN_TICKS) return null
        lastFlagTick[input.tracked.uuid] = input.tick

        val detail = if (invalidYaw) {
            "non-finite yaw"
        } else {
            "pitch ${"%.1f".format(snapshot.pitch)} outside the vanilla range"
        }
        return Evidence(id, displayName, 4.0, EvidenceConfidence.STRONG, detail)
    }

    override fun remove(uuid: UUID) {
        lastFlagTick.remove(uuid)
    }

    override fun reset() {
        lastFlagTick.clear()
    }

    companion object {
        // One rotation byte of slack above the vanilla clamp
        private const val MAX_PITCH = 90.5f
        private const val FLAG_COOLDOWN_TICKS = 20L
    }
}

private fun attackAimedDuringLatencyWindow(event: AttackEvent): Boolean {
    val firstTick = (event.actionTick - 2).coerceAtLeast(0)
    val lastTick = minOf(event.tick, event.actionTick + 2)
    val attackerSnapshots = event.attacker.snapshotsBetween(firstTick, lastTick).ifEmpty {
        listOfNotNull(event.attacker.snapshotAtOrBefore(event.actionTick))
    }
    val positions = victimPositions(event, firstTick, event.tick)
    return attackerSnapshots.any { snapshot -> aimsAtVictim(event, snapshot, positions, 0.18) }
}

private fun victimPositions(event: AttackEvent, firstTick: Long, lastTick: Long): List<Vec3> =
    event.victim.snapshotsBetween(firstTick, lastTick).map { it.position }.ifEmpty {
        listOf(event.victim.serverPosition)
    }

private fun aimsAtVictim(
    event: AttackEvent,
    attackerSnapshot: PlayerSnapshot,
    victimPositions: List<Vec3>,
    inflate: Double = 0.12,
): Boolean = victimPositions.any { victimPosition ->
    AntiCheatGeometry.aimsAt(
        event.attackerEntity,
        attackerSnapshot.position,
        event.victimEntity,
        victimPosition,
        attackerSnapshot.yaw,
        attackerSnapshot.pitch,
        inflate,
    )
}

private fun rotationDistance(yaw: Float, pitch: Float, otherYaw: Float, otherPitch: Float): Double =
    hypot(abs(Mth.wrapDegrees(yaw - otherYaw)).toDouble(), abs(pitch - otherPitch).toDouble())

private fun combatExempt(event: AttackEvent, globallyLagging: Boolean): Boolean =
    globallyLagging ||
        event.attacker.isInGrace(event.tick, CheatDetector.joinGraceTicks.value) ||
        event.victim.isInGrace(event.tick, CheatDetector.joinGraceTicks.value) ||
        event.attacker.latest?.let { it.spectator || it.creativeFlying || it.passenger } != false
