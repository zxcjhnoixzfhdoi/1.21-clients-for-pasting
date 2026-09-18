package onl.luka.grizzly.command

import com.mojang.brigadier.context.StringRange
import com.mojang.brigadier.suggestion.Suggestion
import com.mojang.brigadier.suggestion.Suggestions
import onl.luka.grizzly.module.modules.other.Commands

object CommandEngine {

    @JvmStatic
    fun clientPrefixChar(): Char? = Commands.clientPrefix.value.firstOrNull()

    @JvmStatic
    fun isCustomPrefix(c: Char): Boolean =
        c == clientPrefixChar() || c == PublicCommands.prefixChar()

    @JvmStatic
    fun executeClientCommand(line: String): Boolean {
        val prefix = clientPrefixChar() ?: return false
        if (!line.startsWith(prefix)) return false
        ClientCommands.execute(line)
        return true
    }

    @JvmStatic
    fun suggestions(input: String, cursor: Int): Suggestions? {
        if (input.isEmpty()) return null
        val prefix = input[0]
        if (!isCustomPrefix(prefix)) return null

        val end = cursor.coerceIn(0, input.length)
        val start = input.lastIndexOf(' ', end - 1) + 1
        val word = input.substring(start, end)
        val range = StringRange.between(start, end)

        val candidates = if (prefix == clientPrefixChar()) {
            ClientCommands.suggestions(input, word)
        } else {
            PublicCommands.names()
        }
        val suggestions = candidates
            .filter { it.startsWith(word, ignoreCase = true) }
            .map { Suggestion(range, it) }
        return Suggestions(range, suggestions)
    }

    @JvmStatic
    fun handleIncomingChat(raw: String) {
        PublicCommands.handleIncomingChat(raw)
    }
}
