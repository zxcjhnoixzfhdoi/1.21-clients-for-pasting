package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import onl.luka.grizzly.module.modules.utility.CheatDetector
import onl.luka.grizzly.util.NotificationManager
import java.util.UUID
import kotlin.math.max

// A live, in-session summary of one player's violations
internal data class SessionFlag(
    val uuid: UUID,
    val name: String,
    val topCheck: String,
    val topVl: Double,
    val checks: Int,
    val detections: Int,
)

// Decaying violation levels drive alerts; crossing the detection threshold writes to the database.
internal object ViolationManager {
    private class CheckState {
        var vl: Double = 0.0
        var peakVl: Double = 0.0
        var lastEvidenceTick: Long = 0
        var lastAlertMs: Long = 0
        var lastAlertVl: Double = 0.0
        var lastCommitTick: Long = Long.MIN_VALUE
        var strongSinceCommit: Boolean = false
    }

    private class PlayerState(var name: String) {
        val checks = LinkedHashMap<String, CheckState>()
        var detections: Int = 0
    }

    private val players = HashMap<UUID, PlayerState>()

    fun tick(tick: Long) {
        val playerIterator = players.iterator()
        while (playerIterator.hasNext()) {
            val (_, player) = playerIterator.next()
            val checkIterator = player.checks.iterator()
            while (checkIterator.hasNext()) {
                val state = checkIterator.next().value
                if (tick - state.lastEvidenceTick > 20) state.vl = (state.vl - 0.025).coerceAtLeast(0.0)
                if (state.vl == 0.0 && tick - state.lastEvidenceTick > 400) checkIterator.remove()
            }
            if (player.checks.isEmpty()) playerIterator.remove()
        }
    }

    fun add(client: Minecraft, player: TrackedPlayer, evidence: Evidence, tick: Long) {
        val playerState = players.getOrPut(player.uuid) { PlayerState(player.name) }
        playerState.name = player.name
        val state = playerState.checks.getOrPut(evidence.checkId) { CheckState() }
        state.vl = (state.vl + evidence.amount).coerceAtMost(50.0)
        state.peakVl = max(state.peakVl, state.vl)
        state.lastEvidenceTick = tick
        if (evidence.confidence == EvidenceConfidence.STRONG) state.strongSinceCommit = true

        commitIfConclusive(client, player, playerState, state, evidence, tick)

        val threshold = CheatDetector.alertThreshold.value.toDouble()
        val now = System.currentTimeMillis()
        val reachedThreshold = state.vl >= threshold
        val verbose = CheatDetector.verboseEvidence.value
        val requiredGain = if (verbose) 0.5 else threshold.coerceAtMost(1.5)
        val gainedEnough = state.lastAlertMs == 0L || state.vl - state.lastAlertVl >= requiredGain
        if ((!reachedThreshold && !verbose) || !gainedEnough) return
        if (now - state.lastAlertMs < CheatDetector.alertCooldownMs.value) return

        state.lastAlertMs = now
        state.lastAlertVl = state.vl
        alert(client, player, evidence, state.vl)
    }

    // Promotes a sustained violation level into a permanent detection
    private fun commitIfConclusive(
        client: Minecraft,
        player: TrackedPlayer,
        playerState: PlayerState,
        state: CheckState,
        evidence: Evidence,
        tick: Long,
    ) {
        if (!CheatDetector.recordDetections.value) return
        if (state.vl < CheatDetector.detectionVl.value) return
        if (state.lastCommitTick != Long.MIN_VALUE && tick - state.lastCommitTick < COMMIT_COOLDOWN_TICKS) return
        // Ignore server-side NPCs and holograms, which never hold a player list entry.
        val info = client.connection?.getPlayerInfo(player.uuid) ?: return

        state.lastCommitTick = tick
        playerState.detections++
        val strong = state.strongSinceCommit
        state.strongSinceCommit = false

        val outcome = CheatDatabase.recordDetection(
            uuid = player.uuid,
            name = info.profile.name ?: player.profileName,
            checkId = evidence.checkId,
            vl = state.vl,
            strong = strong,
            detail = evidence.detail,
            server = AntiCheatEngine.serverId(),
            session = AntiCheatEngine.sessionId(),
        )
        if (outcome.escalated) announceVerdict(client, outcome.record)
    }

    private fun announceVerdict(client: Minecraft, record: CheatRecord) {
        val (label, color) = when (record.verdict) {
            CheatVerdict.CHEATER -> "CHEATER" to ChatFormatting.RED
            CheatVerdict.SUSPECT -> "SUSPECT" to ChatFormatting.GOLD
            CheatVerdict.CLEAN -> return
        }
        val checks = record.topChecks(3).joinToString(", ") { (id, stat) -> "${checkLabel(id)} x${stat.hits}" }
        val message = Component.literal("[AC] ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(record.name).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" recorded as ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(label).withStyle(color))
            .append(
                Component.literal(" (score ${"%.1f".format(record.effectiveScore())}, $checks)")
                    .withStyle(ChatFormatting.DARK_GRAY),
            )
        client.player?.sendSystemMessage(message)
        NotificationManager.show("Cheat Detector: $label", record.name, 5000L)
    }

    // Chat warning shown the first time a recorded player is seen on a server
    fun announceKnownPlayer(client: Minecraft, record: CheatRecord) {
        val (label, color) = when (record.verdict) {
            CheatVerdict.CHEATER -> "known cheater" to ChatFormatting.RED
            CheatVerdict.SUSPECT -> "suspected cheater" to ChatFormatting.GOLD
            CheatVerdict.CLEAN -> return
        }
        val checks = record.topChecks(3).joinToString(", ") { (id, stat) -> "${checkLabel(id)} x${stat.hits}" }
        val sessions = record.sessionCount
        val message = Component.literal("[AC] ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(record.name).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" is a ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(label).withStyle(color))
            .append(
                Component.literal(" - ${record.detections} detections over $sessions sessions ($checks)")
                    .withStyle(ChatFormatting.DARK_GRAY),
            )
        client.player?.sendSystemMessage(message)
        if (record.verdict == CheatVerdict.CHEATER) {
            NotificationManager.show("Known cheater", record.name, 5000L)
        }
    }

    private fun alert(client: Minecraft, player: TrackedPlayer, evidence: Evidence, vl: Double) {
        val message = Component.literal("[AC] ").withStyle(ChatFormatting.DARK_AQUA)
            .append(Component.literal(player.name).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" flagged ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(evidence.checkName).withStyle(ChatFormatting.RED))
            .append(Component.literal(" VL ${"%.1f".format(vl)}").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(" (${evidence.detail})").withStyle(ChatFormatting.DARK_GRAY))

        when (CheatDetector.alertMode.value) {
            CheatDetector.AlertMode.CHAT -> client.player?.sendSystemMessage(message)
            CheatDetector.AlertMode.NOTIFICATION -> showNotification(player, evidence, vl)
            CheatDetector.AlertMode.BOTH -> {
                client.player?.sendSystemMessage(message)
                showNotification(player, evidence, vl)
            }
        }
    }

    private fun showNotification(player: TrackedPlayer, evidence: Evidence, vl: Double) {
        NotificationManager.show(
            "Cheat Detector: ${evidence.checkName}",
            "${player.name} - VL ${"%.1f".format(vl)}",
            3200L,
        )
    }

    fun suspiciousPlayers(threshold: Double): Int =
        players.count { (_, player) -> player.checks.values.any { it.vl >= threshold } }

    // Everything flagged since the world was joined, worst first
    fun sessionFlags(): List<SessionFlag> =
        players.mapNotNull { (uuid, player) ->
            val top = player.checks.maxByOrNull { it.value.peakVl } ?: return@mapNotNull null
            SessionFlag(
                uuid = uuid,
                name = player.name,
                topCheck = checkLabel(top.key),
                topVl = top.value.peakVl,
                checks = player.checks.size,
                detections = player.detections,
            )
        }.sortedByDescending { it.topVl }

    fun checkLabel(checkId: String): String = CHECK_LABELS[checkId] ?: checkId

    fun remove(uuid: UUID) {
        players.remove(uuid)
    }

    fun reset() {
        players.clear()
    }

    private const val COMMIT_COOLDOWN_TICKS = 400L

    private val CHECK_LABELS = mapOf(
        "noslow_a" to "No Slow",
        "autoblock_a" to "Auto Block",
        "scaffold" to "Scaffold",
        "legit_scaffold_a" to "Legit Scaffold",
        "killaura_a" to "Kill Aura",
        "silentaim_a" to "Silent Aim",
        "reach_a" to "Reach",
        "autoclicker_a" to "Auto Clicker",
        "speed_a" to "Speed",
        "flight_a" to "Flight",
        "velocity_a" to "Anti Knockback",
        "invalidrot_a" to "Invalid Rotation",
    )
}
