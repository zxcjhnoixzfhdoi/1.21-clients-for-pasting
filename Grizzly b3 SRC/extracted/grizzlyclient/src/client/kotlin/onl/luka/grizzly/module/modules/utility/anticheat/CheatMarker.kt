package onl.luka.grizzly.module.modules.utility.anticheat

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.network.chat.contents.TranslatableContents
import onl.luka.grizzly.module.modules.utility.CheatDetector
import java.util.UUID

// Warning badge for recorded players, spliced into the tab list, chat and nametags.
object CheatMarker {

    fun verdictFor(uuid: UUID): CheatVerdict? = markable(CheatDatabase.lookup(uuid))

    private fun markable(record: CheatRecord?): CheatVerdict? {
        if (record == null || !CheatDetector.isEnabled()) return null
        return when (record.verdict) {
            CheatVerdict.CHEATER -> CheatVerdict.CHEATER
            CheatVerdict.SUSPECT -> CheatVerdict.SUSPECT.takeIf { CheatDetector.markSuspects.value }
            CheatVerdict.CLEAN -> null
        }
    }

    fun icon(): String = CheatDetector.warningIcon.value.ifBlank { DEFAULT_ICON }

    private fun color(verdict: CheatVerdict): ChatFormatting = when (verdict) {
        CheatVerdict.CHEATER -> ChatFormatting.RED
        CheatVerdict.SUSPECT -> ChatFormatting.GOLD
        CheatVerdict.CLEAN -> ChatFormatting.GRAY
    }

    // The badge itself, carrying a summary of why the player is on the list
    private fun badge(record: CheatRecord, verdict: CheatVerdict): MutableComponent {
        val label = if (verdict == CheatVerdict.CHEATER) "Known cheater" else "Suspected cheater"
        val checks = record.topChecks(4).joinToString("\n") { (id, stat) ->
            "  ${ViolationManager.checkLabel(id)} x${stat.hits}"
        }
        val tooltip = Component.literal(label).withStyle(color(verdict))
            .append(
                Component.literal(
                    "\n${record.name}\n" +
                        "${record.detections} detections over ${record.sessionCount} sessions\n" +
                        "score ${"%.1f".format(record.effectiveScore())}\n$checks",
                ).withStyle(ChatFormatting.GRAY),
            )
        return Component.literal(icon())
            .withStyle(
                Style.EMPTY.withColor(color(verdict)).withHoverEvent(HoverEvent.ShowText(tooltip)),
            )
    }

    // Prefixes a rendered player name with the badge
    fun decorateName(uuid: UUID, name: Component): Component {
        val record = CheatDatabase.lookup(uuid) ?: return name
        val verdict = markable(record) ?: return name
        return Component.empty().append(badge(record, verdict)).append(" ").append(name)
    }

    // Plain-text variant for the nametag renderer, which works on strings
    fun namePrefix(uuid: UUID): String {
        val record = CheatDatabase.lookup(uuid) ?: return ""
        markable(record) ?: return ""
        return "${icon()} "
    }

    // Walks a chat message and inserts the badge in front of every recorded name it mentions
    fun decorateChat(message: Component): Component {
        if (!CheatDetector.isEnabled() || !CheatDetector.markChat.value) return message
        val index = CheatDatabase.nameIndex()
        if (index.isEmpty()) return message
        val names = index
            .mapNotNull { (name, record) -> markable(record)?.let { name to record } }
            .toMap()
        if (names.isEmpty()) return message
        // Cheap guard against marking an already marked message twice.
        if (message.string.contains(icon())) return message
        return transform(message, names)
    }

    private fun transform(component: Component, names: Map<String, CheatRecord>): Component {
        val contents = component.contents
        val rebuilt: MutableComponent = when (contents) {
            is PlainTextContents -> splitLiteral(contents.text(), names)
            is TranslatableContents -> {
                val args = contents.args.map { if (it is Component) transform(it, names) else it }
                MutableComponent.create(
                    TranslatableContents(contents.key, contents.fallback, args.toTypedArray()),
                )
            }
            else -> MutableComponent.create(contents)
        }
        rebuilt.setStyle(component.style)
        component.siblings.forEach { rebuilt.append(transform(it, names)) }
        return rebuilt
    }

    private fun splitLiteral(text: String, names: Map<String, CheatRecord>): MutableComponent {
        val matches = findMatches(text, names)
        if (matches.isEmpty()) return Component.literal(text)

        val result = Component.empty()
        var cursor = 0
        for ((start, record) in matches) {
            if (start > cursor) result.append(Component.literal(text.substring(cursor, start)))
            val verdict = markable(record) ?: continue
            result.append(badge(record, verdict))
            result.append(Component.literal(text.substring(start, start + record.name.length)))
            cursor = start + record.name.length
        }
        if (cursor < text.length) result.append(Component.literal(text.substring(cursor)))
        return result
    }

    // Non-overlapping name hits, in the order they appear
    private fun findMatches(text: String, names: Map<String, CheatRecord>): List<Pair<Int, CheatRecord>> {
        val lower = text.lowercase()
        val found = ArrayList<Pair<Int, CheatRecord>>()
        for ((name, record) in names) {
            var index = lower.indexOf(name)
            while (index >= 0) {
                val end = index + name.length
                val startsCleanly = index == 0 || !isNameChar(text[index - 1])
                val endsCleanly = end >= text.length || !isNameChar(text[end])
                if (startsCleanly && endsCleanly) found += index to record
                index = lower.indexOf(name, index + 1)
            }
        }
        found.sortBy { it.first }

        var cursor = 0
        return found.filter { (start, record) ->
            if (start < cursor) return@filter false
            cursor = start + record.name.length
            true
        }
    }

    private fun isNameChar(character: Char): Boolean = character.isLetterOrDigit() || character == '_'

    private const val DEFAULT_ICON = "⚠"
}
