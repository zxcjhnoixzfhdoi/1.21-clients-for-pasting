package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.phys.Vec3
import onl.luka.grizzly.module.modules.utility.CheatDetector
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot

internal class NoSlowCheck : AntiCheatCheck {
    override val id = "noslow_a"
    override val displayName = "No Slow"
    private val streaks = HashMap<UUID, Int>()

    override fun onTick(input: CheckTick): Evidence? {
        val snapshot = input.snapshot
        if (!CheatDetector.noSlow.value || input.exempt || !snapshot.moved) return reset(input.tracked.uuid)
        if (!snapshot.usingItem || snapshot.useAnimation !in SLOWING_ACTIONS) return reset(input.tracked.uuid)
        // Momentum from before the item went up needs several ticks to bleed off,
        // so the check only looks at players who have been using it for a while.
        if (input.tracked.useTicks < MIN_USE_TICKS || !snapshot.onGround) return reset(input.tracked.uuid)
        if (movementExempt(input, velocityTicks = 8) || snapshot.surfaceIce || snapshot.movementTickDelta > 2) {
            return reset(input.tracked.uuid)
        }

        // Using an item scales movement input to 0.2x. Compare against the walking
        // ceiling implied by the player's own attributes, minus the sprint modifier
        // that vanilla strips while an item is in use.
        val walkCeiling = snapshot.groundSpeedCeiling / (if (snapshot.sprinting) SPRINT_MODIFIER else 1.0)
        val limit = walkCeiling * USE_SPEED_FRACTION * CheatDetector.thresholdScale()
        val streak = if (snapshot.horizontalSpeed > limit) (streaks[input.tracked.uuid] ?: 0) + 1 else 0
        streaks[input.tracked.uuid] = streak
        if (streak < 4) return null

        streaks[input.tracked.uuid] = 2
        val ratio = snapshot.horizontalSpeed / limit.coerceAtLeast(1.0E-4)
        return Evidence(
            id,
            displayName,
            if (ratio > 2.0) 2.0 else 1.3,
            if (ratio > 2.0) EvidenceConfidence.STRONG else EvidenceConfidence.SUSPICIOUS,
            "${"%.3f".format(snapshot.horizontalSpeed)} b/t while ${snapshot.useAnimation.name.lowercase()} " +
                "(limit ${"%.3f".format(limit)})",
        )
    }

    private fun reset(uuid: UUID): Evidence? {
        streaks.remove(uuid)
        return null
    }

    override fun remove(uuid: UUID) {
        streaks.remove(uuid)
    }

    override fun reset() {
        streaks.clear()
    }

    companion object {
        private const val MIN_USE_TICKS = 6
        private const val SPRINT_MODIFIER = 1.3
        private const val USE_SPEED_FRACTION = 0.36
        private val SLOWING_ACTIONS = setOf(
            ItemUseAnimation.EAT,
            ItemUseAnimation.DRINK,
            ItemUseAnimation.BLOCK,
            ItemUseAnimation.BOW,
            ItemUseAnimation.CROSSBOW,
            ItemUseAnimation.TRIDENT,
            ItemUseAnimation.SPEAR,
            ItemUseAnimation.SPYGLASS,
            ItemUseAnimation.TOOT_HORN,
        )
    }
}

// Uses reachable acceleration, sustained ground speed, and an independent airborne envelope.
internal class SpeedCheck : AntiCheatCheck {
    override val id = "speed_a"
    override val displayName = "Speed"

    private data class State(
        var buffer: Double = 0.0,
        var excessSamples: Int = 0,
        val recentSpeeds: ArrayDeque<Double> = ArrayDeque(),
        var airborneCeiling: Double? = null,
        var airborneSamples: Int = 0,
    )

    private val states = HashMap<UUID, State>()

    override fun onTick(input: CheckTick): Evidence? {
        if (!CheatDetector.speed.value || input.exempt) return clear(input.tracked.uuid)
        val snapshot = input.snapshot
        val state = states.getOrPut(input.tracked.uuid) { State() }
        if (!snapshot.moved) {
            state.buffer = (state.buffer - 0.2).coerceAtLeast(0.0)
            if (snapshot.onGround) resetAirborneEnvelope(state)
            return null
        }
        if (!snapshot.predictableMovement) return clear(input.tracked.uuid)
        if (snapshot.movementTickDelta > 3 || movementExempt(input, velocityTicks = 10)) {
            return clear(input.tracked.uuid)
        }
        if (snapshot.inWater || snapshot.inLava || snapshot.climbing || snapshot.levitating) {
            return clear(input.tracked.uuid)
        }

        val samples = input.tracked.movementSamples(2)
        val previous = samples.getOrNull(samples.lastIndex - 1)
        val gap = snapshot.movementTickDelta.coerceIn(1, MAX_PREDICTION_GAP)
        val contiguous = previous != null && previous.predictableMovement &&
            snapshot.tick - previous.tick == gap.toLong()
        val speed = snapshot.horizontalSpeed
        val scale = CheatDetector.thresholdScale()

        if (snapshot.horizontalCollision || previous?.horizontalCollision == true) {
            state.buffer = (state.buffer - 0.3).coerceAtLeast(0.0)
            state.excessSamples = 0
            state.recentSpeeds.clear()
            state.airborneCeiling = speed.takeIf { !snapshot.onGround }
            state.airborneSamples = 0
            return null
        }

        // Steady-state ceiling: what the player could hold forever on this surface.
        // Only pure ground movement feeds the average; jumps are handled below.
        val ceiling = snapshot.groundSpeedCeiling.coerceAtLeast(0.05)
        if (contiguous && snapshot.onGround && previous.onGround) {
            state.recentSpeeds.addLast(speed)
            while (state.recentSpeeds.size > AVERAGE_WINDOW) state.recentSpeeds.removeFirst()
        } else {
            state.recentSpeeds.clear()
        }
        val averageSpeed = if (state.recentSpeeds.isEmpty()) 0.0 else state.recentSpeeds.average()
        val averageRatio = if (state.recentSpeeds.size >= AVERAGE_WINDOW) {
            averageSpeed / (ceiling * AVERAGE_TOLERANCE * scale)
        } else {
            0.0
        }

        // Compare vectors, not only magnitudes. After friction, vanilla input can
        // change horizontal velocity by at most one acceleration impulse.
        val accelerationRatio = if (
            contiguous && gap == 1 &&
            !(snapshot.onGround && !previous.onGround)
        ) {
            accelerationRatio(snapshot, previous, scale)
        } else {
            0.0
        }

        // This envelope evolves from the takeoff state rather than accepting the
        // latest observed speed as the next baseline. Constant airborne speed
        // therefore becomes impossible once vanilla drag should have reduced it.
        val airborneRatio = updateAirborneEnvelope(state, snapshot, previous, gap, contiguous, scale)

        val ratio = maxOf(accelerationRatio, averageRatio, airborneRatio)
        state.excessSamples = if (ratio > 1.0) state.excessSamples + 1 else (state.excessSamples - 1).coerceAtLeast(0)
        state.buffer = when {
            ratio > 1.50 -> state.buffer + 3.0
            ratio > 1.25 -> state.buffer + 2.0
            ratio > 1.08 -> state.buffer + 1.25
            ratio > 1.0 -> state.buffer + 0.7
            else -> (state.buffer - 0.45).coerceAtLeast(0.0)
        }.coerceAtMost(8.0)
        val required = 3.0 * CheatDetector.bufferScale()
        if (state.excessSamples < 2 || state.buffer < required) return null

        state.buffer = 1.0
        state.excessSamples = 1
        val source = when {
            ratio == accelerationRatio -> "acceleration"
            ratio == airborneRatio -> "airborne envelope"
            else -> "sustained average"
        }
        if (!snapshot.onGround) {
            state.airborneCeiling = speed
            state.airborneSamples = 0
        }
        return Evidence(
            id,
            displayName,
            (1.1 + (ratio - 1.0) * 2.0).coerceIn(1.1, 3.0),
            if (ratio > 1.35) EvidenceConfidence.STRONG else EvidenceConfidence.SUSPICIOUS,
            "${"%.3f".format(speed)} b/t, $source ratio ${"%.2f".format(ratio)} " +
                "(avg ${"%.3f".format(averageSpeed)}, ceiling ${"%.3f".format(ceiling)})",
        )
    }

    private fun accelerationRatio(
        snapshot: PlayerSnapshot,
        previous: PlayerSnapshot,
        scale: Double,
    ): Double {
        val previousTicks = previous.movementTickDelta.coerceAtLeast(1).toDouble()
        val previousX = previous.delta.x / previousTicks
        val previousZ = previous.delta.z / previousTicks
        val drag = if (previous.onGround) {
            0.91 * previous.slipperiness.coerceIn(0.4, 0.99)
        } else {
            AIR_DRAG
        }
        val residual = hypot(
            snapshot.delta.x - previousX * drag,
            snapshot.delta.z - previousZ * drag,
        )
        var acceleration = if (previous.onGround || snapshot.onGround) {
            maxOf(previous.groundAcceleration, snapshot.groundAcceleration)
        } else {
            airAcceleration(snapshot)
        }
        if (previous.onGround && !snapshot.onGround) acceleration += SPRINT_JUMP_IMPULSE
        val budget = acceleration * ACCELERATION_TOLERANCE * scale + PACKET_EPSILON
        return residual / budget.coerceAtLeast(1.0E-4)
    }

    private fun updateAirborneEnvelope(
        state: State,
        snapshot: PlayerSnapshot,
        previous: PlayerSnapshot?,
        gap: Int,
        contiguous: Boolean,
        scale: Double,
    ): Double {
        if (snapshot.onGround) {
            resetAirborneEnvelope(state)
            return 0.0
        }
        if (!contiguous || previous == null) {
            state.airborneCeiling = snapshot.horizontalSpeed
            state.airborneSamples = 0
            return 0.0
        }

        var limit = state.airborneCeiling ?: previous.horizontalSpeed
        repeat(gap) { step ->
            limit = limit * AIR_DRAG + airAcceleration(snapshot)
            if (step == 0 && previous.onGround) limit += SPRINT_JUMP_IMPULSE
        }
        state.airborneCeiling = limit
        state.airborneSamples++
        if (state.airborneSamples < MIN_AIRBORNE_SAMPLES) return 0.0

        val budget = limit * AIRBORNE_TOLERANCE * scale + PACKET_EPSILON
        return snapshot.horizontalSpeed / budget.coerceAtLeast(1.0E-4)
    }

    private fun airAcceleration(snapshot: PlayerSnapshot): Double {
        val attributeScale = (snapshot.effectiveSpeedAttribute / 0.1).coerceIn(1.0, 3.0)
        return AIR_ACCELERATION * attributeScale
    }

    private fun resetAirborneEnvelope(state: State) {
        state.airborneCeiling = null
        state.airborneSamples = 0
    }

    private fun clear(uuid: UUID): Evidence? {
        states.remove(uuid)
        return null
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }

    companion object {
        private const val AVERAGE_WINDOW = 8
        private const val AVERAGE_TOLERANCE = 1.14
        private const val ACCELERATION_TOLERANCE = 1.18
        private const val AIRBORNE_TOLERANCE = 1.10
        private const val AIR_DRAG = 0.91
        private const val AIR_ACCELERATION = 0.02
        private const val SPRINT_JUMP_IMPULSE = 0.2
        private const val PACKET_EPSILON = 0.006
        private const val MIN_AIRBORNE_SAMPLES = 3
        private const val MAX_PREDICTION_GAP = 3
    }
}

internal class FlightCheck : AntiCheatCheck {
    override val id = "flight_a"
    override val displayName = "Flight"

    private data class State(
        var physicsBuffer: Double = 0.0,
        var physicsSamples: Int = 0,
        var hover: Int = 0,
        var ascent: Int = 0,
    )
    private val states = HashMap<UUID, State>()

    override fun onTick(input: CheckTick): Evidence? {
        if (!CheatDetector.flight.value || input.exempt) return clear(input.tracked.uuid)
        val current = input.snapshot
        if (flightExempt(input, current)) return clear(input.tracked.uuid)
        if (current.moved && !current.predictableMovement) return clear(input.tracked.uuid)

        val state = states.getOrPut(input.tracked.uuid) { State() }
        if (current.onGround || input.tracked.airTicks < MIN_AIR_TICKS) {
            resetAirState(state)
            return null
        }
        if (current.verticalCollision && current.verticalObstruction) {
            resetAirState(state)
            return null
        }

        val gap = current.movementTickDelta.coerceAtLeast(1)
        val verticalRate = if (current.moved) current.delta.y / gap else 0.0
        val hovering = current.moved && !current.supportBelow && abs(verticalRate) < HOVER_VERTICAL_SPEED &&
            gap <= MAX_HOVER_PACKET_GAP
        state.hover = if (hovering) state.hover + 1 else 0

        val movement = input.tracked.movementSamples(2)
        val previous = movement.getOrNull(movement.lastIndex - 1)
        val contiguous = current.moved && previous != null && previous.predictableMovement &&
            current.tick - previous.tick == gap.toLong() && gap <= MAX_PHYSICS_GAP
        val deviation = if (
            contiguous && !previous.onGround &&
            !(previous.verticalCollision && previous.verticalObstruction)
        ) {
            val expected = predictedVerticalDisplacement(previous, current, gap)
            abs(current.delta.y - expected)
        } else {
            0.0
        }
        val tolerance = VERTICAL_BASE_TOLERANCE + VERTICAL_GAP_TOLERANCE * (gap - 1)
        val physicsRatio = deviation / tolerance
        state.physicsSamples = if (physicsRatio > 1.0) {
            state.physicsSamples + 1
        } else {
            (state.physicsSamples - 1).coerceAtLeast(0)
        }
        state.physicsBuffer = when {
            physicsRatio > 2.5 -> state.physicsBuffer + 2.5
            physicsRatio > 1.75 -> state.physicsBuffer + 1.6
            physicsRatio > 1.0 -> state.physicsBuffer + 0.8
            else -> (state.physicsBuffer - 0.55).coerceAtLeast(0.0)
        }.coerceAtMost(10.0)

        val unexpectedAscent = input.tracked.airTicks > 5 && verticalRate > 0.08 && physicsRatio > 1.0
        state.ascent = if (unexpectedAscent) state.ascent + 1 else (state.ascent - 1).coerceAtLeast(0)

        return when {
            state.hover >= HOVER_TICKS -> {
                state.hover = 2
                state.physicsBuffer = state.physicsBuffer.coerceAtLeast(1.0)
                Evidence(
                    id,
                    displayName,
                    2.4,
                    EvidenceConfidence.STRONG,
                    "unsupported airborne hover for $HOVER_TICKS ticks",
                )
            }
            state.ascent >= 3 -> {
                state.ascent = 1
                Evidence(id, displayName, 2.0, EvidenceConfidence.STRONG, "ascending without a valid impulse")
            }
            state.physicsSamples >= 3 && state.physicsBuffer >= 3.2 * CheatDetector.bufferScale() -> {
                state.physicsSamples = 1
                state.physicsBuffer = 1.0
                Evidence(
                    id,
                    displayName,
                    if (physicsRatio > 2.0) 2.2 else 1.4,
                    if (physicsRatio > 2.0) EvidenceConfidence.STRONG else EvidenceConfidence.SUSPICIOUS,
                    "gravity prediction missed by ${"%.4f".format(deviation)} " +
                        "($gap tick${if (gap == 1) "" else "s"})",
                )
            }
            else -> null
        }
    }

    private fun predictedVerticalDisplacement(
        previous: PlayerSnapshot,
        current: PlayerSnapshot,
        gap: Int,
    ): Double {
        var velocity = previous.delta.y / previous.movementTickDelta.coerceAtLeast(1)
        var displacement = 0.0
        val gravity = current.gravityAttribute.coerceIn(0.01, 0.2)
        repeat(gap) {
            velocity = (velocity - gravity) * VERTICAL_DRAG
            displacement += velocity
        }
        return displacement
    }

    private fun flightExempt(input: CheckTick, current: PlayerSnapshot): Boolean =
        current.passenger || current.fallFlying || current.swimming || current.inWater || current.inLava ||
            current.climbing || current.spectator || current.creativeFlying || current.insideMovementModifier ||
            current.slowFalling || current.levitating ||
            input.tracked.receivedVelocityWithin(input.tick, 12) || input.tracked.damagedWithin(input.tick, 8)

    private fun resetAirState(state: State) {
        state.physicsBuffer = 0.0
        state.physicsSamples = 0
        state.hover = 0
        state.ascent = 0
    }

    private fun clear(uuid: UUID): Evidence? {
        states.remove(uuid)
        return null
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }

    companion object {
        private const val VERTICAL_DRAG = 0.98
        private const val VERTICAL_BASE_TOLERANCE = 0.028
        private const val VERTICAL_GAP_TOLERANCE = 0.012
        private const val HOVER_VERTICAL_SPEED = 0.008
        private const val HOVER_TICKS = 5
        private const val MIN_AIR_TICKS = 3
        private const val MAX_PHYSICS_GAP = 3
        private const val MAX_HOVER_PACKET_GAP = 3
    }
}

// The server broadcasts an entity motion packet whenever it pushes a player
internal class AntiKnockbackCheck : AntiCheatCheck {
    override val id = "velocity_a"
    override val displayName = "Anti Knockback"

    private data class Pending(
        val startTick: Long,
        val direction: Vec3,
        val magnitude: Double,
        var travelled: Double = 0.0,
        var best: Double = 0.0,
        var invalid: Boolean = false,
    )

    private data class State(var pending: Pending? = null, var buffer: Int = 0)

    private val states = HashMap<UUID, State>()

    override fun onTick(input: CheckTick): Evidence? {
        if (!CheatDetector.antiKnockback.value) return clear(input.tracked.uuid)
        val snapshot = input.snapshot

        val knockback = input.tracked.pendingKnockback
        if (knockback != null) {
            input.tracked.clearKnockback()
            val state = states.getOrPut(input.tracked.uuid) { State() }
            state.pending = null
            // Only usable while the push is still fresh; a player who was out of
            // tracking range when it landed has already absorbed it.
            if (input.tick - input.tracked.lastVelocityTick > 2) return null
            if (input.exempt || unreliableTarget(snapshot)) return null

            val horizontal = hypot(knockback.x, knockback.z)
            val direction = Vec3(knockback.x / horizontal, 0.0, knockback.z / horizontal)
            // Someone sprinting into the hit cancels much of it legitimately,
            // since vanilla only halves their existing velocity before pushing.
            val approach = if (snapshot.moved) {
                -(snapshot.delta.x * direction.x + snapshot.delta.z * direction.z) /
                    snapshot.movementTickDelta.coerceAtLeast(1)
            } else {
                0.0
            }
            if (approach > MAX_APPROACH_SPEED) return null

            state.pending = Pending(input.tick, direction, horizontal)
            return null
        }

        val state = states[input.tracked.uuid] ?: return null
        val pending = state.pending ?: return null
        val elapsed = input.tick - pending.startTick
        if (elapsed <= 0) return null
        if (input.exempt || unreliableTarget(snapshot) || snapshot.horizontalCollision) pending.invalid = true

        if (snapshot.moved) {
            pending.travelled += snapshot.delta.x * pending.direction.x + snapshot.delta.z * pending.direction.z
            if (pending.travelled > pending.best) pending.best = pending.travelled
        }
        if (elapsed < RESOLVE_TICKS) return null

        state.pending = null
        if (pending.invalid) return null
        val expected = pending.magnitude * MIN_TRAVEL_FRACTION
        if (pending.best >= expected) {
            state.buffer = (state.buffer - 1).coerceAtLeast(0)
            return null
        }

        state.buffer++
        val required = (3 * CheatDetector.bufferScale()).toInt().coerceAtLeast(2)
        if (state.buffer < required) return null

        state.buffer = 1
        val retained = (pending.best / pending.magnitude.coerceAtLeast(1.0E-4) / MIN_TRAVEL_FRACTION)
            .coerceIn(0.0, 1.0)
        return Evidence(
            id,
            displayName,
            if (retained < 0.15) 2.6 else 1.8,
            if (retained < 0.15) EvidenceConfidence.STRONG else EvidenceConfidence.SUSPICIOUS,
            "kept ${(retained * 100).toInt()}% of a ${"%.2f".format(pending.magnitude)} knockback",
        )
    }

    private fun unreliableTarget(snapshot: PlayerSnapshot): Boolean =
        snapshot.passenger || snapshot.spectator || snapshot.creativeFlying || snapshot.fallFlying ||
            snapshot.swimming || snapshot.inWater || snapshot.inLava || snapshot.climbing ||
            snapshot.levitating

    private fun clear(uuid: UUID): Evidence? {
        states.remove(uuid)
        return null
    }

    override fun remove(uuid: UUID) {
        states.remove(uuid)
    }

    override fun reset() {
        states.clear()
    }

    companion object {
        // Long enough for a high-ping player's movement packets to catch up
        private const val RESOLVE_TICKS = 10L

        // Vanilla carries a knockback about 2.4x its magnitude across this window.
        private const val MIN_TRAVEL_FRACTION = 0.5

        // Blocks per tick of movement into the push above which the sample is discarded
        private const val MAX_APPROACH_SPEED = 0.1
    }
}

private fun movementExempt(input: CheckTick, velocityTicks: Int): Boolean {
    val snapshot = input.snapshot
    return snapshot.passenger || snapshot.fallFlying || snapshot.swimming || snapshot.spectator ||
        snapshot.creativeFlying || input.tracked.receivedVelocityWithin(input.tick, velocityTicks) ||
        input.tracked.damagedWithin(input.tick, 6)
}

private val PlayerSnapshot.predictableMovement: Boolean
    get() = positionUpdateCount == 1 && !positionSynchronized
