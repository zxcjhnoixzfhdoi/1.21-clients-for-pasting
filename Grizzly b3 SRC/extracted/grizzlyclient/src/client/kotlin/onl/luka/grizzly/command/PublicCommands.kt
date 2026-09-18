package onl.luka.grizzly.command

import net.minecraft.client.Minecraft
import onl.luka.grizzly.module.modules.other.Commands

object PublicCommands {

    private data class PublicCommand(val name: String, val reply: () -> String)

    private val commands = listOf(
        PublicCommand("ping") { pingReply() },
        PublicCommand("tps") { tpsReply() },
        PublicCommand("help") { helpReply() },
    )

    private val byName = commands.associateBy { it.name }
    private val cooldowns = mutableMapOf<String, Long>()

    private const val COOLDOWN_MS = 1000L

    fun prefixChar(): Char? = Commands.prefix.value.firstOrNull()

    fun names(): List<String> {
        val prefix = prefixChar() ?: return emptyList()
        return commands.map { "$prefix${it.name}" }
    }

    @JvmStatic
    fun handleIncomingChat(raw: String) {
        val module = Commands
        if (!module.isEnabled() || !module.publicCommands.value) return
        val prefix = prefixChar() ?: return

        val clean = raw.replace(Regex("§."), "")
        val channel = detectChannel(clean)
        if (module.partyGuildOnly.value && isHypixel() && channel == Channel.PUBLIC) return

        val match = commandToken(prefix).find(clean) ?: return
        val name = match.groupValues[1].lowercase()
        val command = byName[name] ?: return
        execute(command)
    }

    private fun execute(command: PublicCommand) {
        val now = System.currentTimeMillis()
        val last = cooldowns[command.name]
        if (last != null && now - last < COOLDOWN_MS) return
        cooldowns[command.name] = now

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        player.connection.sendChat(command.reply())
    }

    private fun pingReply(): String {
        val mc = Minecraft.getInstance()
        val player = mc.player
        val latency = player?.connection?.getPlayerInfo(player.uuid)?.latency
        val ping = if (latency != null) latency.toLong() else mc.getCurrentServer()?.ping ?: -1L
        return if (ping >= 0) "Pong! ${ping}ms" else "Pong! ?ms"
    }

    private fun helpReply(): String =
        "Available commands: ${commands.joinToString(", ") { it.name }}"

    private fun tpsReply(): String = "TPS: %.1f".format(TpsTracker.current())

    private enum class Channel { PUBLIC, PARTY, GUILD }

    private fun detectChannel(clean: String): Channel {
        val lower = clean.lowercase()
        return when {
            lower.startsWith("party > ") || lower.startsWith("[party] ") -> Channel.PARTY
            lower.startsWith("guild > ") || lower.startsWith("[guild] ") -> Channel.GUILD
            else -> Channel.PUBLIC
        }
    }

    private fun isHypixel(): Boolean =
        Minecraft.getInstance().getCurrentServer()?.ip?.lowercase()?.contains("hypixel") == true

    private fun commandToken(prefix: Char): Regex =
        Regex("(?:^|[:>]\\s*)" + Regex.escape(prefix.toString()) + "([a-zA-Z0-9_-]+)")
}
