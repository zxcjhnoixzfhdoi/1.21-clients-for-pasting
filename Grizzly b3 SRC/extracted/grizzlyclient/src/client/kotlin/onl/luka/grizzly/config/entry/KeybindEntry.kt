package onl.luka.grizzly.config.entry

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import onl.luka.grizzly.module.modules.movement.InvMove
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

/**
 * An integer entry holding a GLFW key code.
 *
 * In addition to the standard [onChange] listener inherited from [ConfigEntry],
 * keybinds expose an [onPress] event that fires when the key first goes down
 * (i.e. leading-edge detection, not repeat). The key is polled each rendered
 * frame by [ConfigManager], independently of the game timer.
 *
 * Use [GLFW.GLFW_KEY_UNKNOWN] (-1) to represent an unbound key.
 */
class KeybindEntry(name: String, default: Int) : ConfigEntry<Int>(name, default) {
    companion object {
        private const val MOUSE_CODE_BASE = -100

        fun mouseButtonCode(button: Int): Int = MOUSE_CODE_BASE - button

        fun isMouseButton(code: Int): Boolean = code <= MOUSE_CODE_BASE

        fun mouseButton(code: Int): Int = MOUSE_CODE_BASE - code
    }

    private val pressListeners = mutableListOf<() -> Unit>()
    private var wasDown = false

    /** Register a listener that fires once when the key is first pressed. */
    fun onPress(listener: () -> Unit): KeybindEntry {
        pressListeners += listener
        return this
    }

    /**
     * Pretend the key was already down, so the next poll won't fire a press event.
     * Call this immediately after programmatically assigning a new key value.
     */
    fun suppressNextPress() { wasDown = true }

    /** Called every rendered frame by [onl.luka.grizzly.config.ConfigManager]. */
    fun tick() {
        if (value == GLFW.GLFW_KEY_UNKNOWN) {
            wasDown = false
            return
        }
        val mc = Minecraft.getInstance()
        val window = mc.window.handle()
        val isDown = if (isMouseButton(value)) {
            GLFW.glfwGetMouseButton(window, mouseButton(value)) == GLFW.GLFW_PRESS
        } else {
            GLFW.glfwGetKey(window, value) == GLFW.GLFW_PRESS
        }
        if (!allowsClientKeybinds(mc)) {
            wasDown = isDown
            return
        }
        if (isDown && !wasDown) {
            pressListeners.forEach { it() }
        }
        wasDown = isDown
    }

    private fun allowsClientKeybinds(client: Minecraft): Boolean =
        client.gui.screen() == null || InvMove.allowsClientKeybindsInCurrentScreen(client)

    override fun toJson(): JsonElement = JsonPrimitive(value)
    override fun fromJson(element: JsonElement) { element.intOrNull?.let { value = it } }
}
