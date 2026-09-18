package onl.luka.grizzly.command

import net.minecraft.client.Minecraft
import onl.luka.grizzly.module.modules.utility.anticheat.CheatDatabase
import onl.luka.grizzly.module.modules.utility.anticheat.CheatRecord
import onl.luka.grizzly.module.modules.utility.anticheat.CheatVerdict
import onl.luka.grizzly.module.modules.utility.anticheat.ViolationManager
import java.util.UUID
import java.util.concurrent.TimeUnit

// `.cheaters` - reads and edits the local cheater database
object CheaterCommands {

    private val subcommands = listOf("list", "info", "mark", "suspect", "forget", "session", "purge", "wipe")

    fun execute(args: List<String>) {
        val prefix = CommandEngine.clientPrefixChar() ?: return
        when (args.firstOrNull()?.lowercase()) {
            null, "list" -> list()
            "info" -> info(args.drop(1))
            "mark" -> mark(args.drop(1), CheatVerdict.CHEATER)
            "suspect" -> mark(args.drop(1), CheatVerdict.SUSPECT)
            "forget" -> forget(args.drop(1))
            "session" -> session()
            "purge" -> purge()
            "wipe" -> wipe(args.drop(1))
            else -> CommandFeedback.chat("Unknown option. Use ${prefix}cheaters ${subcommands.joinToString("|")}.")
        }
    }

    fun suggestions(tokens: List<String>): List<String> = when (tokens.size) {
        2 -> subcommands
        3 -> when (tokens[1].lowercase()) {
            "info", "mark", "suspect", "forget" -> CheatDatabase.all().map { it.name }.sorted()
            "wipe" -> listOf("confirm")
            else -> emptyList()
        }
        else -> emptyList()
    }

    fun help(prefix: Char) {
        CommandFeedback.chat("  ${prefix}cheaters list - show recorded players")
        CommandFeedback.chat("  ${prefix}cheaters info <name> - full record for one player")
        CommandFeedback.chat("  ${prefix}cheaters session - players flagged since you joined")
        CommandFeedback.chat("  ${prefix}cheaters mark|suspect <name> - set a verdict by hand")
        CommandFeedback.chat("  ${prefix}cheaters forget <name> - delete one record")
        CommandFeedback.chat("  ${prefix}cheaters purge - drop records past the retention window")
        CommandFeedback.chat("  ${prefix}cheaters wipe confirm - delete the whole database")
    }

    private fun list() {
        val records = CheatDatabase.ranked()
        if (records.isEmpty()) {
            CommandFeedback.chat("No players recorded yet.")
            return
        }
        val cheaters = records.count { it.verdict == CheatVerdict.CHEATER }
        val suspects = records.count { it.verdict == CheatVerdict.SUSPECT }
        CommandFeedback.chat("${records.size} recorded - $cheaters cheaters, $suspects suspects:")
        records.take(LIST_LIMIT).forEach { record ->
            CommandFeedback.chat(
                "  ${record.name} - ${label(record)} " +
                    "score ${"%.1f".format(record.effectiveScore())}, " +
                    "${record.detections} detections, ${record.distinctChecks} checks, " +
                    "last ${ago(record.lastDetection)}",
            )
        }
        if (records.size > LIST_LIMIT) CommandFeedback.chat("  ...and ${records.size - LIST_LIMIT} more.")
    }

    private fun info(args: List<String>) {
        val record = resolve(args) ?: return
        CommandFeedback.chat("${record.name} - ${label(record)}${if (record.manual) " (set by hand)" else ""}")
        CommandFeedback.chat(
            "  score ${"%.1f".format(record.effectiveScore())} of ${"%.1f".format(record.score)} raw, " +
                "${record.detections} detections across ${record.sessionCount} sessions",
        )
        CommandFeedback.chat("  first seen ${ago(record.firstSeen)}, last detection ${ago(record.lastDetection)}")
        if (record.servers.isNotEmpty()) CommandFeedback.chat("  servers: ${record.servers.joinToString(", ")}")
        record.topChecks(8).forEach { (id, stat) ->
            CommandFeedback.chat(
                "  ${ViolationManager.checkLabel(id)}: ${stat.hits} hits (${stat.strongHits} strong), " +
                    "peak VL ${"%.1f".format(stat.peakVl)} - ${stat.lastDetail}",
            )
        }
    }

    private fun session() {
        val flags = ViolationManager.sessionFlags()
        if (flags.isEmpty()) {
            CommandFeedback.chat("Nothing flagged this session.")
            return
        }
        CommandFeedback.chat("Flagged this session:")
        flags.take(LIST_LIMIT).forEach { flag ->
            CommandFeedback.chat(
                "  ${flag.name} - ${flag.topCheck} peak VL ${"%.1f".format(flag.topVl)}, " +
                    "${flag.checks} checks, ${flag.detections} recorded",
            )
        }
    }

    private fun mark(args: List<String>, verdict: CheatVerdict) {
        if (args.isEmpty()) {
            CommandFeedback.chat("Usage: ${CommandEngine.clientPrefixChar()}cheaters mark <name>")
            return
        }
        val name = args[0]
        val existing = CheatDatabase.find(name)
        val uuid = existing?.let { runCatching { UUID.fromString(it.uuid) }.getOrNull() }
            ?: onlineUuid(name)
        if (uuid == null) {
            CommandFeedback.chat("'$name' is not recorded and is not online, so there is no UUID to store.")
            return
        }
        val record = CheatDatabase.mark(uuid, existing?.name ?: onlineName(name) ?: name, verdict)
        CommandFeedback.chat("Marked ${record.name} as ${label(record)}.")
    }

    private fun forget(args: List<String>) {
        val record = resolve(args) ?: return
        val name = record.name
        if (CheatDatabase.forget(record)) {
            CommandFeedback.chat("Removed $name from the database.")
        } else {
            CommandFeedback.chat("Could not remove $name.")
        }
    }

    private fun purge() {
        val removed = CheatDatabase.prune()
        CheatDatabase.save()
        CommandFeedback.chat("Dropped $removed stale records.")
    }

    private fun wipe(args: List<String>) {
        if (args.firstOrNull()?.lowercase() != "confirm") {
            val prefix = CommandEngine.clientPrefixChar() ?: return
            CommandFeedback.chat(
                "This deletes all ${CheatDatabase.all().size} records. " +
                    "Run ${prefix}cheaters wipe confirm to continue.",
            )
            return
        }
        CommandFeedback.chat("Deleted ${CheatDatabase.clear()} records.")
    }

    private fun resolve(args: List<String>): CheatRecord? {
        if (args.isEmpty()) {
            CommandFeedback.chat("Usage: ${CommandEngine.clientPrefixChar()}cheaters info <name>")
            return null
        }
        val record = CheatDatabase.find(args[0])
        if (record == null) CommandFeedback.chat("No record for '${args[0]}'.")
        return record
    }

    private fun onlineUuid(name: String): UUID? = onlineInfo(name)?.profile?.id

    private fun onlineName(name: String): String? = onlineInfo(name)?.profile?.name

    private fun onlineInfo(name: String) =
        Minecraft.getInstance().connection?.listedOnlinePlayers
            ?.firstOrNull { it.profile.name.equals(name, ignoreCase = true) }

    private fun label(record: CheatRecord): String = when (record.verdict) {
        CheatVerdict.CHEATER -> "cheater"
        CheatVerdict.SUSPECT -> "suspect"
        CheatVerdict.CLEAN -> "clean"
    }

    private fun ago(timestamp: Long): String {
        if (timestamp <= 0L) return "never"
        val elapsed = System.currentTimeMillis() - timestamp
        val days = TimeUnit.MILLISECONDS.toDays(elapsed)
        if (days > 0) return "${days}d ago"
        val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
        if (hours > 0) return "${hours}h ago"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
        return if (minutes > 0) "${minutes}m ago" else "just now"
    }

    private const val LIST_LIMIT = 15
}
