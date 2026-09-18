package onl.luka.grizzly.module.modules.utility

import net.minecraft.client.Minecraft
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.modules.utility.anticheat.AntiCheatEngine
import onl.luka.grizzly.module.modules.utility.anticheat.CheatDatabase
import onl.luka.grizzly.module.modules.utility.anticheat.ProtocolDetector
import onl.luka.grizzly.util.NotificationManager

object CheatDetector : Module(
    "Cheat Detector",
    "Observes remote players, reports movement, combat, and placement anomalies, and records repeat offenders.",
    Category.UTILITY,
) {
    enum class Sensitivity { LENIENT, STANDARD, STRICT }
    enum class AlertMode { CHAT, NOTIFICATION, BOTH }
    enum class Protocol { AUTO, MODERN, LEGACY }

    private val checksGroup = group("Checks")
    val noSlow = boolean("No Slow", true).inGroup(checksGroup)
    val autoBlock = boolean("Auto Block", true).inGroup(checksGroup)
    val scaffold = boolean("Scaffold", true).inGroup(checksGroup)
    val legitScaffold = boolean("Legit Scaffold", false).inGroup(checksGroup)
    val killAura = boolean("Kill Aura", true).inGroup(checksGroup)
    val silentAim = boolean("Silent Aim", true).inGroup(checksGroup)
    val reach = boolean("Reach", true).inGroup(checksGroup)
    val autoClicker = boolean("Auto Clicker", true).inGroup(checksGroup)
    val speed = boolean("Speed", true).inGroup(checksGroup)
    val flight = boolean("Flight", true).inGroup(checksGroup)
    val antiKnockback = boolean("Anti Knockback", true).inGroup(checksGroup)
    val invalidRotation = boolean("Invalid Rotation", true).inGroup(checksGroup)

    private val analysisGroup = group("Analysis")
    val sensitivity = enum("Sensitivity", Sensitivity.STANDARD).inGroup(analysisGroup)
    val protocol = enum("Protocol", Protocol.AUTO).inGroup(analysisGroup)
    val maxDistance = int("Max Distance", 96, 16, 256).inGroup(analysisGroup)
    val joinGraceTicks = int("Join Grace Ticks", 60, 20, 200).inGroup(analysisGroup)

    private val alertsGroup = group("Alerts")
    val alertMode = enum("Alert Mode", AlertMode.CHAT).inGroup(alertsGroup)
    val alertThreshold = float("Alert Threshold", 6f, 1f, 20f).inGroup(alertsGroup)
    val alertCooldownMs = int("Alert Cooldown (ms)", 3000, 250, 15000).inGroup(alertsGroup)
    val verboseEvidence = boolean("Show All Evidence", false).also {
        it.aliases("Verbose Evidence")
    }.inGroup(alertsGroup)

    private val warningsGroup = group("Warnings")
    val warnOnJoin = boolean("Warn On Join", true).inGroup(warningsGroup)
    val markSuspects = boolean("Mark Suspects", true).inGroup(warningsGroup)
    val markTabList = boolean("Mark Tab List", true).inGroup(warningsGroup)
    val markChat = boolean("Mark Chat", true).inGroup(warningsGroup)
    val markNametags = boolean("Mark Nametags", true).inGroup(warningsGroup)
    val warningIcon = string("Warning Icon", "⚠").inGroup(warningsGroup)

    private val databaseGroup = group("Database")
    val recordDetections = boolean("Record Detections", true).inGroup(databaseGroup)
    val detectionVl = float("Detection VL", 12f, 4f, 40f).inGroup(databaseGroup)
    val convictionScore = float("Conviction Score", 8f, 2f, 40f).inGroup(databaseGroup)
    val retentionDays = int("Retention (days)", 90, 1, 730).inGroup(databaseGroup)
    val clearDatabase = button("Clear Database", "Clear") {
        val removed = CheatDatabase.clear()
        NotificationManager.showConfig("Cheat Detector", "Cleared $removed database entries")
    }.inGroup(databaseGroup)

    override fun onEnabled() {
        AntiCheatEngine.reset()
    }

    override fun onDisabled() {
        AntiCheatEngine.reset()
        CheatDatabase.save()
    }

    override fun onTick(client: Minecraft) {
        AntiCheatEngine.tick(client)
    }

    fun thresholdScale(): Double = when (sensitivity.value) {
        Sensitivity.LENIENT -> 1.18
        Sensitivity.STANDARD -> 1.0
        Sensitivity.STRICT -> 0.9
    }

    // Buffer sizes scale inversely with sensitivity, so strict mode also flags sooner
    fun bufferScale(): Double = when (sensitivity.value) {
        Sensitivity.LENIENT -> 1.35
        Sensitivity.STANDARD -> 1.0
        Sensitivity.STRICT -> 0.75
    }

    fun usesLegacyCombatRules(): Boolean = when (protocol.value) {
        Protocol.LEGACY -> true
        Protocol.MODERN -> false
        Protocol.AUTO -> ProtocolDetector.isLegacy()
    }
}
