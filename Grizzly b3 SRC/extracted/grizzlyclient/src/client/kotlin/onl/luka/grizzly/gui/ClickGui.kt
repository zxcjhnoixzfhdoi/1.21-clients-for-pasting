package onl.luka.grizzly.gui

import onl.luka.grizzly.config.ConfigManager
import onl.luka.grizzly.module.modules.other.AutoConfig
import onl.luka.grizzly.config.entry.*
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.ModuleManager
import onl.luka.grizzly.module.ModuleProfile
import onl.luka.grizzly.module.modules.other.ClickGui
import onl.luka.grizzly.module.modules.other.Colour
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.util.*
import onl.luka.grizzly.util.Text
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import com.mojang.blaze3d.platform.InputConstants
import onl.luka.grizzly.gui.helpers.ITEM_LIST_DROPDOWN_MAX_H
import onl.luka.grizzly.gui.helpers.InputHelper
import onl.luka.grizzly.gui.helpers.commitEditingColor
import onl.luka.grizzly.gui.helpers.filteredItemListRowsFor
import onl.luka.grizzly.gui.helpers.findItemByName
import onl.luka.grizzly.gui.helpers.getAllItems
import onl.luka.grizzly.gui.helpers.hsvToRgb
import onl.luka.grizzly.gui.helpers.itemCategories
import onl.luka.grizzly.gui.helpers.pickerRowIconName
import onl.luka.grizzly.gui.helpers.pickerRowId
import onl.luka.grizzly.gui.helpers.pickerRowLabel
import onl.luka.grizzly.gui.helpers.rgbToHsv
import onl.luka.grizzly.gui.ui.ClickGuiUiFactory
import onl.luka.grizzly.gui.ui.ItemPickerRow
import onl.luka.grizzly.gui.ui.ModulePickerRow
import onl.luka.grizzly.gui.ui.ModulePickerState
import onl.luka.grizzly.gui.ui.ItemPickerState
import onl.luka.grizzly.gui.ui.MinecraftUiRenderer
import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.gui.ui.UiRect
import onl.luka.grizzly.gui.ui.UiRuntime
import onl.luka.grizzly.gui.ui.ClickGuiUiFactory.Companion.sliderT
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class ClickGui : Screen(Component.literal("Medved")) {
    private val uiController = ClickGuiUiController(this)

        val collapsed = mutableSetOf<Module.Category>()
        val positions = mutableMapOf<Module.Category, Pair<Int, Int>>()
        val expandedModules = mutableSetOf<Module>()
        var expandedColorEntry: ColorEntry? = null
        var expandedEnum: EnumEntry<*>? = null
        val renderOrder = mutableListOf<Module.Category?>()
        var presetNameBuffer = "default"
        var cfgPanelX = -1
        var cfgPanelY = -1
        var cfgPanelCollapsed = true
        internal var defaultPositions = mapOf<Module.Category, Pair<Int, Int>>()
        internal var defaultCfgPanelY = -1
        var dropdownScroll = 0

        internal val PNL_W = 176  // panel width
        internal val HDR_H = 20   // header bar height
        private val PNL_GAP = 8
        private val PNL_MARGIN = 10
        private val PNL_TOP = 30

        fun layoutPanels(viewportWidth: Int) {
            collapsed.addAll(Module.Category.entries)
            var x = PNL_MARGIN
            var y = PNL_TOP
            for (cat in Module.Category.entries) {
                if (x + PNL_W > viewportWidth - PNL_MARGIN) { x = PNL_MARGIN; y += HDR_H + PNL_GAP }
                positions[cat] = Pair(x, y)
                x += PNL_W + PNL_GAP
            }
            if (x + PNL_W > viewportWidth - PNL_MARGIN) { x = PNL_MARGIN; y += HDR_H + PNL_GAP }
            cfgPanelX = x
            cfgPanelY = y
            cfgPanelCollapsed = true
        }

        fun resetPos() {
            positions.clear()
            renderOrder.clear()
            renderOrder.add(null)
            renderOrder.addAll(Module.Category.entries)

            layoutPanels(width)

            defaultPositions = positions.toMap()
            defaultCfgPanelY = cfgPanelY
            dropdownScroll = 0
            NotificationManager.show("Layout Reset")
        }


    internal var selectedCategory: Module.Category? = null

    internal var editingString: StringEntry? = null
    internal var editingProfileName: ModuleProfile? = null
    internal val entryField = TextField()
    internal var draggingStringEntry = false
    internal var entryFieldTextX = 0
    internal var entryFieldVisibleW = 60
    internal var listeningKeybind: KeybindEntry? = null
    internal var itemListSearch = TextField()
    internal var draggingCat: Module.Category? = null
    internal var dragOffX = 0
    internal var dragOffY = 0
    internal var hoveredMod: Module? = null
    internal var draggingSlider: SliderDrag? = null
    internal var draggingXmlSlider: XmlSliderDrag? = null
    internal var editingColorEntry: ColorEntry? = null
    internal var editingColorChannel: Int? = null
    internal var editingColorHex = false
    internal var colorPickerMode = ColorPickerMode.CUSTOM
    internal var colorPickerModeExpanded = false
    internal var colorPickerCustomValue: Color? = null
    internal var presetFieldActive = false
    internal val presetField = TextField()
    internal var draggingCfgPanel = false
    internal var cfgDragOffX = 0
    internal var cfgDragOffY = 0
    internal var draggingPresetField = false
    internal var presetFieldTextX = 0
    internal var presetFieldVisibleW = 150

    internal enum class MetaField { DESCRIPTION, ANTICHEAT, SERVER, MAPPING_SERVER }

    internal var metaPreset: String? = null
    internal var metaDraft: onl.luka.grizzly.config.PresetMetadata? = null
    internal var metaFieldActive: MetaField? = null
    internal var metaMappingIndex = -1
    internal val metaField = TextField()
    internal var draggingMetaField = false
    internal var metaFieldTextX = 0
    internal var metaFieldVisibleW = 120

    internal fun openMetadata(preset: String) {
        commitMetaField()
        if (metaPreset == preset) {
            saveMetadata()
            metaPreset = null
            metaDraft = null
            return
        }
        saveMetadata()
        metaPreset = preset
        metaDraft = ConfigManager.metadataFor(preset)
    }

    internal fun metaFieldName(): String = when (metaFieldActive) {
        MetaField.DESCRIPTION -> "description"
        MetaField.ANTICHEAT -> "anticheat"
        MetaField.SERVER -> "server"
        MetaField.MAPPING_SERVER -> "mapping:$metaMappingIndex"
        null -> ""
    }

    internal fun beginMetaEditAt(
        field: MetaField,
        initial: String,
        event: onl.luka.grizzly.gui.ui.UiPointerEvent,
        mappingIndex: Int = -1,
    ) {
        beginMetaEdit(field, initial, mappingIndex)
        draggingMetaField = true
        val textX = event.node.bounds.x.toInt() + event.node.style.padding.left.toInt()
        val visibleW = (event.node.bounds.width - event.node.style.padding.left - event.node.style.padding.right)
            .toInt()
            .coerceAtLeast(1)
        metaFieldTextX = textX
        metaFieldVisibleW = visibleW
        metaField.cursor = metaField.posFromPixel(event.x.toInt() - textX)
        metaField.selAnchor = metaField.cursor
        metaField.clampScroll(visibleW)
    }

    internal fun beginMetaEdit(field: MetaField, initial: String, mappingIndex: Int = -1) {
        commitMetaField()
        metaFieldActive = field
        metaMappingIndex = mappingIndex
        metaField.set(initial)
        metaField.cursor = metaField.text.length
        metaField.selAnchor = -1
    }

    /** Writes whatever is in the shared field back to whichever value opened it. */
    internal fun commitMetaField() {
        val field = metaFieldActive ?: return
        val text = metaField.text.trim()
        metaFieldActive = null

        when (field) {
            MetaField.DESCRIPTION -> metaDraft?.description = metaField.text
            MetaField.ANTICHEAT -> metaDraft?.anticheat = metaField.text
            MetaField.SERVER -> {
                val draft = metaDraft
                if (draft != null && text.isNotEmpty() && draft.servers.none { it.equals(text, true) }) {
                    draft.servers += text
                }
            }
            MetaField.MAPPING_SERVER -> {
                val index = metaMappingIndex
                val entry = AutoConfig.mappings
                if (index in entry.value.indices && text.isNotEmpty()) {
                    val existing = entry.value[index]
                    entry.removeAt(index)
                    entry.add(text, existing.preset)
                }
            }
        }
        metaMappingIndex = -1
        metaField.set("")
        saveMetadata()
    }

    internal fun saveMetadata() {
        val preset = metaPreset ?: return
        val draft = metaDraft ?: return
        ConfigManager.updateMetadata(preset, draft)
    }
    internal val cursorVisible get() = (System.currentTimeMillis() / 530) % 2 == 0L

    internal var expandedItemList: onl.luka.grizzly.config.entry.ItemListEntry? = null
    internal var expandedModuleList: ModuleListEntry? = null
    internal var editingItemListSearch = false
    internal var itemListScroll = 0
    internal var itemListAddedScroll = 0
    internal var itemListCategory: String? = null

    enum class ColorPickerMode { CUSTOM, THEME, CHROMA }

    internal sealed interface SliderDrag {
        val barX: Int
        val barW: Int
        data class ColorMap(val entry: ColorEntry, val mapX: Int, val mapY: Int, val mapW: Int, val mapH: Int) : SliderDrag {
            override val barX: Int get() = mapX
            override val barW: Int get() = mapW
        }
        data class ColorHue(val entry: ColorEntry, override val barX: Int, override val barW: Int, val barY: Int, val barH: Int, val horizontal: Boolean = false) : SliderDrag
        data class ColorAlpha(val entry: ColorEntry, override val barX: Int, override val barW: Int, val barY: Int, val barH: Int, val horizontal: Boolean = false) : SliderDrag
        data class ChromaSpeed(val entry: ColorEntry, override val barX: Int, override val barW: Int, val barY: Int, val barH: Int) : SliderDrag
    }

    internal sealed interface XmlSliderDrag {
        val bounds: UiRect
        data class IntValue(val entry: IntEntry, override val bounds: UiRect) : XmlSliderDrag
        data class FloatValue(val entry: FloatEntry, override val bounds: UiRect) : XmlSliderDrag
        data class DoubleValue(val entry: DoubleEntry, override val bounds: UiRect) : XmlSliderDrag
        data class IntRangeValue(val entry: IntRangeEntry, val high: Boolean, override val bounds: UiRect) : XmlSliderDrag
        data class FloatRangeValue(val entry: FloatRangeEntry, val high: Boolean, override val bounds: UiRect) : XmlSliderDrag
    }

    internal fun argb(a: Int, r: Int, g: Int, b: Int) =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    internal val ACCENT   get() = Colour.accent.liveColor(Colour.accent.value).argb
    internal val TEXT     = argb(255, 215, 215, 228)
    internal val guiFont  get() = Font.getFont()
    internal fun styled(text: String) = Font.styledText(text)

    override fun init() {
        super.init()
        // close container
        minecraft.player?.let { player ->
            if (player.containerMenu !== player.inventoryMenu) {
                player.closeContainer()
            }
        }
        // re-sync movement KeyMappings from the physical GLFW state
        val window = minecraft.window.handle()
        for (km in listOf(
            minecraft.options.keyUp, minecraft.options.keyDown,
            minecraft.options.keyLeft, minecraft.options.keyRight,
            minecraft.options.keyJump, minecraft.options.keySprint,
            minecraft.options.keyShift
        )) {
            val bound = InputConstants.getKey(km.saveString())
            if (bound.type == InputConstants.Type.KEYSYM) {
                val held = GLFW.glfwGetKey(window, bound.value) == GLFW.GLFW_PRESS
                KeyMapping.set(bound, held)
            }
        }
        if (positions.isEmpty()) layoutPanels(width)
        if (cfgPanelX < 0) { cfgPanelX = 10; cfgPanelY = 30; cfgPanelCollapsed = true }
        if (renderOrder.isEmpty()) {
            renderOrder.addAll(Module.Category.entries)
            renderOrder.add(null)
        }
        if (null !in renderOrder) renderOrder.add(null)
        dropdownScroll = 0

        presetField.text = presetNameBuffer
        presetField.end(false)
    }

    override fun isPauseScreen() = false
    override fun isInGameUi() = true

    override fun extractBackground(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        uiController.renderMain(g, mx, my)

        val hov = hoveredMod
        if (hov != null && ClickGui.showDescriptions.value && hov.description.isNotBlank()) {
            drawTooltip(g, hov.description, mx, my)
        }
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        ConfigManager.refreshDynamicColors()
    }

    internal inner class TextField {
        var text      = ""
        var cursor    = 0
        var selAnchor = -1
        var scrollPx  = 0

        val selMin get() = if (selAnchor < 0) cursor.coerceIn(0, text.length) else minOf(cursor, selAnchor).coerceIn(0, text.length)
        val selMax get() = if (selAnchor < 0) cursor.coerceIn(0, text.length) else maxOf(cursor, selAnchor).coerceIn(0, text.length)
        val hasSelection get() = selAnchor >= 0 && selAnchor != cursor

        fun set(s: String) { text = s; cursor = s.length; selAnchor = -1; scrollPx = 0 }
        fun insert(s: String) {
            clampCursor()
            if (hasSelection) { text = text.removeRange(selMin, selMax); cursor = selMin; selAnchor = -1 }
            text = text.substring(0, cursor) + s + text.substring(cursor)
            cursor += s.length
        }
        fun backspace() {
            clampCursor()
            if (hasSelection) { text = text.removeRange(selMin, selMax); cursor = selMin; selAnchor = -1 }
            else if (cursor > 0) { text = text.removeRange(cursor - 1, cursor); cursor-- }
        }
        fun deleteForward() {
            clampCursor()
            if (hasSelection) { text = text.removeRange(selMin, selMax); cursor = selMin; selAnchor = -1 }
            else if (cursor < text.length) { text = text.removeRange(cursor, cursor + 1) }
        }
        fun backspaceWord() {
            clampCursor()
            if (hasSelection) { backspace(); return }
            var start = cursor
            while (start > 0 && text[start - 1] == ' ') start--
            while (start > 0 && text[start - 1] != ' ') start--
            text = text.removeRange(start, cursor); cursor = start; selAnchor = -1
        }
        fun move(delta: Int, selecting: Boolean) {
            if (!selecting && hasSelection) { cursor = if (delta < 0) selMin else selMax; selAnchor = -1; return }
            if (selecting && selAnchor < 0) selAnchor = cursor else if (!selecting) selAnchor = -1
            cursor = (cursor + delta).coerceIn(0, text.length)
        }
        fun wordMove(forward: Boolean, selecting: Boolean) {
            var i = cursor
            if (forward) { while (i < text.length && text[i] == ' ') i++; while (i < text.length && text[i] != ' ') i++ }
            else         { while (i > 0 && text[i - 1] == ' ') i--;  while (i > 0 && text[i - 1] != ' ') i-- }
            if (selecting && selAnchor < 0) selAnchor = cursor else if (!selecting) selAnchor = -1
            cursor = i
        }
        fun home(selecting: Boolean) {
            if (selecting && selAnchor < 0) selAnchor = cursor else if (!selecting) selAnchor = -1
            cursor = 0
        }
        fun end(selecting: Boolean) {
            if (selecting && selAnchor < 0) selAnchor = cursor else if (!selecting) selAnchor = -1
            cursor = text.length
        }
        fun selectAll() { selAnchor = 0; cursor = text.length }
        fun copy() = if (hasSelection) text.substring(selMin, selMax) else ""
        fun cut()  = copy().also { if (hasSelection) { text = text.removeRange(selMin, selMax); cursor = selMin; selAnchor = -1 } }
        fun posFromPixel(relPx: Int): Int {
            clampCursor()
            val adjusted = relPx + scrollPx
            if (adjusted <= 0 || text.isEmpty()) return 0
            for (i in 1..text.length) {
                val half = (guiFont.width(styled(text.substring(0, i - 1))) + guiFont.width(styled(text.substring(0, i)))) / 2
                if (adjusted <= half) return i - 1
            }
            return text.length
        }
        fun clampScroll(visWidth: Int) {
            clampCursor()
            val curPx = guiFont.width(styled(text.substring(0, cursor)))
            if (curPx - scrollPx < 0)        scrollPx = curPx
            if (curPx - scrollPx > visWidth) scrollPx = curPx - visWidth
            scrollPx = scrollPx.coerceAtLeast(0)
        }
        private fun clampCursor() {
            cursor = cursor.coerceIn(0, text.length)
            if (selAnchor >= 0) selAnchor = selAnchor.coerceIn(0, text.length)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, inBounds: Boolean): Boolean {
        val mx = event.x().toInt(); val my = event.y().toInt(); val btn = event.button()

        listeningKeybind?.let { entry ->
            entry.value = KeybindEntry.mouseButtonCode(btn)
            entry.suppressNextPress()
            listeningKeybind = null
            return true
        }

        if (editingString != null) { editingString!!.value = entryField.text; editingString = null }
        if (editingProfileName != null) commitProfileName()
        if (editingColorEntry != null) { commitEditingColor(); editingColorEntry = null; editingColorChannel = null; editingColorHex = false }
        if (metaFieldActive != null) commitMetaField()
        presetFieldActive = false

        if (uiController.handleMainClick(mx, my, btn)) return true
        return super.mouseClicked(event, inBounds)
    }







    private fun applyColorMapClick(entry: ColorEntry, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        val bounds = event.node.bounds
        val saturation = ((event.x - bounds.x) / (bounds.width - 1f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val value = 1f - ((event.y - bounds.y) / (bounds.height - 1f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        if (entry.pickerMode == ColorEntry.PickerMode.CHROMA) {
            entry.chromaSaturation = saturation
            entry.chromaBrightness = value
        } else {
            val (hue, _, _) = rgbToHsv(entry.customValue)
            entry.customValue = hsvToRgb(hue, saturation, value).copy(a = entry.customValue.a)
            colorPickerCustomValue = entry.customValue
        }
        applyDynamicColorState(entry)
    }

    private fun applyColorHueClick(entry: ColorEntry, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        val bounds = event.node.bounds
        val hue = if (isHorizontalBar(event)) {
            ((event.x - bounds.x) / (bounds.width - 1f).coerceAtLeast(1f)).coerceIn(0f, 1f) * 360f
        } else {
            ((event.y - bounds.y) / (bounds.height - 1f).coerceAtLeast(1f)).coerceIn(0f, 1f) * 360f
        }
        val (_, saturation, value) = rgbToHsv(entry.customValue)
        entry.customValue = hsvToRgb(hue, saturation, value).copy(a = entry.customValue.a)
        colorPickerCustomValue = entry.customValue
        applyDynamicColorState(entry)
    }

    private fun applyColorAlphaClick(entry: ColorEntry, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        val bounds = event.node.bounds
        val t = if (isHorizontalBar(event)) {
            ((event.x - bounds.x) / (bounds.width - 1f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        } else {
            1f - ((event.y - bounds.y) / (bounds.height - 1f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        }
        val alpha = (255f * t).roundToInt().coerceIn(0, 255)
        entry.customValue = entry.customValue.copy(a = alpha)
        colorPickerCustomValue = entry.customValue
        applyDynamicColorState(entry)
    }

    // Entry points for the inline picker. The old overlay bound these on its own document;
    // now the main document does, so they have to be reachable from the controller.
    internal fun pickColorMode(entry: ColorEntry, mode: ColorEntry.PickerMode) {
        if (mode == ColorEntry.PickerMode.THEME && !supportsThemeMode(entry)) return
        entry.pickerMode = mode
        colorPickerMode = when (mode) {
            ColorEntry.PickerMode.CUSTOM -> ColorPickerMode.CUSTOM
            ColorEntry.PickerMode.THEME -> ColorPickerMode.THEME
            ColorEntry.PickerMode.CHROMA -> ColorPickerMode.CHROMA
        }
        colorPickerModeExpanded = false
        applyDynamicColorState(entry)
    }

    internal fun beginColorMapDrag(entry: ColorEntry, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        applyColorMapClick(entry, event)
        draggingSlider = SliderDrag.ColorMap(
            entry,
            event.node.bounds.x.toInt(),
            event.node.bounds.y.toInt(),
            event.node.bounds.width.toInt(),
            event.node.bounds.height.toInt(),
        )
    }

    internal fun beginColorHueDrag(entry: ColorEntry, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        if (entry.pickerMode == ColorEntry.PickerMode.CHROMA) return
        applyColorHueClick(entry, event)
        draggingSlider = SliderDrag.ColorHue(
            entry,
            event.node.bounds.x.toInt(),
            event.node.bounds.width.toInt(),
            event.node.bounds.y.toInt(),
            event.node.bounds.height.toInt(),
            isHorizontalBar(event),
        )
    }

    internal fun beginColorAlphaDrag(entry: ColorEntry, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        applyColorAlphaClick(entry, event)
        draggingSlider = SliderDrag.ColorAlpha(
            entry,
            event.node.bounds.x.toInt(),
            event.node.bounds.width.toInt(),
            event.node.bounds.y.toInt(),
            event.node.bounds.height.toInt(),
            isHorizontalBar(event),
        )
    }

    internal fun beginColorSpeedDrag(entry: ColorEntry, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        applyColorSpeedClick(entry, event)
        draggingSlider = SliderDrag.ChromaSpeed(
            entry,
            event.node.bounds.x.toInt(),
            event.node.bounds.width.toInt(),
            event.node.bounds.y.toInt(),
            event.node.bounds.height.toInt(),
        )
    }

    internal fun beginColorChannelEdit(entry: ColorEntry, channel: Int) {
        editingColorEntry = entry
        editingColorChannel = channel
        editingColorHex = false
        entryField.set(
            listOf(entry.customValue.r, entry.customValue.g, entry.customValue.b, entry.customValue.a)[channel]
                .toString()
        )
        entryField.cursor = entryField.text.length
        entryField.selAnchor = -1
    }

    internal fun beginColorHexEdit(entry: ColorEntry) {
        editingColorEntry = entry
        editingColorHex = true
        editingColorChannel = null
        entryField.set("#%02X%02X%02X%02X".format(entry.value.a, entry.value.r, entry.value.g, entry.value.b))
        entryField.cursor = entryField.text.length
        entryField.selAnchor = -1
    }

    private fun isHorizontalBar(event: onl.luka.grizzly.gui.ui.UiPointerEvent): Boolean =
        event.node.attributes["horizontal"]?.equals("true", ignoreCase = true) == true

    private fun applyColorSpeedClick(entry: ColorEntry, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        val bounds = event.node.bounds
        val t = ((event.x - bounds.x) / bounds.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
        entry.chromaSpeed = 0.05f + t * (8f - 0.05f)
        applyDynamicColorState(entry)
    }






    /** Prepares everything the inline picker needs to draw, ready for the blueprint. */
    internal fun itemPickerState(entry: ItemListEntry): ItemPickerState {
        val addedSet = entry.value.map { it.lowercase() }.toHashSet()
        val all = filteredItemListRowsFor(itemListSearch.text, entry.filter)
        val listHeight = xmlItemListListHeight(entry)

        // Only the window gets turned into nodes. The spacers either side keep the scroll range
        // honest, so the list behaves as if all of it were there.
        val onScreen = listHeight / ROW_H
        itemListScroll = itemListScroll.coerceIn(0, (all.size - onScreen).coerceAtLeast(0))
        val first = itemListScroll
        val window = all.drop(first).take(onScreen + ROW_OVERSCAN)

        val rows = window.map { row ->
            val id = pickerRowId(row)
            ItemPickerRow(
                id = id,
                label = pickerRowLabel(row),
                icon = pickerRowIconName(row),
                added = id.lowercase() in addedSet,
            )
        }
        return ItemPickerState(
            header = "${entry.value.size} items (${entry.mode.name.lowercase().replaceFirstChar { it.uppercase() }})",
            search = itemListSearch.text,
            searchActive = editingItemListSearch,
            searchCursor = itemListSearch.cursor,
            searchScroll = itemListSearch.scrollPx,
            rows = rows,
            added = xmlAddedItemIcons(entry),
            rowScroll = first * ROW_H,
            addedScroll = itemListAddedScroll,
            listHeight = listHeight,
            leadSpace = first * ROW_H,
            tailSpace = ((all.size - first - window.size) * ROW_H).coerceAtLeast(0),
        )
    }

    /** Capped so a long item list grows the pane a sane amount and then scrolls inside itself. */
    private fun xmlItemListListHeight(entry: ItemListEntry): Int {
        val rows = filteredItemListRowsFor(itemListSearch.text, entry.filter).size.coerceAtLeast(1)
        val top = PICKER_CHROME_H + (if (entry.value.isNotEmpty()) ADDED_STRIP_H else 0)
        return ((ITEM_LIST_DROPDOWN_MAX_H - top) / 18).coerceAtLeast(1).coerceAtMost(rows) * 18
    }

    internal fun modulePickerState(entry: ModuleListEntry): ModulePickerState {
        val all = filteredModuleListRows(itemListSearch.text)
        val listHeight = xmlModuleListListHeight()
        val onScreen = (listHeight / ROW_H).coerceAtLeast(1)
        itemListScroll = itemListScroll.coerceIn(0, (all.size - onScreen).coerceAtLeast(0))
        val first = itemListScroll
        val window = all.drop(first).take(onScreen + ROW_OVERSCAN)

        return ModulePickerState(
            header = "${entry.value.size} hidden modules",
            search = itemListSearch.text,
            searchActive = editingItemListSearch,
            searchCursor = itemListSearch.cursor,
            searchScroll = itemListSearch.scrollPx,
            rows = window.map { ModulePickerRow(it.name, entry.contains(it.name)) },
            rowScroll = first * ROW_H,
            listHeight = listHeight,
            leadSpace = first * ROW_H,
            tailSpace = ((all.size - first - window.size) * ROW_H).coerceAtLeast(0),
        )
    }

    private fun xmlModuleListListHeight(): Int {
        val rows = filteredModuleListRows(itemListSearch.text).size.coerceAtLeast(1)
        return ((ITEM_LIST_DROPDOWN_MAX_H - MODULE_LIST_CHROME_H) / 18)
            .coerceAtLeast(1)
            .coerceAtMost(rows) * 18
    }

    internal fun toggleModuleListValue(entry: ModuleListEntry, name: String) {
        if (entry.contains(name)) entry.remove(name) else entry.add(name)
    }

    internal fun beginItemSearchEdit(event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        itemListSearch.cursor = itemListSearch.posFromPixel((event.x - event.node.bounds.x - 3f).roundToInt())
        editingItemListSearch = true
    }

    internal fun toggleItemListValue(entry: ItemListEntry, id: String) {
        if (entry.value.any { it.equals(id, ignoreCase = true) }) entry.remove(id) else entry.add(id)
    }

    private fun xmlAddedItemIcons(entry: ItemListEntry): List<Pair<String, String>> =
        entry.value.mapNotNull { id ->
            val icon = if (id.endsWith("_category")) {
                val categoryId = id.removeSuffix("_category")
                val category = itemCategories.firstOrNull { it.id == categoryId }
                getAllItems().firstOrNull { category?.matches(it.first) == true }?.first
            } else {
                findItemByName(id)?.first
            }
            icon?.let { id to it }
        }

    private fun filteredModuleListRows(query: String): List<Module> {
        val normalized = query.trim().lowercase()
        return ModuleManager.getAll()
            .asSequence()
            .filter { it.showInModulesList }
            .filter { it.name != "Modules List" }
            .filter {
                normalized.isBlank() ||
                    it.name.lowercase().contains(normalized) ||
                    it.category.name.lowercase().replace('_', ' ').contains(normalized)
            }
            .sortedWith(compareBy<Module>({ it.category.ordinal }, { it.name.lowercase() }))
            .toList()
    }






    private fun noopUiRenderer(): onl.luka.grizzly.gui.ui.UiRenderer =
        object : onl.luka.grizzly.gui.ui.UiRenderer {
            override val fontHeight: Float get() = guiFont.lineHeight.toFloat()
            override fun textWidth(text: String): Float = guiFont.width(styled(text)).toFloat()
            override fun fill(rect: UiRect, color: Int, radius: Float) {}
            override fun border(rect: UiRect, color: Int, width: Float, radius: Float) {}
            override fun text(text: String, x: Float, y: Float, color: Int) {}
            override fun clip(rect: UiRect) {}
            override fun unclip() {}
        }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mx = mouseX.toInt(); val my = mouseY.toInt()
        if (uiController.handleMainScroll(mouseX.toFloat(), mouseY.toFloat(), scrollY.toFloat())) return true
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (InputHelper.handleKeyPressed(this, event)) return true

        val key = event.key()

        if (key == GLFW.GLFW_KEY_ESCAPE || key == ClickGui.keybind.value) { onClose(); return true }

        val inputKey = InputConstants.getKey(event)
        KeyMapping.set(inputKey, true)
        KeyMapping.click(inputKey)
        return false
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        return InputHelper.handleKeyReleased(this, event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        return InputHelper.handleCharTyped(this, event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val xmlSlider = draggingXmlSlider
        if (xmlSlider != null) {
            val t = sliderT(xmlSlider.bounds, event.x().toFloat())
            when (xmlSlider) {
                is XmlSliderDrag.IntValue ->
                    xmlSlider.entry.value = (xmlSlider.entry.min + t * (xmlSlider.entry.max - xmlSlider.entry.min)).roundToInt()
                is XmlSliderDrag.FloatValue ->
                    xmlSlider.entry.value = xmlSlider.entry.min + t * (xmlSlider.entry.max - xmlSlider.entry.min)
                is XmlSliderDrag.DoubleValue ->
                    xmlSlider.entry.value = xmlSlider.entry.min + t.toDouble() * (xmlSlider.entry.max - xmlSlider.entry.min)
                is XmlSliderDrag.IntRangeValue -> {
                    val value = (xmlSlider.entry.min + t * (xmlSlider.entry.max - xmlSlider.entry.min)).roundToInt()
                    val (lo, hi) = xmlSlider.entry.value
                    xmlSlider.entry.value =
                        if (xmlSlider.high) lo to value.coerceAtLeast(lo)
                        else value.coerceAtMost(hi) to hi
                }
                is XmlSliderDrag.FloatRangeValue -> {
                    val scale = Math.pow(10.0, xmlSlider.entry.decimals.toDouble()).toFloat()
                    val value = (Math.round((xmlSlider.entry.min + t * (xmlSlider.entry.max - xmlSlider.entry.min)) * scale).toFloat()) / scale
                    val (lo, hi) = xmlSlider.entry.value
                    xmlSlider.entry.value =
                        if (xmlSlider.high) lo to value.coerceAtLeast(lo)
                        else value.coerceAtMost(hi) to hi
                }
            }
            return true
        }

        val slider = draggingSlider
        if (slider != null) {
            when (slider) {
                is SliderDrag.ColorAlpha -> {
                    val t = if (slider.horizontal) {
                        (event.x().toInt() - slider.barX).coerceIn(0, slider.barW - 1)
                            .toFloat() / (slider.barW - 1).coerceAtLeast(1)
                    } else {
                        1f - (event.y().toInt() - slider.barY).coerceIn(0, slider.barH - 1)
                            .toFloat() / (slider.barH - 1).coerceAtLeast(1)
                    }
                    val alpha = (255f * t).roundToInt().coerceIn(0, 255)
                    slider.entry.customValue = slider.entry.customValue.copy(a = alpha)
                    applyDynamicColorState(slider.entry)
                }
                is SliderDrag.ColorMap -> {
                    val px = (event.x().toInt() - slider.mapX).coerceIn(0, slider.mapW - 1)
                    val py = (event.y().toInt() - slider.mapY).coerceIn(0, slider.mapH - 1)
                    val saturation = px.toFloat() / (slider.mapW - 1).coerceAtLeast(1)
                    val value = 1f - py.toFloat() / (slider.mapH - 1).coerceAtLeast(1)
                    if (slider.entry.pickerMode == ColorEntry.PickerMode.CHROMA) {
                        slider.entry.chromaSaturation = saturation
                        slider.entry.chromaBrightness = value
                    } else {
                        val current = slider.entry.customValue
                        val (hue, _, _) = rgbToHsv(current)
                        slider.entry.customValue = hsvToRgb(hue, saturation, value).copy(a = current.a)
                    }
                    applyDynamicColorState(slider.entry)
                }
                is SliderDrag.ColorHue -> {
                    val tY = if (slider.horizontal) {
                        (event.x().toInt() - slider.barX).coerceIn(0, slider.barW - 1)
                            .toFloat() / (slider.barW - 1).coerceAtLeast(1)
                    } else {
                        (event.y().toInt() - slider.barY).coerceIn(0, slider.barH - 1)
                            .toFloat() / (slider.barH - 1).coerceAtLeast(1)
                    }
                    if (slider.entry.pickerMode == ColorEntry.PickerMode.CHROMA) {
                        slider.entry.chromaSpeed = 0.05f + tY * (8f - 0.05f)
                    } else {
                        val newHue = tY * 360f
                        val current = slider.entry.customValue
                        val (_, saturation, value) = rgbToHsv(current)
                        slider.entry.customValue = hsvToRgb(newHue, saturation, value).copy(a = current.a)
                    }
                    applyDynamicColorState(slider.entry)
                }
                is SliderDrag.ChromaSpeed -> {
                    val px = (event.x().toInt() - slider.barX).coerceIn(0, slider.barW - 1)
                    val tX = px.toFloat() / (slider.barW - 1).coerceAtLeast(1)
                    slider.entry.chromaSpeed = 0.05f + tX * (8f - 0.05f)
                    applyDynamicColorState(slider.entry)
                }
            }
            return true
        }
        if (draggingPresetField) {
            val relX = event.x().toInt() - presetFieldTextX
            presetField.apply { cursor = posFromPixel(relX); clampScroll(presetFieldVisibleW) }
            return true
        }
        if (draggingMetaField && metaFieldActive != null) {
            metaField.apply { cursor = posFromPixel(event.x().toInt() - metaFieldTextX); clampScroll(metaFieldVisibleW) }
            return true
        }
        if (draggingStringEntry && editingString != null) {
            entryField.apply { cursor = posFromPixel(event.x().toInt() - entryFieldTextX); clampScroll(entryFieldVisibleW) }
            return true
        }
        if (draggingCfgPanel) {
            cfgPanelX = event.x().toInt() - cfgDragOffX
            cfgPanelY = event.y().toInt() - cfgDragOffY + dropdownScroll
            return true
        }
        val cat = draggingCat ?: return super.mouseDragged(event, dragX, dragY)
        positions[cat] = Pair(event.x().toInt() - dragOffX, event.y().toInt() - dragOffY + dropdownScroll)
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggingCat = null
        draggingSlider = null
        draggingXmlSlider = null
        draggingCfgPanel = false
        draggingPresetField = false
        draggingStringEntry = false
        draggingMetaField = false
        constrainXmlDropdownScroll()
        return super.mouseReleased(event)
    }

    override fun onClose() {
        super.onClose()
        commitMetaField()
        saveMetadata()
        presetNameBuffer = presetField.text
        ClickGui.disable()
    }

    internal fun configEntries(mod: Module) = mod.entries.filter {
        it.name != "enabled" && !(mod.isProtected && it.name == "keybind") &&
                (it.visibleWhen?.invoke() != false)
    }

    /** A blank name would leave an unclickable row, so an empty field keeps the old one. */
    internal fun commitProfileName() {
        editingProfileName?.let { profile ->
            entryField.text.trim().takeIf { it.isNotEmpty() }?.let { profile.name = it }
        }
        editingProfileName = null
    }

    internal fun colorChannelCount(entry: ColorEntry) = if (entry.allowAlpha) 4 else 3

    internal fun supportsThemeMode(entry: ColorEntry) = entry !== Colour.accent

    internal fun constrainXmlDropdownScroll() {
        dropdownScroll = dropdownScroll.coerceIn(0, uiController.dropdownScrollMax().roundToInt().coerceAtLeast(0))
    }

    internal fun applyDynamicColorState(entry: ColorEntry) {
        entry.applyDynamicColor(Colour.accent.liveColor(Colour.accent.value), ColorEntry.chromaTimeSeconds(), supportsThemeMode(entry))
    }

    internal fun toggleExpand(mod: Module) {
        if (mod in expandedModules) expandedModules.remove(mod) else expandedModules.add(mod)
        expandedColorEntry = null
        expandedEnum = null
    }

    internal fun bringToFront(cat: Module.Category) {
        renderOrder.remove(cat)
        renderOrder.add(cat)
    }

    internal fun bringConfigToFront() {
        renderOrder.remove(null)
        renderOrder.add(null)
    }

    internal fun drawTooltip(g: GuiGraphicsExtractor, text: String, mx: Int, my: Int) {
        val pad = 4
        val styledTooltip = styled(text)
        val bw = guiFont.width(styledTooltip) + pad * 2
        val bh = 8 + pad * 2
        var tx = mx + 10
        var ty = my - bh - 4
        if (tx + bw > width - 2) tx = width - bw - 2
        if (ty < 2) ty = my + 10
        g.fill(tx, ty, tx + bw, ty + bh, argb(230, 10, 10, 20))
        g.fill(tx, ty, tx + 1, ty + bh, ACCENT)
        g.Text(guiFont, styledTooltip, tx + pad, ty + pad, TEXT)
    }

    companion object {
        private const val ROW_H = 18
        private const val ROW_OVERSCAN = 2

        // padding + header + gap + search + gap + padding, matching the inline picker templates
        private const val PICKER_CHROME_H = 5 + 9 + 4 + 12 + 4 + 5
        private const val ADDED_STRIP_H = 21 + 4
        private const val MODULE_LIST_CHROME_H = PICKER_CHROME_H

        fun resetPositions() {
            val mc = Minecraft.getInstance()
            if (mc.gui.screen() is onl.luka.grizzly.gui.ClickGui) {
                (mc.gui.screen() as onl.luka.grizzly.gui.ClickGui).resetPos()
            }
        }
    }
}
