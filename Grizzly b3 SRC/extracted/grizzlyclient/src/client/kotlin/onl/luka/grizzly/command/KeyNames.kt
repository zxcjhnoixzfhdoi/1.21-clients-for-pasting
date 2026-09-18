package onl.luka.grizzly.command

import com.mojang.blaze3d.platform.InputConstants
import onl.luka.grizzly.config.entry.KeybindEntry
import org.lwjgl.glfw.GLFW

object KeyNames {

    private val mouseAliases = mapOf(
        "lmb" to "key.mouse.left",
        "rmb" to "key.mouse.right",
        "mmb" to "key.mouse.middle",
        "mouse3" to "key.mouse.3",
        "mouse4" to "key.mouse.4",
        "mouse5" to "key.mouse.5",
        "mouse6" to "key.mouse.6",
        "mouse7" to "key.mouse.7",
        "mouse8" to "key.mouse.8",
    )

    private val keyboardAliases = mapOf(
        "esc" to "escape",
        "return" to "enter",
        "spacebar" to "space",
        "caps" to "caps.lock",
        "capslock" to "caps.lock",
        "lshift" to "left.shift",
        "rshift" to "right.shift",
        "lctrl" to "left.control",
        "rctrl" to "right.control",
        "lcontrol" to "left.control",
        "rcontrol" to "right.control",
        "lalt" to "left.alt",
        "ralt" to "right.alt",
        "del" to "delete",
        "ins" to "insert",
        "pgup" to "page.up",
        "pgdn" to "page.down",
        "minus" to "minus",
        "equals" to "equal",
        "period" to "period",
        "comma" to "comma",
        "semicolon" to "semicolon",
        "apostrophe" to "apostrophe",
        "slash" to "slash",
        "backslash" to "backslash",
        "grave" to "grave",
        "lbracket" to "left.bracket",
        "rbracket" to "right.bracket",
    )

    fun parse(raw: String): Int? {
        val name = raw.trim().lowercase()
        if (name.isEmpty()) return null
        val resolved = mouseAliases[name] ?: "key.keyboard.${keyboardAliases[name] ?: name}"
        val key = runCatching { InputConstants.getKey(resolved) }.getOrNull() ?: return null
        return when (key.type) {
            InputConstants.Type.KEYSYM -> if (key.value == GLFW.GLFW_KEY_UNKNOWN) null else key.value
            InputConstants.Type.MOUSE -> KeybindEntry.mouseButtonCode(key.value)
            else -> null
        }
    }

    fun displayName(code: Int): String {
        if (code == GLFW.GLFW_KEY_UNKNOWN) return "None"
        return if (KeybindEntry.isMouseButton(code)) {
            InputConstants.Type.MOUSE.getOrCreate(KeybindEntry.mouseButton(code)).getDisplayName().string
        } else {
            InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().string
        }
    }

    fun suggestions(): List<String> {
        val keys = mutableListOf<String>()
        ('a'..'z').forEach { keys += it.toString() }
        ('0'..'9').forEach { keys += it.toString() }
        (1..12).forEach { keys += "f$it" }
        keys += listOf(
            "esc", "tab", "caps", "lshift", "rshift", "lctrl", "rctrl", "lalt", "ralt",
            "space", "enter", "backspace", "delete", "insert", "home", "end", "pgup", "pgdn",
            "up", "down", "left", "right",
            "lmb", "rmb", "mmb", "mouse3", "mouse4", "mouse5",
        )
        return keys
    }
}
