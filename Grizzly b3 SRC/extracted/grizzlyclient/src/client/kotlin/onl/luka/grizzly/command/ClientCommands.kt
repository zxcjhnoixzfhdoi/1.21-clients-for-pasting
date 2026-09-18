package onl.luka.grizzly.command

import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.ModuleManager
import onl.luka.grizzly.module.modules.other.ClickGui
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

object ClientCommands {

    private val unbindAliases = setOf("none", "unbind", "clear")

    fun execute(line: String) {
        val prefix = CommandEngine.clientPrefixChar() ?: return
        val args = line.substring(1).trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        when (args.firstOrNull()?.lowercase()) {
            null -> CommandFeedback.chat("Client commands: ${prefix}help, ${prefix}bind, ${prefix}toggle, ${prefix}say, ${prefix}vclip, ${prefix}clickgui, ${prefix}cheaters")
            "help" -> help()
            "bind" -> bind(args.drop(1))
            "toggle" -> toggle(args.drop(1))
            "say" -> say(args.drop(1))
            "vclip" -> vclip(args.drop(1))
            "clickgui" -> openClickGui()
            "cheaters" -> CheaterCommands.execute(args.drop(1))
            else -> CommandFeedback.chat("Unknown command '$prefix${args[0]}'. Use ${prefix}help.")
        }
    }

    fun suggestions(input: String, word: String): List<String> {
        val prefix = CommandEngine.clientPrefixChar() ?: return emptyList()
        val commandNames = listOf(
            "${prefix}bind",
            "${prefix}toggle",
            "${prefix}say",
            "${prefix}vclip",
            "${prefix}clickgui",
            "${prefix}cheaters",
            "${prefix}help",
        )
        val tokens = input.split(' ')
        return when {
            tokens.size <= 1 -> commandNames
            tokens[0] == "${prefix}bind" && tokens.size == 2 -> moduleNames()
            tokens[0] == "${prefix}bind" && tokens.size == 3 -> KeyNames.suggestions()
            tokens[0] == "${prefix}toggle" && tokens.size == 2 -> moduleNames()
            tokens[0] == "${prefix}cheaters" -> CheaterCommands.suggestions(tokens)
            else -> emptyList()
        }
    }

    private fun help() {
        val p = CommandEngine.clientPrefixChar() ?: return
        CommandFeedback.chat("Client commands:")
        CommandFeedback.chat("  ${p}bind <module> <key> - bind a key to a module")
        CommandFeedback.chat("  ${p}toggle <module> - toggle a module on/off")
        CommandFeedback.chat("  ${p}say <message> - send a message that starts with the client prefix")
        CommandFeedback.chat("  ${p}vclip <blocks> - move vertically by a signed distance")
        CommandFeedback.chat("  ${p}clickgui - open the click GUI")
        CommandFeedback.chat("  ${p}help - show this list")
        CheaterCommands.help(p)
    }

    private fun bind(args: List<String>) {
        val p = CommandEngine.clientPrefixChar() ?: return
        if (args.isEmpty()) {
            CommandFeedback.chat("Usage: ${p}bind <module> <key> (or ${p}bind <module> none to unbind)")
            return
        }
        val module = findModule(args[0])
        if (module == null) {
            CommandFeedback.chat("Module '${args[0]}' not found.")
            return
        }
        if (module.isProtected) {
            CommandFeedback.chat("'${module.name}' is protected and cannot be bound.")
            return
        }
        if (args.size == 1) {
            CommandFeedback.chat("${module.name} is bound to ${KeyNames.displayName(module.keybind.value)}.")
            return
        }
        val keyRaw = args[1]
        if (keyRaw.lowercase() in unbindAliases) {
            module.keybind.value = GLFW.GLFW_KEY_UNKNOWN
            CommandFeedback.chat("Unbound ${module.name}.")
            return
        }
        val code = KeyNames.parse(keyRaw)
        if (code == null) {
            CommandFeedback.chat("Unknown key '${keyRaw}'.")
            return
        }
        module.keybind.value = code
        module.keybind.suppressNextPress()
        CommandFeedback.chat("Bound ${module.name} to ${KeyNames.displayName(code)}.")
    }

    private fun toggle(args: List<String>) {
        val p = CommandEngine.clientPrefixChar() ?: return
        if (args.isEmpty()) {
            CommandFeedback.chat("Usage: ${p}toggle <module>")
            return
        }
        val module = findModule(args[0])
        if (module == null) {
            CommandFeedback.chat("Module '${args[0]}' not found.")
            return
        }
        if (module.isProtected) {
            CommandFeedback.chat("'${module.name}' is protected and cannot be toggled.")
            return
        }
        module.toggle()
        CommandFeedback.chat("${module.name} ${if (module.isEnabled()) "enabled" else "disabled"}.")
    }

    private fun say(args: List<String>) {
        val p = CommandEngine.clientPrefixChar() ?: return
        if (args.isEmpty()) {
            CommandFeedback.chat("Usage: ${p}say <message>")
            return
        }

        val player = Minecraft.getInstance().player
        if (player == null) {
            CommandFeedback.chat("You must be connected to a server to send chat messages.")
            return
        }

        player.connection.sendChat(args.joinToString(" "))
    }

    private fun vclip(args: List<String>) {
        val p = CommandEngine.clientPrefixChar() ?: return
        if (args.size != 1) {
            CommandFeedback.chat("Usage: ${p}vclip <blocks>")
            return
        }

        val distance = args[0].toDoubleOrNull()
        if (distance == null || !distance.isFinite()) {
            CommandFeedback.chat("'${args[0]}' is not a valid distance.")
            return
        }

        val player = Minecraft.getInstance().player
        if (player == null) {
            CommandFeedback.chat("You must be in a world to use vclip.")
            return
        }

        val velocity = player.deltaMovement
        player.setPos(player.x, player.y + distance, player.z)
        player.setDeltaMovement(velocity.x, 0.0, velocity.z)
        CommandFeedback.chat("Clipped ${formatDistance(distance)} blocks vertically.")
    }

    private fun formatDistance(distance: Double): String =
        if (distance % 1.0 == 0.0) distance.toLong().toString() else distance.toString()

    private fun openClickGui() {
        val mc = Minecraft.getInstance()
        ClickGui.enable()
        if (mc.gui.screen() !is onl.luka.grizzly.gui.ClickGui) {
            mc.gui.setScreen(onl.luka.grizzly.gui.ClickGui())
        }
    }

    private fun findModule(query: String): Module? {
        val normalized = normalize(query)
        return ModuleManager.getAll().firstOrNull { normalize(it.name) == normalized }
    }

    private fun moduleNames(): List<String> =
        ModuleManager.getAll()
            .filter { !it.isProtected }
            .map { normalize(it.name) }
            .sorted()

    private fun normalize(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]"), "")
}
