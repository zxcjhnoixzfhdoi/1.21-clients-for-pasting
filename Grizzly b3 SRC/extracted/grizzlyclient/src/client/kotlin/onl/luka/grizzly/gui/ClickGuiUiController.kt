package onl.luka.grizzly.gui

import onl.luka.grizzly.config.ConfigManager
import onl.luka.grizzly.util.NotificationManager
import onl.luka.grizzly.module.modules.other.AutoConfig
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.config.entry.DoubleEntry
import onl.luka.grizzly.config.entry.FloatEntry
import onl.luka.grizzly.config.entry.FloatRangeEntry
import onl.luka.grizzly.config.entry.IntEntry
import onl.luka.grizzly.config.entry.IntRangeEntry
import onl.luka.grizzly.config.entry.ModuleListEntry
import onl.luka.grizzly.gui.ui.ClickGuiUiFactory
import onl.luka.grizzly.gui.ui.MinecraftUiRenderer
import onl.luka.grizzly.gui.ui.UiDocument
import onl.luka.grizzly.gui.ui.UiNode
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.ModuleManager
import onl.luka.grizzly.gui.ui.UiRect
import onl.luka.grizzly.gui.ui.UiRenderer
import onl.luka.grizzly.gui.ui.UiRuntime
import onl.luka.grizzly.module.modules.other.ClickGui as ClickGuiModule
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

internal class ClickGuiUiController(
    private val gui: ClickGui,
) {
    fun renderMain(g: GuiGraphicsExtractor, mx: Int, my: Int) {
        if (ClickGuiModule.blurBackground.value && ClickGuiModule.showBackground.value) {
            try {
                g.javaClass.getMethod("blurBeforeThisStratum").invoke(g)
                g.javaClass.getMethod("nextStratum").invoke(g)
            } catch (ignored: Exception) {}
        }

        val runtime = UiRuntime(MinecraftUiRenderer(g))
        val document = buildMainDocument()
        runtime.layout(document, UiRect(0f, 0f, gui.width.toFloat(), gui.height.toFloat()))
        runtime.render(document, mx.toFloat(), my.toFloat())
        gui.hoveredMod = runtime
            .nodeAt(document, mx.toFloat(), my.toFloat()) { moduleForId(it) != null }
            ?.id
            ?.let(::moduleForId)
    }

    /**
     * The description tooltip needs to know which module row the cursor is over. Taken off the
     * laid-out tree rather than tracked while drawing, so it cannot drift from what is on screen.
     */
    private fun moduleForId(id: String): Module? {
        val name = id.removePrefix(MODULE_ID_PREFIX).takeIf { it != id } ?: return null
        return ModuleManager.getAll().firstOrNull { it.name == name }
    }

    fun handleMainClick(mx: Int, my: Int, btn: Int): Boolean {
        val runtime = UiRuntime(noopRenderer())
        val document = buildMainDocument()
        runtime.layout(document, UiRect(0f, 0f, gui.width.toFloat(), gui.height.toFloat()))
        return runtime.mouseClicked(document, mx.toFloat(), my.toFloat(), btn)
    }

    /**
     * The pane's real scroll range, straight off a laid-out document. Estimating it from panel
     * heights went stale the moment settings started expanding inline.
     */
    fun dropdownScrollMax(): Float {
        val runtime = UiRuntime(noopRenderer())
        val document = buildMainDocument()
        runtime.layout(document, UiRect(0f, 0f, gui.width.toFloat(), gui.height.toFloat()))
        return findScroll(document.root, "dropdown")?.scrollMax ?: 0f
    }

    private fun findScroll(node: UiNode, key: String): UiNode? {
        if (node.attributes["scrollKey"] == key) return node
        node.children.forEach { child -> findScroll(child, key)?.let { return it } }
        return null
    }

    fun handleMainScroll(mouseX: Float, mouseY: Float, scrollY: Float): Boolean {
        val runtime = UiRuntime(noopRenderer())
        val document = buildMainDocument()
        runtime.layout(document, UiRect(0f, 0f, gui.width.toFloat(), gui.height.toFloat()))
        return runtime.mouseScrolled(document, mouseX, mouseY, scrollY) { key, value, max ->
            when (key) {
                "dropdown" -> gui.dropdownScroll = value.roundToInt().coerceIn(0, max.roundToInt().coerceAtLeast(0))
                "item-list.rows",
                "module-list.rows" -> gui.itemListScroll = (value / 18f).roundToInt().coerceAtLeast(0)
                "item-list.added" -> gui.itemListAddedScroll = value.roundToInt().coerceAtLeast(0)
            }
        }
    }

    private fun buildMainDocument(): UiDocument {
        val factory = ClickGuiUiFactory(
            onCategoryClicked = { category, event ->
                if (event.button == 1) {
                    gui.bringToFront(category)
                    if (category in gui.collapsed) gui.collapsed.remove(category) else gui.collapsed.add(category)
                    true
                } else {
                    gui.bringToFront(category)
                    gui.draggingCat = category
                    gui.dragOffX = (event.x - event.node.bounds.x).roundToInt()
                    gui.dragOffY = (event.y - event.node.bounds.y).roundToInt()
                    true
                }
            },
            onModuleExpandRequested = { module -> gui.toggleExpand(module) },
            onKeybindRequested = { entry -> gui.listeningKeybind = entry },
            onProfileSelected = { module, index -> module.switchToProfile(index) },
            onProfileAdded = { module ->
                // Land on the new profile so the settings below it are the ones you just made.
                module.addProfile()
                module.switchToProfile(module.profiles.lastIndex)
            },
            onProfileRemoved = { module, index -> module.removeProfile(index) },
            onProfileRenameRequested = { profile, event ->
                gui.editingProfileName = profile
                gui.entryField.set(profile.name)
                val textX = event.node.bounds.x.toInt() + event.node.style.padding.left.toInt()
                val visibleW = (event.node.bounds.width - event.node.style.padding.left - event.node.style.padding.right)
                    .toInt()
                    .coerceAtLeast(1)
                gui.entryFieldTextX = textX
                gui.entryFieldVisibleW = visibleW
                gui.entryField.cursor = gui.entryField.posFromPixel(event.x.toInt() - textX)
                gui.entryField.selAnchor = gui.entryField.cursor
                gui.entryField.clampScroll(visibleW)
            },
            onNumericDragStarted = { entry, bounds ->
                gui.draggingXmlSlider = when (entry) {
                    is IntEntry -> ClickGui.XmlSliderDrag.IntValue(entry, bounds)
                    is FloatEntry -> ClickGui.XmlSliderDrag.FloatValue(entry, bounds)
                    is DoubleEntry -> ClickGui.XmlSliderDrag.DoubleValue(entry, bounds)
                    else -> null
                }
            },
            onRangeDragStarted = { entry, bounds, high ->
                gui.draggingXmlSlider = when (entry) {
                    is IntRangeEntry -> ClickGui.XmlSliderDrag.IntRangeValue(entry, high, bounds)
                    is FloatRangeEntry -> ClickGui.XmlSliderDrag.FloatRangeValue(entry, high, bounds)
                    else -> null
                }
            },
            onStringEditRequested = { entry, event ->
                gui.editingString = entry
                gui.entryField.set(entry.value)
                val textX = event.node.bounds.x.toInt() + event.node.style.padding.left.toInt()
                val visibleW = (event.node.bounds.width - event.node.style.padding.left - event.node.style.padding.right)
                    .toInt()
                    .coerceAtLeast(1)
                gui.entryFieldTextX = textX
                gui.entryFieldVisibleW = visibleW
                gui.entryField.cursor = gui.entryField.posFromPixel(event.x.toInt() - textX)
                gui.entryField.selAnchor = gui.entryField.cursor
                gui.draggingStringEntry = true
                gui.entryField.clampScroll(visibleW)
            },
            onColorEditRequested = { entry, _ ->
                gui.expandedColorEntry = if (gui.expandedColorEntry == entry) null else entry
                gui.colorPickerModeExpanded = false
                if (gui.expandedColorEntry == entry) {
                    gui.colorPickerMode = when (entry.pickerMode) {
                        ColorEntry.PickerMode.CUSTOM -> ClickGui.ColorPickerMode.CUSTOM
                        ColorEntry.PickerMode.THEME -> ClickGui.ColorPickerMode.THEME
                        ColorEntry.PickerMode.CHROMA -> ClickGui.ColorPickerMode.CHROMA
                    }
                    gui.colorPickerCustomValue = entry.customValue
                } else {
                    gui.editingColorEntry = null
                    gui.editingColorChannel = null
                    gui.editingColorHex = false
                }
            },
            openColorEntry = gui.expandedColorEntry,
            openModuleListEntryForBinding = gui.expandedModuleList,
            openModulePicker = gui.expandedModuleList?.let(gui::modulePickerState),
            onModuleSearchClicked = { event -> gui.beginItemSearchEdit(event) },
            onModuleRowToggled = { entry, name -> gui.toggleModuleListValue(entry, name) },
            openItemListEntryForBinding = gui.expandedItemList,
            openItemPicker = gui.expandedItemList?.let(gui::itemPickerState),
            onItemSearchClicked = { event -> gui.beginItemSearchEdit(event) },
            onItemRowToggled = { entry, id -> gui.toggleItemListValue(entry, id) },
            onItemRemoved = { entry, id -> entry.remove(id) },
            onColorModeToggled = { gui.colorPickerModeExpanded = !gui.colorPickerModeExpanded },
            onColorModePicked = { entry, mode -> gui.pickColorMode(entry, mode) },
            onColorMapDrag = { entry, event -> gui.beginColorMapDrag(entry, event) },
            onColorHueDrag = { entry, event -> gui.beginColorHueDrag(entry, event) },
            onColorAlphaDrag = { entry, event -> gui.beginColorAlphaDrag(entry, event) },
            onColorSpeedDrag = { entry, event -> gui.beginColorSpeedDrag(entry, event) },
            onColorChannelEdit = { entry, channel -> gui.beginColorChannelEdit(entry, channel) },
            onColorHexEdit = { entry -> gui.beginColorHexEdit(entry) },
            onItemListRequested = { entry, _ ->
                gui.expandedItemList = if (gui.expandedItemList == entry) null else entry
                if (gui.expandedItemList == entry) {
                    gui.expandedModuleList = null
                    gui.itemListSearch.set("")
                    gui.editingItemListSearch = true
                    gui.itemListScroll = 0
                    gui.itemListAddedScroll = 0
                    gui.itemListCategory = null
                } else {
                    gui.editingItemListSearch = false
                    gui.itemListScroll = 0
                    gui.itemListAddedScroll = 0
                }
            },
            onModuleListRequested = { entry, _ ->
                gui.expandedModuleList = if (gui.expandedModuleList == entry) null else entry
                if (gui.expandedModuleList == entry) {
                    gui.expandedItemList = null
                    gui.itemListSearch.set("")
                    gui.editingItemListSearch = true
                    gui.itemListScroll = 0
                } else {
                    gui.editingItemListSearch = false
                    gui.itemListScroll = 0
                }
            },
            onEnumDropdownRequested = { entry, _ ->
                gui.expandedEnum = if (gui.expandedEnum == entry) null else entry
            },
            onEnumOptionChosen = { gui.expandedEnum = null },
            onHudEditRequested = { module ->
                Minecraft.getInstance().gui.setScreen(HudEditorScreen(module, gui))
            },
            onConfigHeaderClicked = handler@ { event ->
                when (event.button) {
                    0 -> {
                        gui.bringConfigToFront()
                        gui.draggingCfgPanel = true
                        gui.cfgDragOffX = (event.x - event.node.bounds.x).roundToInt()
                        gui.cfgDragOffY = (event.y - event.node.bounds.y).roundToInt()
                        true
                    }
                    1 -> {
                        gui.bringConfigToFront()
                        gui.cfgPanelCollapsed = !gui.cfgPanelCollapsed
                        true
                    }
                    else -> false
                }
            },
            onPresetEditRequested = { event ->
                gui.presetFieldActive = true
                gui.draggingPresetField = true
                val textX = event.node.bounds.x.toInt() + event.node.style.padding.left.toInt()
                val visibleW = (event.node.bounds.width - event.node.style.padding.left - event.node.style.padding.right)
                    .toInt()
                    .coerceAtLeast(1)
                gui.presetFieldTextX = textX
                gui.presetFieldVisibleW = visibleW
                gui.presetField.cursor = gui.presetField.posFromPixel(event.x.toInt() - textX)
                gui.presetField.selAnchor = gui.presetField.cursor
                gui.presetField.clampScroll(visibleW)
            },
            onPresetSelected = { preset ->
                gui.presetField.set(preset)
                gui.presetField.clampScroll(gui.presetFieldVisibleW)
                gui.presetNameBuffer = preset
            },
            presetNameProvider = { gui.presetField.text },
            metadataProvider = { gui.metaDraft },
            saveMetadataProvider = { name ->
                gui.metaDraft?.takeIf { gui.metaPreset == name } ?: ConfigManager.metadataFor(name)
            },
            onMetaAction = { action, event -> handleMetaAction(action, event) },
        )

        return factory.dropdownDocument(
            panelPositions = gui.positions,
            categoryOrder = gui.renderOrder.filterNotNull(),
            expandedModules = gui.expandedModules,
            collapsedCategories = gui.collapsed,
            activeStringEntry = gui.editingString,
            editingProfile = gui.editingProfileName,
            activeStringText = gui.entryField.text,
            activeStringCursor = gui.entryField.cursor,
            activeStringScrollPx = gui.entryField.scrollPx,
            listeningKeybind = gui.listeningKeybind,
            openEnumEntry = gui.expandedEnum,
            openColorEntry = gui.expandedColorEntry,
            itemPicker = gui.expandedItemList?.let(gui::itemPickerState),
            modulePicker = gui.expandedModuleList?.let(gui::modulePickerState),
            colorModeOpen = gui.colorPickerModeExpanded,
            colorEditingChannel = gui.editingColorChannel.takeIf { gui.editingColorEntry != null && !gui.editingColorHex },
            colorEditingHex = gui.editingColorEntry != null && gui.editingColorHex,
            colorEditingText = gui.entryField.text,
            openItemListEntry = gui.expandedItemList,
            openModuleListEntry = gui.expandedModuleList,
            configX = gui.cfgPanelX,
            configY = gui.cfgPanelY,
            configCollapsed = gui.cfgPanelCollapsed,
            presetName = gui.presetField.text,
            presetActive = gui.presetFieldActive,
            presetCursor = gui.presetField.cursor,
            presetScrollPx = gui.presetField.scrollPx,
            presets = ConfigManager.listPresets(),
            metaPreset = gui.metaPreset,
            meta = gui.metaDraft,
            metaActiveField = gui.metaFieldName(),
            metaText = gui.metaField.text,
            metaCursor = gui.metaField.cursor,
            metaScrollPx = gui.metaField.scrollPx,
            scrollY = gui.dropdownScroll.toFloat(),
            includeEntry = { module, entry -> entry in gui.configEntries(module) },
        )
    }

    private fun handleMetaAction(action: String, event: onl.luka.grizzly.gui.ui.UiPointerEvent) {
        val draft = gui.metaDraft
        when {
            action.startsWith("open:") -> gui.openMetadata(action.removePrefix("open:"))

            action == "meta:anticheat" ->
                gui.beginMetaEditAt(ClickGui.MetaField.ANTICHEAT, draft?.anticheat.orEmpty(), event)
            action == "meta:description" ->
                gui.beginMetaEditAt(ClickGui.MetaField.DESCRIPTION, draft?.description.orEmpty(), event)
            action == "meta:server" ->
                gui.beginMetaEditAt(ClickGui.MetaField.SERVER, "", event)

            action == "meta:overwrite" -> draft?.let {
                it.overwriteUnbound = !it.overwriteUnbound
                gui.saveMetadata()
            }
            action == "meta:disable" -> draft?.let {
                it.disableUnaffected = !it.disableUnaffected
                gui.saveMetadata()
            }
            action == "meta:delete" -> gui.metaPreset?.let { preset ->
                ConfigManager.deletePreset(preset)
                gui.metaPreset = null
                gui.metaDraft = null
                NotificationManager.showConfig("Config Deleted", preset)
            }

            action.startsWith("meta:cat:") -> draft?.let {
                val name = action.removePrefix("meta:cat:")
                val category = Module.Category.entries.firstOrNull { entry -> entry.name == name }
                if (category != null) {
                    if (!it.categories.remove(category)) it.categories.add(category)
                    gui.saveMetadata()
                }
            }
            action.startsWith("meta:server-remove:") -> draft?.let {
                val index = action.substringAfterLast(':').toIntOrNull() ?: return@let
                if (index in it.servers.indices) {
                    it.servers.removeAt(index)
                    gui.saveMetadata()
                }
            }

            action == "mapping:add" ->
                AutoConfig.mappings.add("server.address", gui.presetField.text.ifBlank { "default" })
            action.startsWith("mapping:server:") -> {
                val index = action.substringAfterLast(':').toIntOrNull() ?: return
                val current = AutoConfig.mappings.value.getOrNull(index) ?: return
                gui.beginMetaEditAt(ClickGui.MetaField.MAPPING_SERVER, current.server, event, index)
            }
            action.startsWith("mapping:preset:") -> {
                val index = action.substringAfterLast(':').toIntOrNull() ?: return
                cycleMappingPreset(index)
            }
            action.startsWith("mapping:remove:") -> {
                val index = action.substringAfterLast(':').toIntOrNull() ?: return
                AutoConfig.mappings.removeAt(index)
            }
        }
    }

    private fun cycleMappingPreset(index: Int) {
        val presets = ConfigManager.listPresets()
        if (presets.isEmpty()) return
        val mapping = AutoConfig.mappings.value.getOrNull(index) ?: return
        val next = presets[(presets.indexOf(mapping.preset) + 1).mod(presets.size)]
        AutoConfig.mappings.removeAt(index)
        AutoConfig.mappings.add(mapping.server, next)
    }

    private fun noopRenderer(): UiRenderer =
        object : UiRenderer {
            override val fontHeight: Float get() = gui.guiFont.lineHeight.toFloat()
            override fun textWidth(text: String): Float = gui.guiFont.width(gui.styled(text)).toFloat()
            override fun fill(rect: UiRect, color: Int, radius: Float) {}
            override fun border(rect: UiRect, color: Int, width: Float, radius: Float) {}
            override fun text(text: String, x: Float, y: Float, color: Int) {}
            override fun clip(rect: UiRect) {}
            override fun unclip() {}
        }

    private companion object {
        const val MODULE_ID_PREFIX = "module:"
    }
}
