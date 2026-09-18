package onl.luka.grizzly.gui.ui

import onl.luka.grizzly.config.ConfigGroup
import onl.luka.grizzly.config.ConfigManager
import onl.luka.grizzly.config.entry.BooleanEntry
import onl.luka.grizzly.config.entry.ButtonEntry
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.config.entry.Color
import onl.luka.grizzly.config.entry.ConfigEntry
import onl.luka.grizzly.config.entry.DoubleEntry
import onl.luka.grizzly.config.entry.EnumEntry
import onl.luka.grizzly.config.entry.FloatEntry
import onl.luka.grizzly.config.entry.FloatRangeEntry
import onl.luka.grizzly.config.entry.HudEditEntry
import onl.luka.grizzly.config.entry.IntEntry
import onl.luka.grizzly.config.entry.IntRangeEntry
import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.config.entry.KeybindEntry
import onl.luka.grizzly.config.entry.ModuleListEntry
import onl.luka.grizzly.config.entry.StringEntry
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.Module.Category
import onl.luka.grizzly.module.HudModule
import onl.luka.grizzly.module.ModuleManager
import onl.luka.grizzly.module.modules.other.AutoConfig
import onl.luka.grizzly.module.ModuleProfile
import onl.luka.grizzly.module.modules.other.ClickGui as ClickGuiModule
import onl.luka.grizzly.module.modules.other.Colour
import onl.luka.grizzly.util.NotificationManager
import kotlin.math.roundToInt

class ClickGuiUiFactory(
    private val templates: UiTemplateSet = ClickGuiUiResources.templates(),
    private val onCategoryClicked: (Category, UiPointerEvent) -> Boolean = { _, _ -> false },
    private val onModuleExpandRequested: (Module) -> Unit = {},
    private val onKeybindRequested: (KeybindEntry) -> Unit = {},
    private val onProfileSelected: (Module, Int) -> Unit = { _, _ -> },
    private val onProfileRenameRequested: (ModuleProfile, UiPointerEvent) -> Unit = { _, _ -> },
    private val onProfileAdded: (Module) -> Unit = {},
    private val onProfileRemoved: (Module, Int) -> Unit = { _, _ -> },
    private val onNumericDragStarted: (ConfigEntry<*>, UiRect) -> Unit = { _, _ -> },
    private val onRangeDragStarted: (ConfigEntry<*>, UiRect, Boolean) -> Unit = { _, _, _ -> },
    private val onStringEditRequested: (StringEntry, UiPointerEvent) -> Unit = { _, _ -> },
    private val onColorEditRequested: (ColorEntry, UiPointerEvent) -> Unit = { _, _ -> },
    private val onItemListRequested: (ItemListEntry, UiPointerEvent) -> Unit = { _, _ -> },
    private val onModuleListRequested: (ModuleListEntry, UiPointerEvent) -> Unit = { _, _ -> },
    private val onEnumDropdownRequested: (EnumEntry<*>, UiPointerEvent) -> Unit = { _, _ -> },
    private val onEnumOptionChosen: (EnumEntry<*>) -> Unit = {},
    private val openColorEntry: ColorEntry? = null,
    private val openModuleListEntryForBinding: ModuleListEntry? = null,
    private val openModulePicker: ModulePickerState? = null,
    private val onModuleSearchClicked: (UiPointerEvent) -> Unit = {},
    private val onModuleRowToggled: (ModuleListEntry, String) -> Unit = { _, _ -> },
    private val openItemListEntryForBinding: ItemListEntry? = null,
    private val openItemPicker: ItemPickerState? = null,
    private val onItemSearchClicked: (UiPointerEvent) -> Unit = {},
    private val onItemRowToggled: (ItemListEntry, String) -> Unit = { _, _ -> },
    private val onItemRemoved: (ItemListEntry, String) -> Unit = { _, _ -> },
    private val onColorModeToggled: () -> Unit = {},
    private val onColorModePicked: (ColorEntry, ColorEntry.PickerMode) -> Unit = { _, _ -> },
    private val onColorMapDrag: (ColorEntry, UiPointerEvent) -> Unit = { _, _ -> },
    private val onColorHueDrag: (ColorEntry, UiPointerEvent) -> Unit = { _, _ -> },
    private val onColorAlphaDrag: (ColorEntry, UiPointerEvent) -> Unit = { _, _ -> },
    private val onColorSpeedDrag: (ColorEntry, UiPointerEvent) -> Unit = { _, _ -> },
    private val onColorChannelEdit: (ColorEntry, Int) -> Unit = { _, _ -> },
    private val onColorHexEdit: (ColorEntry) -> Unit = {},
    private val onHudEditRequested: (HudModule) -> Unit = {},
    private val onConfigHeaderClicked: (UiPointerEvent) -> Boolean = { false },
    private val onPresetEditRequested: (UiPointerEvent) -> Unit = {},
    private val onPresetSelected: (String) -> Unit = {},
    private val presetNameProvider: () -> String = { "default" },
    private val onMetaAction: (String, UiPointerEvent) -> Unit = { _, _ -> },
    private val metadataProvider: () -> onl.luka.grizzly.config.PresetMetadata? = { null },
    private val saveMetadataProvider: (String) -> onl.luka.grizzly.config.PresetMetadata =
        { ConfigManager.metadataFor(it) },
) {


    private fun themeValues(): Map<String, String> =
        mapOf(
            "theme.bg" to hex(shade(8, 0.05f, 90)),
            // Panels sit on frosted glass, so surfaces above them are translucent white lifts
            // rather than opaque greys. Anything opaque here would kill the blur underneath.
            "theme.panelBg" to hex(shade(16, 0.10f, 168)),
            "theme.glassTint" to hex(shade(12, 0.08f, 64)),
            // One blur pass per panel is not free, so the frost follows the same switch the
            // background blur does rather than running whether or not anyone wants it.
            "theme.panelBlur" to ClickGuiModule.blurBackground.value.toString(),
            "theme.shadow" to "#4A000000",
            "theme.border" to "#1FFFFFFF",
            "theme.borderSoft" to "#12FFFFFF",
            "theme.divider" to "#14FFFFFF",
            "theme.headerBg" to "#00000000",
            "theme.moduleBg" to "#00000000",
            "theme.moduleHoverBg" to "#14FFFFFF",
            "theme.entryBg" to "#00000000",
            "theme.entrySelectedBg" to "#22FFFFFF",
            "theme.entryHoverBg" to "#0FFFFFFF",
            "theme.controlBg" to "#1AFFFFFF",
            "theme.controlActiveBg" to "#2BFFFFFF",
            "theme.sliderBg" to "#1FFFFFFF",
            "theme.sliderFg" to sliderFg(),
            "theme.accentGlow" to accentGlow(),
            "theme.text" to "#FFE6E6F0",
            "theme.textDim" to "#FF8A8AA0",
            "theme.accent" to accentHex(),
            "theme.transparent" to "#00000000",
        )

    private fun shade(base: Int, mix: Float, alpha: Int = 255): Int {
        val c = Colour.bg.liveColor(Colour.bg.value)
        val r = (base + (c.r - base) * mix).toInt().coerceIn(0, 255)
        val g = (base + (c.g - base) * mix).toInt().coerceIn(0, 255)
        val b = (base + (c.b - base) * mix).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun hex(argb: Int): String =
        "#%08X".format(argb)

    private fun accentHex(): String =
        "#%08X".format(Colour.accent.liveColor(Colour.accent.value).argb)

    private fun accentGlow(): String {
        val color = Colour.accent.liveColor(Colour.accent.value)
        return "#%08X".format((0x40 shl 24) or (color.r shl 16) or (color.g shl 8) or color.b)
    }

    private fun sliderFg(): String {
        val color = Colour.accent.liveColor(Colour.accent.value)
        return "#%08X".format((255 shl 24) or ((color.r * 0.8f).toInt() shl 16) or ((color.g * 0.8f).toInt() shl 8) or (color.b * 0.8f).toInt())
    }

    fun dropdownDocument(
        panelPositions: Map<Category, Pair<Int, Int>>,
        categoryOrder: List<Category> = Category.entries,
        expandedModules: Set<Module>,
        collapsedCategories: Set<Category>,
        activeStringEntry: StringEntry? = null,
        editingProfile: ModuleProfile? = null,
        activeStringText: String = "",
        activeStringCursor: Int = 0,
        activeStringScrollPx: Int = 0,
        listeningKeybind: KeybindEntry? = null,
        openEnumEntry: EnumEntry<*>? = null,
        openColorEntry: ColorEntry? = null,
        itemPicker: ItemPickerState? = null,
        modulePicker: ModulePickerState? = null,
        colorModeOpen: Boolean = false,
        colorEditingChannel: Int? = null,
        colorEditingHex: Boolean = false,
        colorEditingText: String = "",
        openItemListEntry: ItemListEntry? = null,
        openModuleListEntry: ModuleListEntry? = null,
        configX: Int = 10,
        configY: Int = 30,
        configCollapsed: Boolean = true,
        presetName: String = "",
        presetActive: Boolean = false,
        presetCursor: Int = 0,
        presetScrollPx: Int = 0,
        presets: List<String> = emptyList(),
        metaPreset: String? = null,
        meta: onl.luka.grizzly.config.PresetMetadata? = null,
        metaActiveField: String = "",
        metaText: String = "",
        metaCursor: Int = 0,
        metaScrollPx: Int = 0,
        scrollY: Float = 0f,
        includeEntry: (Module, ConfigEntry<*>) -> Boolean = { module, entry ->
            entry.name != "enabled" && !(module.isProtected && entry.name == "keybind") &&
                (entry.visibleWhen?.invoke() != false)
        },
    ): UiDocument {
        return UiDocument(
            ClickGuiBlueprint(
                templates,
                ClickGuiLayoutContext(
                    categoryPositions = panelPositions,
                    categoryOrder = categoryOrder,
                    expandedModules = expandedModules,
                    collapsedCategories = collapsedCategories,
                    activeStringEntry = activeStringEntry,
                    editingProfile = editingProfile,
                    activeStringText = activeStringText,
                    activeStringCursor = activeStringCursor,
                    activeStringScrollPx = activeStringScrollPx,
                    listeningKeybind = listeningKeybind,
                    openEnumEntry = openEnumEntry,
                    openColorEntry = openColorEntry,
                    itemPicker = itemPicker,
                    modulePicker = modulePicker,
                    colorModeOpen = colorModeOpen,
                    colorEditingChannel = colorEditingChannel,
                    colorEditingHex = colorEditingHex,
                    colorEditingText = colorEditingText,
                    openItemListEntry = openItemListEntry,
                    openModuleListEntry = openModuleListEntry,
                    configX = configX,
                    configY = configY,
                    configCollapsed = configCollapsed,
                    presetName = presetName,
                    presetActive = presetActive,
                    presetCursor = presetCursor,
                    presetScrollPx = presetScrollPx,
                    presets = presets,
                    metaPreset = metaPreset,
                    meta = meta,
                    metaActiveField = metaActiveField,
                    metaText = metaText,
                    metaCursor = metaCursor,
                    metaScrollPx = metaScrollPx,
                    dropdownScroll = scrollY,
                    includeEntry = includeEntry,
                ),
            ).instantiateMode("dropdown-mode")
        ).validate("dropdown Click GUI UI").bindGeneratedActions()
    }

    fun categoryPanel(
        category: Category,
        position: Pair<Int, Int>,
        expandedModules: Set<Module>,
        includeEntry: (Module, ConfigEntry<*>) -> Boolean,
    ): UiNode {
        val modules = ModuleManager.getByCategory(category).flatMap { module ->
            val moduleRow = moduleNode(module)
            if (module !in expandedModules) {
                listOf(moduleRow)
            } else {
                listOf(moduleRow) + entryNodes(module, includeEntry)
            }
        }

        return templates.instantiate(
            "category-panel",
            values = mapOf(
                "category.name" to category.name,
                "panel.x" to position.first.toString(),
                "panel.y" to position.second.toString(),
            ),
            slots = mapOf("modules" to modules),
        )
    }

    fun moduleCard(module: Module): UiNode =
        templates.instantiate(
            "module-card",
            values = moduleValues(module),
        )

    fun moduleNode(module: Module): UiNode =
        templates.instantiate(
            "module-row",
            values = moduleValues(module),
        )

    fun entryNodes(
        module: Module,
        includeEntry: (Module, ConfigEntry<*>) -> Boolean,
    ): List<UiNode> {
        val nodes = mutableListOf<UiNode>()
        var previousGroup: ConfigGroup? = null

        for (entry in module.entries.filter { includeEntry(module, it) }) {
            val group = entry.group
            if (group != null && group !== previousGroup) {
                nodes += templates.instantiate(
                    "group-header",
                    values = mapOf("group.name" to label(group.name)),
                )
            }
            nodes += entryNode(module, entry)
            previousGroup = group
        }

        return nodes
    }

    fun entryNode(module: Module, entry: ConfigEntry<*>): UiNode {
        val template = when (entry) {
            is BooleanEntry -> "boolean-entry"
            else -> "entry-row"
        }

        return templates.instantiate(
            template,
            values = mapOf(
                "entry.id" to entryId(module, entry),
                "entry.label" to label(entry.name),
                "entry.value" to entryValue(entry),
                "entry.fg" to if (entry is BooleanEntry && entry.value) "#FFE6E6F0" else "#FF8A8AA0",
            ),
        )
    }

    private fun UiDocument.bindGeneratedActions(): UiDocument = apply {
        Category.entries.forEach { category ->
            onClick(categoryId(category)) { event ->
                onCategoryClicked(category, event)
            }
        }

        onClick("config:header") { event -> onConfigHeaderClicked(event) }
        onClick("config:preset-input") { event ->
            onPresetEditRequested(event)
            true
        }
        onClick("config:save") {
            val name = presetNameProvider().ifBlank { "default" }
            val meta = saveMetadataProvider(name)
            ConfigManager.savePreset(name, meta)
            NotificationManager.showConfig("Config Saved", name)
            true
        }
        onClick("config:load") {
            val name = presetNameProvider().ifBlank { "default" }
            if (ConfigManager.loadPreset(name)) NotificationManager.showConfig("Config Loaded", name)
            else NotificationManager.showConfig("Not Found", name)
            true
        }
        onClick("config:folder") {
            ConfigManager.openPresetFolder()
            true
        }
        ConfigManager.listPresets().forEach { preset ->
            onClick(presetId(preset)) { event ->
                if (event.button == 1) onMetaAction("open:$preset", event) else onPresetSelected(preset)
                true
            }
        }

        listOf(
            "config:meta:anticheat", "config:meta:description", "config:meta:server",
            "config:meta:overwrite", "config:meta:disable", "config:meta:delete",
            "config:mapping:add",
        ).forEach { id ->
            onClick(id) { event ->
                onMetaAction(id.removePrefix("config:"), event)
                true
            }
        }

        Module.Category.entries.forEach { category ->
            onClick("config:meta:cat:${category.name}") { event ->
                onMetaAction("meta:cat:${category.name}", event)
                true
            }
        }

        metadataProvider()?.servers?.indices?.forEach { index ->
            onClick("config:meta:server:$index") { event ->
                onMetaAction("meta:server-remove:$index", event)
                true
            }
        }

        AutoConfig.mappings.value.indices.forEach { index ->
            onClick("config:mapping:server:$index") { event ->
                onMetaAction("mapping:server:$index", event)
                true
            }
            onClick("config:mapping:preset:$index") { event ->
                onMetaAction("mapping:preset:$index", event)
                true
            }
            onClick("config:mapping:remove:$index") { event ->
                onMetaAction("mapping:remove:$index", event)
                true
            }
        }

        ModuleManager.getAll().forEach { module ->
            onClick(moduleId(module)) { event ->
                val clickedExpandZone = event.x >= event.node.bounds.right - 18f && module.entries.isNotEmpty()
                when {
                    event.button == 0 && clickedExpandZone -> {
                        onModuleExpandRequested(module)
                        true
                    }
                    event.button == 1 -> {
                        onModuleExpandRequested(module)
                        true
                    }
                    event.button == 0 && !module.isProtected -> {
                        module.toggle()
                        true
                    }
                    event.button == 0 -> {
                        onModuleExpandRequested(module)
                        true
                    }
                    else -> false
                }
            }

            module.entries.forEach { entry ->
                when (entry) {
                    is BooleanEntry -> onClick(entryId(module, entry)) {
                        entry.value = !entry.value
                        true
                    }
                    is ButtonEntry -> onClick(entryId(module, entry)) {
                        entry.action()
                        true
                    }
                    is HudEditEntry -> onClick(entryId(module, entry)) {
                        if (module is HudModule) {
                            onHudEditRequested(module)
                            true
                        } else {
                            false
                        }
                    }
                    is EnumEntry<*> -> onClick(entryId(module, entry)) { event ->
                        onEnumDropdownRequested(entry, event)
                        true
                    }.also { bindEnumActions(module, entry) }
                    is KeybindEntry -> onClick(entryId(module, entry)) {
                        onKeybindRequested(entry)
                        true
                    }
                    is StringEntry -> onClick(entryId(module, entry)) { event ->
                        onStringEditRequested(entry, event)
                        true
                    }
                    is ColorEntry -> onClick(entryId(module, entry)) { event ->
                        onColorEditRequested(entry, event)
                        true
                    }.also { if (entry === openColorEntry) bindColorActions(entry) }
                    is ItemListEntry -> onClick(entryId(module, entry)) { event ->
                        onItemListRequested(entry, event)
                        true
                    }.also {
                        val picker = openItemPicker
                        if (entry === openItemListEntryForBinding && picker != null) {
                            bindItemPickerActions(entry, picker)
                        }
                    }
                    is ModuleListEntry -> onClick(entryId(module, entry)) { event ->
                        onModuleListRequested(entry, event)
                        true
                    }.also {
                        val picker = openModulePicker
                        if (entry === openModuleListEntryForBinding && picker != null) {
                            bindModulePickerActions(entry, picker)
                        }
                    }
                    is IntEntry -> onClick(entryId(module, entry)) { event ->
                        if (entry.min == Int.MIN_VALUE || entry.max == Int.MAX_VALUE) return@onClick false
                        applyIntDrag(entry, event)
                        onNumericDragStarted(entry, event.node.bounds)
                        true
                    }
                    is FloatEntry -> onClick(entryId(module, entry)) { event ->
                        if (entry.min == -Float.MAX_VALUE || entry.max == Float.MAX_VALUE) return@onClick false
                        applyFloatDrag(entry, event)
                        onNumericDragStarted(entry, event.node.bounds)
                        true
                    }
                    is DoubleEntry -> onClick(entryId(module, entry)) { event ->
                        if (entry.min == -Double.MAX_VALUE || entry.max == Double.MAX_VALUE) return@onClick false
                        applyDoubleDrag(entry, event)
                        onNumericDragStarted(entry, event.node.bounds)
                        true
                    }
                    is IntRangeEntry -> onClick(entryId(module, entry)) { event ->
                        val high = rangeHighClosest(entryLowT(entry), entryHighT(entry), numericT(event))
                        applyIntRangeDrag(entry, event, high)
                        onRangeDragStarted(entry, event.node.bounds, high)
                        true
                    }
                    is FloatRangeEntry -> onClick(entryId(module, entry)) { event ->
                        val high = rangeHighClosest(entryLowT(entry), entryHighT(entry), numericT(event))
                        applyFloatRangeDrag(entry, event, high)
                        onRangeDragStarted(entry, event.node.bounds, high)
                        true
                    }
                    else -> Unit
                }
            }

            bindProfileActions(module)
        }
    }

    /**
     * Index 0 is the module's own configuration, so its bind badge edits the module keybind and
     * it has no delete control.
     */
    private fun UiDocument.bindProfileActions(module: Module): UiDocument = apply {
        if (module.isProtected) return@apply

        module.profiles.forEachIndexed { index, profile ->
            // Clicking a name both switches to that profile and puts a cursor in it, so the one
            // obvious gesture covers picking a profile and naming it.
            onClick(profileNameId(module, index)) { event ->
                onProfileSelected(module, index)
                onProfileRenameRequested(profile, event)
                true
            }
            onClick(profileBindId(module, index)) {
                onKeybindRequested(profile.keybind)
                true
            }
            if (index > 0) {
                onClick(profileRemoveId(module, index)) {
                    onProfileRemoved(module, index)
                    true
                }
            }
        }
        onClick(profileAddId(module)) {
            onProfileAdded(module)
            true
        }
    }

    /**
     * The inline picker uses fixed ids because only one is ever open, so these are bound once
     * against whichever entry that is rather than per row.
     */
    private fun UiDocument.bindColorActions(entry: ColorEntry): UiDocument = apply {
        onClick("color:mode") {
            onColorModeToggled()
            true
        }
        ColorEntry.PickerMode.entries.forEach { mode ->
            onClick("color:mode:${mode.name}") {
                onColorModePicked(entry, mode)
                true
            }
        }
        onClick("color:map") { event ->
            onColorMapDrag(entry, event)
            true
        }
        onClick("color:hue") { event ->
            onColorHueDrag(entry, event)
            true
        }
        onClick("color:alpha") { event ->
            onColorAlphaDrag(entry, event)
            true
        }
        onClick("color:speed") { event ->
            onColorSpeedDrag(entry, event)
            true
        }
        repeat(if (entry.allowAlpha) 4 else 3) { channel ->
            onClick("color:channel:$channel") {
                onColorChannelEdit(entry, channel)
                true
            }
        }
        onClick("color:hex") {
            onColorHexEdit(entry)
            true
        }
    }

    private fun UiDocument.bindModulePickerActions(
        entry: ModuleListEntry,
        picker: ModulePickerState,
    ): UiDocument = apply {
        onClick("module-list:search") { event ->
            onModuleSearchClicked(event)
            true
        }
        picker.rows.forEach { row ->
            onClick("module-list:row:${row.name}") {
                onModuleRowToggled(entry, row.name)
                true
            }
        }
    }

    /** Fixed ids again, so only the picker that is actually open may claim them. */
    private fun UiDocument.bindItemPickerActions(entry: ItemListEntry, picker: ItemPickerState): UiDocument = apply {
        onClick("item-list:search") { event ->
            onItemSearchClicked(event)
            true
        }
        picker.rows.forEach { row ->
            onClick("item-list:row:${row.id}") {
                onItemRowToggled(entry, row.id)
                true
            }
        }
        picker.added.forEach { (id, _) ->
            onClick("item-list:remove:$id") {
                onItemRemoved(entry, id)
                true
            }
        }
    }

    private fun UiDocument.bindEnumActions(module: Module, entry: EnumEntry<*>): UiDocument = apply {
        entry.constants.forEachIndexed { index, _ ->
            onClick(enumOptionId(module, entry, index)) {
                entry.setByIndex(index)
                onEnumOptionChosen(entry)
                true
            }
        }
    }

    private fun applyIntDrag(entry: IntEntry, event: UiPointerEvent) {
        val t = numericT(event)
        entry.value = (entry.min + t * (entry.max - entry.min)).roundToInt()
    }

    private fun applyFloatDrag(entry: FloatEntry, event: UiPointerEvent) {
        val t = numericT(event)
        entry.value = entry.min + t * (entry.max - entry.min)
    }

    private fun applyDoubleDrag(entry: DoubleEntry, event: UiPointerEvent) {
        val t = numericT(event).toDouble()
        entry.value = entry.min + t * (entry.max - entry.min)
    }

    private fun applyIntRangeDrag(entry: IntRangeEntry, event: UiPointerEvent, high: Boolean) {
        val t = numericT(event)
        val value = (entry.min + t * (entry.max - entry.min)).roundToInt()
        val (lo, hi) = entry.value
        entry.value = if (high) lo to value.coerceAtLeast(lo) else value.coerceAtMost(hi) to hi
    }

    private fun applyFloatRangeDrag(entry: FloatRangeEntry, event: UiPointerEvent, high: Boolean) {
        val t = numericT(event)
        val scale = Math.pow(10.0, entry.decimals.toDouble()).toFloat()
        val value = (Math.round((entry.min + t * (entry.max - entry.min)) * scale).toFloat()) / scale
        val (lo, hi) = entry.value
        entry.value = if (high) lo to value.coerceAtLeast(lo) else value.coerceAtMost(hi) to hi
    }

    private fun numericT(event: UiPointerEvent): Float =
        sliderT(event.node.bounds, event.x)

    internal companion object {
        const val DEFAULT_SLIDER_KNOB_SIZE = 8f
        const val DEFAULT_SLIDER_TRACK_INSET = DEFAULT_SLIDER_KNOB_SIZE / 2f

        fun sliderT(bounds: UiRect, x: Float): Float =
            ((x - (bounds.x + DEFAULT_SLIDER_TRACK_INSET)) /
                (bounds.width - DEFAULT_SLIDER_TRACK_INSET * 2f).coerceAtLeast(1f))
                .coerceIn(0f, 1f)
    }

    private fun entryLowT(entry: IntRangeEntry): Float =
        (entry.value.first - entry.min).toFloat() / (entry.max - entry.min).coerceAtLeast(1).toFloat()

    private fun entryHighT(entry: IntRangeEntry): Float =
        (entry.value.second - entry.min).toFloat() / (entry.max - entry.min).coerceAtLeast(1).toFloat()

    private fun entryLowT(entry: FloatRangeEntry): Float =
        (entry.value.first - entry.min) / (entry.max - entry.min).coerceAtLeast(0.0001f)

    private fun entryHighT(entry: FloatRangeEntry): Float =
        (entry.value.second - entry.min) / (entry.max - entry.min).coerceAtLeast(0.0001f)

    private fun rangeHighClosest(low: Float, high: Float, target: Float): Boolean =
        kotlin.math.abs(target - high) < kotlin.math.abs(target - low)

    private fun moduleValues(module: Module): Map<String, String> =
        mapOf(
            "id" to moduleId(module),
            "name" to module.name,
            "description" to module.description,
            "bg" to if (module.isEnabled()) themeValues()["theme.entrySelectedBg"].orEmpty() else themeValues()["theme.moduleBg"].orEmpty(),
            "fg" to if (module.isEnabled()) "#FFE6E6F0" else "#FF8A8AA0",
        ) + themeValues()





}
