package onl.luka.grizzly.gui.helpers

import com.mojang.blaze3d.platform.InputConstants
import onl.luka.grizzly.gui.ClickGui
import onl.luka.grizzly.gui.helpers.commitEditingColor
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW

internal object InputHelper {

    fun handleKeyPressed(gui: ClickGui, event: KeyEvent): Boolean {
        val key = event.key()
        val mc = Minecraft.getInstance()

        if (gui.listeningKeybind != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                gui.listeningKeybind!!.value = GLFW.GLFW_KEY_UNKNOWN
                gui.listeningKeybind = null
            } else {
                gui.listeningKeybind!!.value = key
                gui.listeningKeybind!!.suppressNextPress()
                gui.listeningKeybind = null
            }
            return true
        }

        if (gui.metaFieldActive != null) {
            val f = gui.metaField
            val ctrl = (event.modifiers() and GLFW.GLFW_MOD_CONTROL) != 0
            val shift = (event.modifiers() and GLFW.GLFW_MOD_SHIFT) != 0
            when {
                key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER -> gui.commitMetaField()
                key == GLFW.GLFW_KEY_ESCAPE -> gui.commitMetaField()
                ctrl && key == GLFW.GLFW_KEY_A -> f.selectAll()
                ctrl && key == GLFW.GLFW_KEY_C -> {
                    val copied = f.copy()
                    if (copied.isNotEmpty()) mc.keyboardHandler.clipboard = copied
                }
                ctrl && key == GLFW.GLFW_KEY_X -> {
                    val cut = f.cut()
                    if (cut.isNotEmpty()) mc.keyboardHandler.clipboard = cut
                }
                ctrl && key == GLFW.GLFW_KEY_V -> f.insert(mc.keyboardHandler.clipboard)
                key == GLFW.GLFW_KEY_BACKSPACE -> if (ctrl) f.backspaceWord() else f.backspace()
                key == GLFW.GLFW_KEY_DELETE -> f.deleteForward()
                key == GLFW.GLFW_KEY_LEFT -> if (ctrl) f.wordMove(false, shift) else f.move(-1, shift)
                key == GLFW.GLFW_KEY_RIGHT -> if (ctrl) f.wordMove(true, shift) else f.move(1, shift)
                key == GLFW.GLFW_KEY_HOME -> f.home(shift)
                key == GLFW.GLFW_KEY_END -> f.end(shift)
            }
            f.clampScroll(gui.metaFieldVisibleW)
            return true
        }

        if (gui.presetFieldActive) {
            val f = gui.presetField
            val ctrl = (event.modifiers() and GLFW.GLFW_MOD_CONTROL) != 0
            val shift = (event.modifiers() and GLFW.GLFW_MOD_SHIFT) != 0
            when {
                key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER -> gui.presetFieldActive = false
                key == GLFW.GLFW_KEY_ESCAPE -> gui.presetFieldActive = false
                ctrl && key == GLFW.GLFW_KEY_A -> f.selectAll()
                ctrl && key == GLFW.GLFW_KEY_C -> {
                    val s = f.copy()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_X -> {
                    val s = f.cut()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_V -> {
                    val clip = mc.keyboardHandler.clipboard.replace(Regex("[</*?\"\\>:|]+"), "_")
                    f.insert(clip)
                }
                key == GLFW.GLFW_KEY_BACKSPACE -> if (ctrl) f.backspaceWord() else f.backspace()
                key == GLFW.GLFW_KEY_DELETE -> f.deleteForward()
                key == GLFW.GLFW_KEY_LEFT -> if (ctrl) f.wordMove(false, shift) else f.move(-1, shift)
                key == GLFW.GLFW_KEY_RIGHT -> if (ctrl) f.wordMove(true, shift) else f.move(1, shift)
                key == GLFW.GLFW_KEY_HOME -> f.home(shift)
                key == GLFW.GLFW_KEY_END -> f.end(shift)
            }
            f.clampScroll(gui.presetFieldVisibleW)
            gui.presetNameBuffer = f.text
            return true
        }

        if (gui.editingColorEntry != null) {
            val f = gui.entryField
            val ctrl = (event.modifiers() and GLFW.GLFW_MOD_CONTROL) != 0
            val shift = (event.modifiers() and GLFW.GLFW_MOD_SHIFT) != 0
            when {
                key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER -> {
                    gui.commitEditingColor()
                    gui.editingColorEntry = null
                    gui.editingColorChannel = null
                    gui.editingColorHex = false
                }
                key == GLFW.GLFW_KEY_ESCAPE -> {
                    gui.editingColorEntry = null
                    gui.editingColorChannel = null
                    gui.editingColorHex = false
                }
                key == GLFW.GLFW_KEY_BACKSPACE -> if (ctrl) f.backspaceWord() else f.backspace()
                key == GLFW.GLFW_KEY_DELETE -> f.deleteForward()
                key == GLFW.GLFW_KEY_LEFT -> if (ctrl) f.wordMove(false, shift) else f.move(-1, shift)
                key == GLFW.GLFW_KEY_RIGHT -> if (ctrl) f.wordMove(true, shift) else f.move(1, shift)
                key == GLFW.GLFW_KEY_HOME -> f.home(shift)
                key == GLFW.GLFW_KEY_END -> f.end(shift)
                ctrl && key == GLFW.GLFW_KEY_A -> f.selectAll()
                ctrl && key == GLFW.GLFW_KEY_C -> {
                    val s = f.copy()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_X -> {
                    val s = f.cut()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_V -> f.insert(mc.keyboardHandler.clipboard)
            }
            f.clampScroll(gui.entryFieldVisibleW)
            return true
        }

        if (gui.editingString != null) {
            val f = gui.entryField
            val ctrl = (event.modifiers() and GLFW.GLFW_MOD_CONTROL) != 0
            val shift = (event.modifiers() and GLFW.GLFW_MOD_SHIFT) != 0
            when {
                key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER -> {
                    gui.editingString!!.value = f.text
                    gui.editingString = null
                }
                key == GLFW.GLFW_KEY_ESCAPE -> {
                    gui.editingString!!.value = f.text
                    gui.editingString = null
                }
                key == GLFW.GLFW_KEY_BACKSPACE -> if (ctrl) f.backspaceWord() else f.backspace()
                key == GLFW.GLFW_KEY_DELETE -> f.deleteForward()
                key == GLFW.GLFW_KEY_LEFT -> if (ctrl) f.wordMove(false, shift) else f.move(-1, shift)
                key == GLFW.GLFW_KEY_RIGHT -> if (ctrl) f.wordMove(true, shift) else f.move(1, shift)
                key == GLFW.GLFW_KEY_HOME -> f.home(shift)
                key == GLFW.GLFW_KEY_END -> f.end(shift)
                ctrl && key == GLFW.GLFW_KEY_A -> f.selectAll()
                ctrl && key == GLFW.GLFW_KEY_C -> {
                    val s = f.copy()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_X -> {
                    val s = f.cut()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_V -> f.insert(mc.keyboardHandler.clipboard)
            }
            f.clampScroll(gui.entryFieldVisibleW)
            return true
        }

        if (gui.editingProfileName != null) {
            val f = gui.entryField
            val ctrl = (event.modifiers() and GLFW.GLFW_MOD_CONTROL) != 0
            val shift = (event.modifiers() and GLFW.GLFW_MOD_SHIFT) != 0
            when {
                key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER -> gui.commitProfileName()
                key == GLFW.GLFW_KEY_ESCAPE -> gui.commitProfileName()
                key == GLFW.GLFW_KEY_BACKSPACE -> if (ctrl) f.backspaceWord() else f.backspace()
                key == GLFW.GLFW_KEY_DELETE -> f.deleteForward()
                key == GLFW.GLFW_KEY_LEFT -> if (ctrl) f.wordMove(false, shift) else f.move(-1, shift)
                key == GLFW.GLFW_KEY_RIGHT -> if (ctrl) f.wordMove(true, shift) else f.move(1, shift)
                key == GLFW.GLFW_KEY_HOME -> f.home(shift)
                key == GLFW.GLFW_KEY_END -> f.end(shift)
                ctrl && key == GLFW.GLFW_KEY_A -> f.selectAll()
                ctrl && key == GLFW.GLFW_KEY_C -> {
                    val s = f.copy()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_X -> {
                    val s = f.cut()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_V -> f.insert(mc.keyboardHandler.clipboard)
            }
            f.clampScroll(gui.entryFieldVisibleW)
            return true
        }

        if (gui.editingItemListSearch) {
            val f = gui.itemListSearch
            val previousText = f.text
            val ctrl = (event.modifiers() and GLFW.GLFW_MOD_CONTROL) != 0
            val shift = (event.modifiers() and GLFW.GLFW_MOD_SHIFT) != 0
            when {
                key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER -> gui.editingItemListSearch = false
                key == GLFW.GLFW_KEY_ESCAPE -> gui.editingItemListSearch = false
                key == GLFW.GLFW_KEY_BACKSPACE -> if (ctrl) f.backspaceWord() else f.backspace()
                key == GLFW.GLFW_KEY_DELETE -> f.deleteForward()
                key == GLFW.GLFW_KEY_LEFT -> if (ctrl) f.wordMove(false, shift) else f.move(-1, shift)
                key == GLFW.GLFW_KEY_RIGHT -> if (ctrl) f.wordMove(true, shift) else f.move(1, shift)
                key == GLFW.GLFW_KEY_HOME -> f.home(shift)
                key == GLFW.GLFW_KEY_END -> f.end(shift)
                ctrl && key == GLFW.GLFW_KEY_A -> f.selectAll()
                ctrl && key == GLFW.GLFW_KEY_C -> {
                    val s = f.copy()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_X -> {
                    val s = f.cut()
                    if (s.isNotEmpty()) mc.keyboardHandler.clipboard = s
                }
                ctrl && key == GLFW.GLFW_KEY_V -> f.insert(mc.keyboardHandler.clipboard)
            }
            if (f.text != previousText) gui.itemListScroll = 0
            f.clampScroll(200)
            return true
        }

        return false
    }

    fun handleKeyReleased(gui: ClickGui, event: KeyEvent): Boolean {
        if (gui.listeningKeybind != null || gui.editingString != null || gui.editingColorEntry != null ||
            gui.editingProfileName != null || gui.editingItemListSearch || gui.presetFieldActive ||
            gui.metaFieldActive != null) return true
        val inputKey = InputConstants.getKey(event)
        val mc = Minecraft.getInstance()
        val physHeld = GLFW.glfwGetKey(mc.window.handle(), event.key()) == GLFW.GLFW_PRESS
        if (!physHeld) KeyMapping.set(inputKey, false)
        return false
    }

    fun handleCharTyped(gui: ClickGui, event: CharacterEvent): Boolean {
        if (gui.metaFieldActive != null) {
            gui.metaField.insert(event.codepointAsString())
            gui.metaField.clampScroll(gui.metaFieldVisibleW)
            return true
        }
        if (gui.presetFieldActive) {
            val ch = event.codepointAsString()
            if (ch.matches(Regex("[^</*?\"\\>:|]+"))) {
                gui.presetField.insert(ch)
                gui.presetField.clampScroll(gui.presetFieldVisibleW)
                gui.presetNameBuffer = gui.presetField.text
            }
            return true
        }
        if (gui.editingColorEntry != null) {
            gui.entryField.insert(event.codepointAsString())
            gui.entryField.clampScroll(gui.entryFieldVisibleW)
            return true
        }
        if (gui.editingString != null) {
            gui.entryField.insert(event.codepointAsString())
            gui.entryField.clampScroll(gui.entryFieldVisibleW)
            return true
        }
        if (gui.editingProfileName != null) {
            gui.entryField.insert(event.codepointAsString())
            gui.entryField.clampScroll(gui.entryFieldVisibleW)
            return true
        }
        if (gui.editingItemListSearch) {
            val ch = event.codepointAsString()
            if (ch.matches(Regex("[^</*?\"\\>:|]+"))) {
                gui.itemListSearch.insert(ch)
                gui.itemListScroll = 0
                gui.itemListSearch.clampScroll(200)
            }
            return true
        }
        return false
    }
}
