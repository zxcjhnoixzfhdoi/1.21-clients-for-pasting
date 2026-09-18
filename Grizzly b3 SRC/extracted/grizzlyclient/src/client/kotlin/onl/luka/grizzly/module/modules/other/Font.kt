package onl.luka.grizzly.module.modules.other

import onl.luka.grizzly.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

object Font : Module("Font", "Customize the font used in GUI and HUD elements", Category.OTHER) {

    override val isProtected = true
    override val showInModulesList = false
    init { enabled.value = true }

    enum class FontChoice(val namespace: String, val path: String) {
        MINECRAFT("medved", "minecraft"),
        INTER("medved", "inter"),
        ROBOTO("medved", "roboto"),
        ARIAL("medved", "arial"),
        SAN_FRANCISCO("medved", "sanfrancisco"),
        JETBRAINS_MONO("medved", "jetbrains_mono"),
        COMIC_RELIEF("medved", "comic_relief"),
        IMPACT("medved", "impact"),
        UNIFORM("minecraft", "uniform"),
    }

    enum class FontOverride {
        DEFAULT,
        MINECRAFT,
        INTER,
        ROBOTO,
        ARIAL,
        SAN_FRANCISCO,
        JETBRAINS_MONO,
        COMIC_RELIEF,
        IMPACT,
        UNIFORM;

        val choice: FontChoice? get() = if (this == DEFAULT) null else FontChoice.valueOf(name)
    }

    val fontChoice = enum("font", FontChoice.INTER)
    private val renderScale = ThreadLocal.withInitial { 1.0f }
    private val override = ThreadLocal<FontChoice?>()

    fun getFont(): Font = Minecraft.getInstance().font

    fun <T> withRenderScale(scale: Float, block: () -> T): T {
        val previous = renderScale.get()
        renderScale.set(previous * scale.coerceAtLeast(0.01f))
        return try {
            block()
        } finally {
            renderScale.set(previous)
        }
    }

    // DEFAULT arrives here as null, so an element can sit inside an outer override and still
    // fall back to the client font.
    fun <T> withOverride(choice: FontChoice?, block: () -> T): T {
        val previous = override.get()
        override.set(choice)
        return try {
            block()
        } finally {
            override.set(previous)
        }
    }

    fun activeChoice(): FontChoice = override.get() ?: fontChoice.value

    fun fontStyle(): Style {
        val choice = activeChoice()
        val path = choice.pathForCurrentScale()
        val desc = FontDescription.Resource(Identifier.fromNamespaceAndPath(choice.namespace, path))
        return Style.EMPTY.withFont(desc)
    }

    fun styledText(text: String): Component =
        Component.literal(text).withStyle(fontStyle())

    // Server-authored components carry their own colours and formatting but no font, so the style
    // is merged in rather than replaced: withStyle keeps every field the component already set.
    fun styledText(component: Component): Component =
        component.copy().withStyle(fontStyle())

    /**
     * Picks the atlas whose oversample matches the physical size the glyphs will end up at, so the
     * common scales land on a 1:1 blit instead of a fractional resample. The old two bucket switch
     * meant anything that was not exactly 2x or 4x got softened on the way to the screen.
     */
    private fun FontChoice.pathForCurrentScale(): String {
        if (namespace != "medved") return path

        val physicalScale = Minecraft.getInstance().window.guiScale.toFloat() * renderScale.get()
        val tier = physicalScale.roundToInt().coerceIn(1, MAX_TIER)
        return if (tier == 1) path else "${path}_${tier}x"
    }

    private const val MAX_TIER = 4
}
