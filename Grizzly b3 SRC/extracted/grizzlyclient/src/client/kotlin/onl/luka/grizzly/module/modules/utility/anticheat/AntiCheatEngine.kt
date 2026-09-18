package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import onl.luka.grizzly.module.modules.utility.CheatDetector
import java.util.UUID

object AntiCheatEngine {
    private data class PlacementCandidate(
        val tracker: TrackedPlayer,
        val actionTick: Long,
        val distance: Double,
        val score: Double,
        val hadSwing: Boolean,
    )

    private val trackers = HashMap<Int, TrackedPlayer>()
    private val lagMonitor = LagMonitor()
    private val checks = listOf<AntiCheatCheck>(
        NoSlowCheck(),
        SpeedCheck(),
        FlightCheck(),
        AntiKnockbackCheck(),
        AutoBlockCheck(),
        KillAuraCheck(),
        SilentAimCheck(),
        ReachCheck(),
        AutoClickerCheck(),
        InvalidRotationCheck(),
        ScaffoldFaceCheck(),
        ScaffoldSnapCheck(),
        LegitScaffoldCheck(),
    )

    /** Identifies the current world session so the database can count separate encounters. */
    private var session = UUID.randomUUID().toString()
    private val greeted = HashSet<UUID>()

    private data class PendingDamage(val victimId: Int, val sourceCauseId: Int, val sourceDirectId: Int)

    private val pendingDamage = ArrayList<PendingDamage>()
    private val pendingBlockChanges = ArrayList<AntiCheatSignal.BlockChange>()
    private var tickCounter = 0L

    @JvmStatic
    fun tick(client: Minecraft) {
        val level = client.level
        val localPlayer = client.player
        if (level == null || localPlayer == null) {
            if (tickCounter != 0L) reset()
            return
        }
        tickCounter++
        pendingDamage.clear()
        pendingBlockChanges.clear()
        ProtocolDetector.refresh(client, tickCounter)

        // Keep a local victim proxy so remote attacks against the client are analyzable.
        // It is not included in the suspects passed to checks below.
        val localTracked = trackers[localPlayer.id] ?: createTracker(
            localPlayer,
            localPlayer.position(),
            localPlayer.yRot,
            localPlayer.xRot,
        )
        localTracked.absoluteMove(
            localPlayer.position(),
            localPlayer.yRot,
            localPlayer.xRot,
            localPlayer.onGround(),
            false,
            tickCounter,
        )

        AntiCheatPacketObserver.drain { signal -> processSignal(client, signal) }
        localTracked.capture(client, localPlayer, tickCounter)

        val maxDistanceSq = CheatDetector.maxDistance.value.toDouble().let { it * it }
        val activeSnapshots = LinkedHashMap<TrackedPlayer, PlayerSnapshot>()
        val presentIds = HashSet<Int>()
        for (entity in level.players()) {
            if (entity === localPlayer || entity.isRemoved || entity.isDeadOrDying) continue
            if (localPlayer.distanceToSqr(entity) > maxDistanceSq) continue
            presentIds += entity.id
            val tracked = trackers[entity.id] ?: createTracker(entity, entity.position(), entity.yRot, entity.xRot)
            activeSnapshots[tracked] = tracked.capture(client, entity, tickCounter)
        }

        val staleIds = trackers.keys.filter { it !in presentIds && level.getEntity(it) == null }
        staleIds.forEach(::removeTracker)

        lagMonitor.update(tickCounter, activeSnapshots.values)
        val globallyLagging = lagMonitor.isLagging(tickCounter)
        for ((tracked, snapshot) in activeSnapshots) {
            val entity = level.getEntity(tracked.entityId) as? Player ?: continue
            val input = CheckTick(client, entity, tracked, snapshot, tickCounter, globallyLagging)
            checks.forEach { check ->
                check.onTick(input)?.let { ViolationManager.add(client, tracked, it, tickCounter) }
            }
        }

        processDamageEvents(client, globallyLagging)
        processBlockChanges(client, globallyLagging)
        ViolationManager.tick(tickCounter)
        if (tickCounter % KNOWN_PLAYER_SCAN_TICKS == 0L) warnKnownPlayers(client)
        CheatDatabase.flush()
    }

    private fun warnKnownPlayers(client: Minecraft) {
        val connection = client.connection ?: return
        val localId = client.player?.uuid
        for (info in connection.listedOnlinePlayers) {
            val uuid = info.profile.id ?: continue
            if (uuid == localId || !greeted.add(uuid)) continue
            val record = CheatDatabase.lookup(uuid) ?: continue
            CheatDatabase.touch(uuid, info.profile.name ?: record.name)
            if (record.verdict == CheatVerdict.CLEAN || !CheatDetector.warnOnJoin.value) continue
            ViolationManager.announceKnownPlayer(client, record)
        }
    }

    fun sessionId(): String = session

    fun serverId(): String =
        Minecraft.getInstance().getCurrentServer()?.ip?.lowercase() ?: "singleplayer"

    private fun processSignal(client: Minecraft, signal: AntiCheatSignal) {
        val level = client.level ?: return
        when (signal) {
            is AntiCheatSignal.Spawn -> {
                val entity = level.getEntity(signal.entityId) as? Player ?: return
                if (entity === client.player) return
                trackers[signal.entityId] ?: createTracker(entity, signal.position, signal.yaw, signal.pitch)
            }
            is AntiCheatSignal.RelativeMove -> {
                val tracked = trackerFor(client, signal.entityId) ?: return
                tracked.relativeMove(signal.position, signal.yaw, signal.pitch, signal.onGround)
            }
            is AntiCheatSignal.AbsoluteMove -> {
                val tracked = trackerFor(client, signal.entityId) ?: return
                val largeCorrection = tracked.serverPosition.distanceToSqr(signal.position) > 16.0
                tracked.absoluteMove(
                    signal.position,
                    signal.yaw,
                    signal.pitch,
                    signal.onGround,
                    signal.teleport || largeCorrection,
                    tickCounter,
                )
            }
            is AntiCheatSignal.Swing -> {
                if (signal.action == 0 || signal.action == 3) {
                    trackerFor(client, signal.entityId)?.markSwing(tickCounter, signal.receivedNanos)
                }
            }
            is AntiCheatSignal.Damage -> {
                trackerFor(client, signal.victimId)?.markDamage(tickCounter)
                val existingIndex = pendingDamage.indexOfFirst { it.victimId == signal.victimId }
                val next = PendingDamage(signal.victimId, signal.sourceCauseId, signal.sourceDirectId)
                if (existingIndex < 0) {
                    pendingDamage += next
                } else if (pendingDamage[existingIndex].sourceCauseId <= 0 && signal.sourceCauseId > 0) {
                    pendingDamage[existingIndex] = next
                }
            }
            is AntiCheatSignal.Velocity ->
                trackerFor(client, signal.entityId)?.markVelocity(tickCounter, signal.movement)
            is AntiCheatSignal.BlockChange -> pendingBlockChanges += signal
            is AntiCheatSignal.Remove -> signal.entityIds.forEach(::removeTracker)
        }
    }

    private fun processDamageEvents(client: Minecraft, globallyLagging: Boolean) {
        val level = client.level ?: return
        for (damage in pendingDamage) {
            val victim = trackers[damage.victimId] ?: continue
            val directAttacker = trackers[damage.sourceCauseId]
            val meleeSource = damage.sourceDirectId <= 0 || damage.sourceDirectId == damage.sourceCauseId
            if (directAttacker != null && !meleeSource) continue
            val attacker = directAttacker ?: inferRecentAttacker(victim)
            if (attacker == null || attacker === victim) continue
            if (attacker.entityId == client.player?.id) continue
            val attackerEntity = level.getEntity(attacker.entityId) as? Player ?: continue
            val victimEntity = level.getEntity(victim.entityId) as? Player ?: continue
            val hadSwing = attacker.swungWithin(tickCounter, 3)
            if (!hadSwing && directAttacker == null) continue
            val actionTick = if (hadSwing) attacker.lastSwingTick else tickCounter

            attacker.lastAttackTick = tickCounter
            val event = AttackEvent(
                attacker,
                victim,
                attackerEntity,
                victimEntity,
                tickCounter,
                actionTick,
                directAttacker != null && meleeSource,
                hadSwing,
            )
            checks.forEach { check ->
                check.onAttack(client, event, globallyLagging)?.let {
                    ViolationManager.add(client, attacker, it, tickCounter)
                }
            }
        }
    }

    private fun inferRecentAttacker(victim: TrackedPlayer): TrackedPlayer? {
        val candidates = trackers.values.asSequence()
            .filter { it !== victim && it.swungWithin(tickCounter, 2) }
            .map { it to it.serverPosition.distanceToSqr(victim.serverPosition) }
            .filter { it.second <= 42.25 }
            .sortedBy { it.second }
            .take(2)
            .toList()
        val nearest = candidates.firstOrNull() ?: return null
        val second = candidates.getOrNull(1)
        if (second != null && second.second - nearest.second < 1.0) return null
        return nearest.first
    }

    private fun processBlockChanges(client: Minecraft, globallyLagging: Boolean) {
        if (pendingBlockChanges.isEmpty() || globallyLagging) return
        val level = client.level ?: return
        for (change in pendingBlockChanges) {
            if (!change.wasAir || change.isAir) continue
            val center = Vec3.atCenterOf(change.position)
            val candidates = trackers.values.asSequence()
                .filter { it.entityId != client.player?.id && !it.isInGrace(tickCounter, CheatDetector.joinGraceTicks.value) }
                .mapNotNull { tracker -> placementCandidate(tracker, center) }
                .filter { it.distance <= 5.5 }
                .sortedBy { it.score }
                .take(2)
                .toList()
            val nearest = candidates.firstOrNull() ?: continue
            val second = candidates.getOrNull(1)
            val attributionGap = second?.let { it.score - nearest.score } ?: Double.POSITIVE_INFINITY
            val requiredGap = if (nearest.hadSwing) 0.6 else 1.25
            if (attributionGap < requiredGap || (!nearest.hadSwing && nearest.distance > 4.75)) continue

            val tracker = nearest.tracker
            val entity = level.getEntity(tracker.entityId) as? Player ?: continue
            tracker.lastPlacementTick = tickCounter
            val event = PlacementEvent(
                tracker,
                entity,
                change.position,
                tickCounter,
                nearest.actionTick,
                attributionGap,
                nearest.hadSwing,
            )
            checks.forEach { check ->
                check.onPlacement(client, event, globallyLagging)?.let {
                    ViolationManager.add(client, tracker, it, tickCounter)
                }
            }
        }
    }

    private fun placementCandidate(tracker: TrackedPlayer, center: Vec3): PlacementCandidate? {
        val recent = tracker.snapshotsBetween((tickCounter - 5).coerceAtLeast(0), tickCounter)
            .filter { it.holdingBlock && !it.passenger && !it.spectator && !it.creativeFlying }
        if (recent.isEmpty()) return null

        val hadSwing = tracker.swungWithin(tickCounter, 4)
        val action = if (hadSwing) {
            tracker.snapshotAtOrBefore(tracker.lastSwingTick)
        } else {
            recent.asReversed().firstOrNull { it.rotated } ?: recent.last()
        } ?: return null
        val distance = action.position.distanceTo(center)
        val score = distance - if (hadSwing) 0.35 else 0.0
        return PlacementCandidate(tracker, action.tick, distance, score, hadSwing)
    }

    private fun trackerFor(client: Minecraft, entityId: Int): TrackedPlayer? {
        trackers[entityId]?.let { return it }
        val entity = client.level?.getEntity(entityId) as? Player ?: return null
        if (entity === client.player) return null
        return createTracker(entity, entity.position(), entity.yRot, entity.xRot)
    }

    private fun createTracker(entity: Player, position: Vec3, yaw: Float, pitch: Float): TrackedPlayer {
        return TrackedPlayer(
            entity.id,
            entity.uuid,
            entity.displayName.string,
            position,
            yaw,
            pitch,
            tickCounter,
        ).also { trackers[entity.id] = it }
    }

    private fun removeTracker(entityId: Int) {
        val tracker = trackers.remove(entityId) ?: return
        checks.forEach { it.remove(tracker.uuid) }
        ViolationManager.remove(tracker.uuid)
    }

    fun hudInfo(): String {
        val localId = Minecraft.getInstance().player?.id
        val trackedCount = trackers.keys.count { it != localId }
        val suspicious = ViolationManager.suspiciousPlayers(CheatDetector.alertThreshold.value.toDouble())
        val base = if (suspicious > 0) "$trackedCount tracked, $suspicious flagged" else "$trackedCount tracked"
        return if (CheatDetector.usesLegacyCombatRules()) "$base, legacy" else base
    }

    @JvmStatic
    fun reset() {
        trackers.clear()
        pendingDamage.clear()
        pendingBlockChanges.clear()
        checks.forEach(AntiCheatCheck::reset)
        ViolationManager.reset()
        lagMonitor.reset()
        AntiCheatPacketObserver.clear()
        ProtocolDetector.reset()
        greeted.clear()
        session = UUID.randomUUID().toString()
        CheatDatabase.save()
        tickCounter = 0L
    }

    private const val KNOWN_PLAYER_SCAN_TICKS = 20L
}
