package onl.luka.grizzly.gui.ui

import onl.luka.grizzly.config.ConfigGroup
import onl.luka.grizzly.config.entry.BooleanEntry
import onl.luka.grizzly.config.entry.ButtonEntry
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.config.entry.ConfigEntry
import onl.luka.grizzly.config.entry.DoubleEntry
import onl.luka.grizzly.config.entry.EnumEntry
import onl.luka.grizzly.config.entry.FloatEntry
import onl.luka.grizzly.config.entry.FloatRangeEntry
import onl.luka.grizzly.config.entry.HudEditEntry
import onl.luka.grizzly.config.entry.IntEntry
import onl.luka.grizzly.config.entry.IntRangeEntry
import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.config.entry.ServerMappingEntry
import onl.luka.grizzly.config.entry.KeybindEntry
import onl.luka.grizzly.module.ModuleProfile
import onl.luka.grizzly.config.entry.ModuleListEntry
import onl.luka.grizzly.config.entry.StringEntry
import onl.luka.grizzly.module.ModuleManager
import onl.luka.grizzly.module.modules.other.AutoConfig
import kotlin.math.pow
import onl.luka.grizzly.module.modules.other.Colour
import onl.luka.grizzly.module.modules.other.Font
import onl.luka.grizzly.module.Module
import onl.luka.grizzly.module.Module.Category
import org.lwjgl.glfw.GLFW

internal class ClickGuiBlueprint(
    private val templates: UiTemplateSet,
    private val layout: ClickGuiLayoutContext,
) {
    fun instantiateMode(name: String): UiNode =
        expandNode(templates.template(name), ClickGuiData())
            ?: error("Click GUI mode '$name' did not produce a root node")

    private fun expandNode(node: UiNode, data: ClickGuiData): UiNode? {
        return when (node.type) {
            "component" -> {
                val templateName = node.attributes["name"]?.replaceData(data)
                    ?: error("component is missing a name")
                expandNode(templates.template(templateName), data)
            }
            "for" -> {
                val items = loopItems(node.attributes["each"], data)
                UiNode(
                    type = "box",
                    axis = node.axis,
                    width = resolvedSize(node, data, width = true),
                    height = resolvedSize(node, data, width = false),
                    x = node.attributes["x"]?.replaceData(data)?.toFloatOrNull() ?: node.x,
                    y = node.attributes["y"]?.replaceData(data)?.toFloatOrNull() ?: node.y,
                    style = node.style,
                    attributes = node.attributes.mapValues { it.value.replaceData(data) },
                    children = items.flatMap { item ->
                        node.children.mapNotNull { child -> expandNode(child, item) }
                    },
                )
            }
            "if" -> {
                if (condition(node.attributes["condition"], data)) {
                    UiNode(
                        type = "box",
                        axis = node.axis,
                        width = resolvedSize(node, data, width = true),
                        height = resolvedSize(node, data, width = false),
                        style = node.style,
                        attributes = node.attributes.mapValues { it.value.replaceData(data) },
                        children = node.children.mapNotNull { expandNode(it, data) },
                    )
                } else {
                    null
                }
            }
            else -> {
                val attrs = node.attributes.mapValues { it.value.replaceData(data) }
                UiNode(
                    type = node.type,
                    id = node.id?.replaceData(data),
                    text = node.text?.replaceData(data),
                    axis = node.axis,
                    width = resolvedSize(node, data, width = true),
                    height = resolvedSize(node, data, width = false),
                    x = attrs["x"]?.toFloatOrNull() ?: node.x,
                    y = attrs["y"]?.toFloatOrNull() ?: node.y,
                    style = node.style,
                    attributes = attrs,
                    children = node.children.mapNotNull { expandNode(it, data) },
                )
            }
        }
    }

    private fun loopItems(each: String?, data: ClickGuiData): List<ClickGuiData> =
        when (each) {
            "categories" -> layout.categoryOrder
                .map { data.copy(category = it) }
            "modules" -> data.category
                ?.let(ModuleManager::getByCategory)
                ?.map { data.copy(module = it) }
                ?: emptyList()
            "selected-modules" -> layout.selectedCategory
                ?.let(ModuleManager::getByCategory)
                ?.map { data.copy(category = layout.selectedCategory, module = it) }
                ?: emptyList()
            "module-description-lines" -> data.module
                ?.let(::moduleDescriptionLines)
                ?.map { data.copy(moduleDescriptionLine = it) }
                ?: emptyList()
            "detail-settings" -> layout.detailModule
                ?.let { module -> settingItems(module).map { data.copy(module = module, setting = it) } }
                ?: emptyList()
            "settings" -> data.module
                ?.let { module -> settingItems(module).map { data.copy(setting = it) } }
                ?: emptyList()
            "module-profiles" -> data.module
                ?.profiles
                ?.indices
                ?.map { data.copy(profileIndex = it) }
                ?: emptyList()
            "module-picker-rows" -> layout.modulePicker
                ?.rows
                ?.map { data.copy(moduleRow = it) }
                ?: emptyList()
            "item-picker-rows" -> layout.itemPicker
                ?.rows
                ?.map { data.copy(itemRow = it) }
                ?: emptyList()
            "item-picker-added" -> layout.itemPicker
                ?.added
                ?.map { data.copy(addedItem = it) }
                ?: emptyList()
            "color-modes" -> layout.openColorEntry
                ?.let(::colorModes)
                ?.map { data.copy(colorMode = it) }
                ?: emptyList()
            "color-channels" -> layout.openColorEntry
                ?.let(::colorChannels)
                ?.map { data.copy(colorChannel = it) }
                ?: emptyList()
            "entry-enum-options" -> ((data.setting as? ClickGuiSettingItem.Entry)?.entry as? EnumEntry<*>)
                ?.constants
                ?.mapIndexed { index, constant ->
                    data.copy(enumOptionIndex = index, enumOptionName = constant.name)
                }
                ?: emptyList()
            "presets" -> layout.presets.map { data.copy(presetName = it) }
            "meta-servers" -> layout.meta?.servers
                ?.mapIndexed { index, server -> data.copy(metaServer = server, metaIndex = index) }
                ?: emptyList()
            "meta-categories" -> Module.Category.entries.map { data.copy(metaCategory = it) }
            "config-mappings" -> AutoConfig.mappings.value
                .indices
                .map { data.copy(metaIndex = it) }
            else -> emptyList()
        }

    private fun metaFieldText(field: String, fallback: String): String =
        if (layout.metaActiveField == field) layout.metaText else fallback

    private fun metaHasCategory(data: ClickGuiData): Boolean {
        val category = data.metaCategory ?: return false
        return layout.meta?.covers(category) == true
    }

    private fun mappingAt(data: ClickGuiData) =
        AutoConfig.mappings.value.getOrNull(data.metaIndex)

    private fun mark(on: Boolean): String = if (on) "[x]" else "[ ]"

    private fun openColorMode(): ColorEntry.PickerMode? = layout.openColorEntry?.pickerMode

    /** Chroma shows its live cycling colour rather than the stored one. */
    private fun colorHsv(): Triple<Float, Float, Float>? {
        val entry = layout.openColorEntry ?: return null
        val (baseHue, baseSat, baseValue) = rgbToHsvTriple(entry.customValue)
        if (entry.pickerMode != ColorEntry.PickerMode.CHROMA) return Triple(baseHue, baseSat, baseValue)

        val hue = ((ColorEntry.chromaTimeSeconds() * entry.chromaSpeed * 360f) % 360f + 360f) % 360f
        return Triple(hue, entry.chromaSaturation, entry.chromaBrightness)
    }

    private fun colorHex(color: onl.luka.grizzly.config.entry.Color): String =
        "#%02X%02X%02X%02X".format(color.a, color.r, color.g, color.b)

    private fun rgbToHsvTriple(color: onl.luka.grizzly.config.entry.Color): Triple<Float, Float, Float> {
        val r = color.r / 255f
        val g = color.g / 255f
        val b = color.b / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == r -> ((g - b) / delta % 6f) * 60f
            max == g -> ((b - r) / delta + 2f) * 60f
            else -> ((r - g) / delta + 4f) * 60f
        }.let { if (it < 0f) it + 360f else it }
        return Triple(hue, if (max == 0f) 0f else delta / max, max)
    }

    /** The theme option is meaningless for the accent colour, which is the theme. */
    private fun colorModes(entry: ColorEntry): List<ColorEntry.PickerMode> =
        if (entry !== Colour.accent) {
            listOf(ColorEntry.PickerMode.CUSTOM, ColorEntry.PickerMode.THEME, ColorEntry.PickerMode.CHROMA)
        } else {
            listOf(ColorEntry.PickerMode.CUSTOM, ColorEntry.PickerMode.CHROMA)
        }

    private fun colorChannels(entry: ColorEntry): List<ColorChannel> {
        val custom = entry.customValue
        val channels = mutableListOf(
            ColorChannel(0, "R", custom.r),
            ColorChannel(1, "G", custom.g),
            ColorChannel(2, "B", custom.b),
        )
        if (entry.allowAlpha) channels += ColorChannel(3, "A", custom.a)
        return channels
    }

    private fun optionOwner(data: ClickGuiData): EnumEntry<*>? =
        (data.setting as? ClickGuiSettingItem.Entry)?.entry as? EnumEntry<*>

    private fun settingItems(module: Module): List<ClickGuiSettingItem> {
        val result = mutableListOf<ClickGuiSettingItem>()
        var previousGroup: ConfigGroup? = null
        for (entry in module.entries.filter { layout.includeEntry(module, it) }) {
            val group = entry.group
            if (group != null && group !== previousGroup) {
                result += ClickGuiSettingItem.Group(group)
            }
            result += ClickGuiSettingItem.Entry(module, entry)
            previousGroup = group
        }
        return result
    }

    private fun condition(condition: String?, data: ClickGuiData): Boolean =
        when (condition) {
            "category.expanded" -> data.category !in layout.collapsedCategories
            "module.expanded" -> data.module in layout.expandedModules
            "module.supports-profiles" -> data.module?.isProtected == false
            "module.has-settings" -> data.module?.entries?.any { layout.includeEntry(data.module, it) } == true
            "setting.module-list-open" ->
                (data.setting as? ClickGuiSettingItem.Entry)?.entry === layout.openModuleListEntry &&
                    layout.modulePicker != null
            "setting.item-list-open" ->
                (data.setting as? ClickGuiSettingItem.Entry)?.entry === layout.openItemListEntry &&
                    layout.itemPicker != null
            "item.has-added" -> layout.itemPicker?.added?.isNotEmpty() == true
            "setting.color-open" ->
                (data.setting as? ClickGuiSettingItem.Entry)?.entry === layout.openColorEntry &&
                    layout.openColorEntry != null
            "color.mode-open" -> layout.colorModeOpen
            "color.is-custom" -> openColorMode() == ColorEntry.PickerMode.CUSTOM
            "color.is-theme" -> openColorMode() == ColorEntry.PickerMode.THEME
            "color.is-chroma" -> openColorMode() == ColorEntry.PickerMode.CHROMA
            "color.has-map" -> openColorMode() != ColorEntry.PickerMode.THEME
            "color.has-alpha" -> layout.openColorEntry?.allowAlpha == true
            "setting.enum-open" ->
                (data.setting as? ClickGuiSettingItem.Entry)?.entry === layout.openEnumEntry &&
                    layout.openEnumEntry != null
            "setting.is-entry" -> data.setting is ClickGuiSettingItem.Entry
            "setting.is-group" -> data.setting is ClickGuiSettingItem.Group
            "config.expanded" -> !layout.configCollapsed
            "presets.empty" -> layout.presets.isEmpty()
            "presets.present" -> layout.presets.isNotEmpty()
            "meta.open" -> layout.meta != null
            "meta.servers.empty" -> layout.meta?.servers.isNullOrEmpty()
            "meta.servers.present" -> layout.meta?.servers?.isNotEmpty() == true
            "mappings.empty" -> AutoConfig.mappings.value.isEmpty()
            "mappings.present" -> AutoConfig.mappings.value.isNotEmpty()
            else -> false
        }

    private fun resolvedSize(node: UiNode, data: ClickGuiData, width: Boolean): UiSize {
        val primary = if (width) "w" else "h"
        val secondary = if (width) "width" else "height"
        return node.attributes[primary]?.replaceData(data)?.toUiSizeOrNull()
            ?: node.attributes[secondary]?.replaceData(data)?.toUiSizeOrNull()
            ?: if (width) node.width else node.height
    }

    private fun String.replaceData(data: ClickGuiData): String =
        Regex("""\{\{\s*([A-Za-z0-9_.-]+)\s*}}""").replace(this) { match ->
            valueFor(data, match.groupValues[1])
        }

    private fun String.toUiSizeOrNull(): UiSize? =
        when (lowercase()) {
            "", "wrap", "auto" -> UiSize.Wrap
            "fill", "match", "*" -> UiSize.Fill
            else -> toFloatOrNull()?.let(UiSize::Px) ?: removeSuffix("px").toFloatOrNull()?.let(UiSize::Px)
        }

    private data class ColorChannel(val index: Int, val label: String, val value: Int)

    private data class ClickGuiData(
        val category: Module.Category? = null,
        val module: Module? = null,
        val setting: ClickGuiSettingItem? = null,
        val enumOptionIndex: Int = -1,
        val enumOptionName: String = "",
        val presetName: String = "",
        val moduleDescriptionLine: String = "",
        val colorMode: ColorEntry.PickerMode? = null,
        val itemRow: ItemPickerRow? = null,
        val moduleRow: ModulePickerRow? = null,
        val addedItem: Pair<String, String>? = null,
        val colorChannel: ColorChannel? = null,
        val profileIndex: Int = -1,
        val metaServer: String = "",
        val metaCategory: Module.Category? = null,
        val metaIndex: Int = -1,
    )

    private fun profileOf(data: ClickGuiData): ModuleProfile? =
        data.module?.profiles?.getOrNull(data.profileIndex)

    private fun valueFor(data: ClickGuiData, key: String): String {
        val entrySetting = data.setting as? ClickGuiSettingItem.Entry
        val groupSetting = data.setting as? ClickGuiSettingItem.Group
        val entry = entrySetting?.entry
        val entryModule = entrySetting?.module ?: data.module

        return when (key) {
            "category.name" -> data.category?.name.orEmpty()
            "category.id" -> data.category?.let(::categoryId).orEmpty()
            "category.x" -> data.category?.let { layout.categoryPositions[it]?.first?.toString() }.orEmpty()
            "category.y" -> data.category?.let { layout.categoryPositions[it]?.second?.toString() }.orEmpty()
            "theme.bg" -> hex(shade(9, 0.06f, 115))
            "theme.panelBg" -> hex(shade(20, 0.10f, 242))
            "theme.headerBg" -> hex(shade(24, 0.18f))
            "theme.sidebarHeaderBg" -> hex(shade(24, 0.16f))
            "theme.sidebarRailBg" -> hex(shade(16, 0.07f))
            "theme.sidebarDividerBg" -> hex(shade(255, 0.05f))
            "theme.moduleBg" -> hex(shade(24, 0.08f))
            "theme.moduleHoverBg" -> hex(shade(38, 0.13f))
            "theme.entryBg" -> hex(shade(21, 0.07f))
            "theme.entrySelectedBg" -> hex(shade(42, 0.18f))
            "theme.entryHoverBg" -> hex(shade(32, 0.12f))
            "theme.controlBg" -> hex(shade(42, 0.14f))
            "theme.controlActiveBg" -> hex(shade(45, 0.25f))
            "theme.sliderBg" -> hex(shade(38, 0.14f))
            "theme.sliderFg" -> sliderFg()
            "theme.accentSoft" -> accentSoftHex()
            "theme.text" -> "#FFE6E6F0"
            "theme.textDim" -> "#FF8A8AA0"
            "theme.accent" -> accentHex()
            "theme.transparent" -> "#00000000"
            "category.bg" -> if (data.category == layout.selectedCategory) hex(shade(42, 0.18f)) else "#00000000"
            "category.fg" -> if (data.category == layout.selectedCategory) "#FFE6E6F0" else "#FF8A8AA0"
            "category.sign" -> if (data.category in layout.collapsedCategories) "+" else "-"
            "config.x" -> layout.configX.toString()
            "config.y" -> layout.configY.toString()
            "config.title" -> "CONFIGS ${if (layout.configCollapsed) "+" else "-"}"
            "config.sign" -> if (layout.configCollapsed) "+" else "-"
            "preset.input" -> if (layout.presetName.isBlank() && !layout.presetActive) "preset name..." else layout.presetName
            "preset.active" -> layout.presetActive.toString()
            "preset.cursor" -> layout.presetCursor.toString()
            "preset.scroll" -> layout.presetScrollPx.toString()
            "meta.name" -> layout.metaPreset.orEmpty()
            "meta.anticheat" -> metaFieldText("anticheat", layout.meta?.anticheat.orEmpty())
            "meta.description" -> metaFieldText("description", layout.meta?.description.orEmpty())
            "meta.server-input" -> metaFieldText("server", "add server...")
            "meta.anticheat-active" -> (layout.metaActiveField == "anticheat").toString()
            "meta.description-active" -> (layout.metaActiveField == "description").toString()
            "meta.server-active" -> (layout.metaActiveField == "server").toString()
            "meta.cursor" -> layout.metaCursor.toString()
            "meta.scroll" -> layout.metaScrollPx.toString()
            "meta.server" -> data.metaServer
            "meta.server.id" -> "config:meta:server:${data.metaIndex}"
            "meta.category.name" -> data.metaCategory?.name?.lowercase().orEmpty()
            "meta.category.id" -> "config:meta:cat:${data.metaCategory?.name.orEmpty()}"
            "meta.category.fg" -> if (metaHasCategory(data)) "#FFE6E6F0" else "#FF6A6A80"
            "meta.category.bg" -> if (metaHasCategory(data)) hex(shade(42, 0.18f)) else hex(shade(21, 0.07f))
            "meta.overwrite.label" -> "overwrite unbound  ${mark(layout.meta?.overwriteUnbound == true)}"
            "meta.disable.label" -> "disable unaffected  ${mark(layout.meta?.disableUnaffected == true)}"
            "mapping.server" -> mappingAt(data)?.server.orEmpty().ifEmpty { "(any)" }
            "mapping.preset" -> mappingAt(data)?.preset.orEmpty()
            "mapping.server.id" -> "config:mapping:server:${data.metaIndex}"
            "mapping.preset.id" -> "config:mapping:preset:${data.metaIndex}"
            "mapping.remove.id" -> "config:mapping:remove:${data.metaIndex}"
            "mapping.active" -> (layout.metaActiveField == "mapping:${data.metaIndex}").toString()
            "preset.name" -> data.presetName
            "preset.id" -> presetId(data.presetName)
            "preset.fg" -> if (data.presetName == layout.presetName) "#FFE6E6F0" else "#FF8A8AA0"
            "preset.bg" -> if (data.presetName == layout.presetName) hex(shade(42, 0.18f)) else hex(shade(21, 0.07f))
            "scroll.dropdown" -> layout.dropdownScroll.toString()
            "module.id" -> data.module?.let(::moduleId).orEmpty()
            "module.name" -> data.module?.name.orEmpty()
            "module.description" -> data.module?.description.orEmpty()
            "module.descriptionLine" -> data.moduleDescriptionLine
            "module.cardH" -> data.module?.let { moduleCardHeight(it).toString() } ?: "54"
            "module.addProfileId" -> data.module?.let(::profileAddId).orEmpty()
            "profile.nameId" -> data.module?.let { profileNameId(it, data.profileIndex) }.orEmpty()
            "profile.bindId" -> data.module?.let { profileBindId(it, data.profileIndex) }.orEmpty()
            "profile.removeId" -> data.module?.let { profileRemoveId(it, data.profileIndex) }.orEmpty()
            "profile.name" -> profileOf(data)
                ?.let { if (it === layout.editingProfile) layout.activeStringText else it.name }
                .orEmpty()
            "profile.nameActive" -> (profileOf(data) === layout.editingProfile).toString()
            "profile.nameCursor" -> if (profileOf(data) === layout.editingProfile) {
                layout.activeStringCursor.toString()
            } else {
                "0"
            }
            "profile.nameScroll" -> if (profileOf(data) === layout.editingProfile) {
                layout.activeStringScrollPx.toString()
            } else {
                "0"
            }
            "profile.nameBg" -> if (profileOf(data) === layout.editingProfile) {
                hex(shade(30, 0.12f))
            } else {
                "#00000000"
            }
            "profile.key" -> profileOf(data)
                ?.let { if (layout.listeningKeybind === it.keybind) "..." else keyName(it.keybind.value) }
                .orEmpty()
            "profile.bg" -> if (data.module?.activeProfile == data.profileIndex) {
                hex(shade(42, 0.18f))
            } else {
                hex(shade(24, 0.08f))
            }
            "profile.fg" -> if (data.module?.activeProfile == data.profileIndex) "#FFE6E6F0" else "#FF8A8AA0"
            "profile.removeSign" -> if (data.profileIndex > 0) "x" else ""
            "module.bg" -> if (data.module?.isEnabled() == true) accentHex() else "#00000000"
            "module.hoverBg" -> if (data.module?.isEnabled() == true) accentLiftHex() else "#14FFFFFF"
            "module.cardBg" -> hex(shade(30, 0.10f))
            "module.cardHoverBg" -> hex(shade(42, 0.15f))
            "module.cardOverlay" -> data.module
                ?.takeIf { it.isEnabled() }
                ?.let { accentOverlayHex() }
                ?: "#00000000"
            "module.fg" -> if (data.module?.isEnabled() == true) accentTextHex() else "#FF8A8AA0"
            "module.signFg" -> if (data.module?.isEnabled() == true) accentTextHex() else "#FF8A8AA0"
            "module.on" -> (data.module?.isEnabled() == true).toString()
            "module.toggleKnob" -> if (data.module?.isEnabled() == true) accentHex() else hex(shade(140, 0.10f))
            "detail.name" -> layout.detailModule?.name.orEmpty()
            "detail.description" -> layout.detailModule?.description.orEmpty()
            "detail.on" -> (layout.detailModule?.isEnabled() == true).toString()
            "detail.toggleKnob" -> if (layout.detailModule?.isEnabled() == true) accentHex() else hex(shade(140, 0.10f))
            "module.sign" -> data.module
                ?.takeIf { module -> module.entries.any { layout.includeEntry(module, it) } }
                ?.let { module -> if (module in layout.expandedModules) "-" else "+" }
                .orEmpty()
            "setting.template" -> settingTemplate(data.setting)
            "entry.id" -> if (entry != null && entryModule != null) entryId(entryModule, entry) else ""
            "entry.label" -> entry?.name?.let(::label).orEmpty()
            "entry.value" -> entry?.let { entryValue(it, layout) }.orEmpty()
            "entry.fg" -> if (entry is BooleanEntry && entry.value) "#FFE6E6F0" else "#FF8A8AA0"
            "entry.on" -> if (entry is BooleanEntry && entry.value) "true" else "false"
            "entry.progress" -> entry?.let(::entryProgress) ?: "0"
            "entry.low" -> entry?.let(::entryLowProgress) ?: "0"
            "entry.high" -> entry?.let(::entryHighProgress) ?: "1"
            "entry.color" -> entry?.let(::entryColor) ?: "#FFFFFFFF"
            "entry.count" -> when (entry) {
                is ItemListEntry -> entry.value.size.toString()
                is ModuleListEntry -> entry.value.size.toString()
                else -> ""
            }
            "entry.enumWidth" -> (entry as? EnumEntry<*>)?.let(::enumSelectedControlWidth)?.toString() ?: "64"
            "entry.open" -> when (entry) {
                is EnumEntry<*> -> (entry === layout.openEnumEntry).toString()
                is ItemListEntry -> (entry === layout.openItemListEntry).toString()
                is ModuleListEntry -> (entry === layout.openModuleListEntry).toString()
                else -> "false"
            }
            "entry.active" -> if (entry === layout.activeStringEntry) "true" else "false"
            "entry.cursor" -> if (entry === layout.activeStringEntry) layout.activeStringCursor.toString() else "0"
            "entry.scroll" -> if (entry === layout.activeStringEntry) layout.activeStringScrollPx.toString() else "0"
            "enum.option.id" -> optionOwner(data)
                ?.let { owner -> entryModule?.let { enumOptionId(it, owner, data.enumOptionIndex) } }
                .orEmpty()
            "enum.option.name" -> label(data.enumOptionName)
            "enum.option.bg" -> if (optionOwner(data)?.value?.name == data.enumOptionName) hex(shade(42, 0.18f)) else hex(shade(21, 0.07f))
            "enum.option.fg" -> if (optionOwner(data)?.value?.name == data.enumOptionName) "#FFE6E6F0" else "#FF8A8AA0"
            "modules.header" -> layout.modulePicker?.header.orEmpty()
            "modules.search" -> layout.modulePicker
                ?.let { if (it.search.isBlank() && !it.searchActive) "Search..." else it.search }
                .orEmpty()
            "modules.searchFg" -> layout.modulePicker
                ?.let { if (it.search.isBlank() && !it.searchActive) "#FF8A8AA0" else "#FFE6E6F0" }
                .orEmpty()
            "modules.searchActive" -> (layout.modulePicker?.searchActive == true).toString()
            "modules.searchCursor" -> (layout.modulePicker?.searchCursor ?: 0).toString()
            "modules.searchScroll" -> (layout.modulePicker?.searchScroll ?: 0).toString()
            "modules.scroll" -> (layout.modulePicker?.rowScroll ?: 0).toString()
            "modules.listH" -> (layout.modulePicker?.listHeight ?: 0).toString()
            "modules.leadSpace" -> (layout.modulePicker?.leadSpace ?: 0).toString()
            "modules.tailSpace" -> (layout.modulePicker?.tailSpace ?: 0).toString()
            "modrow.id" -> data.moduleRow?.let { "module-list:row:${it.name}" }.orEmpty()
            "modrow.label" -> data.moduleRow?.name.orEmpty()
            "modrow.hidden" -> (data.moduleRow?.hidden == true).toString()
            "item.header" -> layout.itemPicker?.header.orEmpty()
            "item.search" -> layout.itemPicker
                ?.let { if (it.search.isBlank() && !it.searchActive) "Search..." else it.search }
                .orEmpty()
            "item.searchFg" -> layout.itemPicker
                ?.let { if (it.search.isBlank() && !it.searchActive) "#FF8A8AA0" else "#FFE6E6F0" }
                .orEmpty()
            "item.searchActive" -> (layout.itemPicker?.searchActive == true).toString()
            "item.searchCursor" -> (layout.itemPicker?.searchCursor ?: 0).toString()
            "item.searchScroll" -> (layout.itemPicker?.searchScroll ?: 0).toString()
            "item.scroll" -> (layout.itemPicker?.rowScroll ?: 0).toString()
            "item.addedScroll" -> (layout.itemPicker?.addedScroll ?: 0).toString()
            "item.leadSpace" -> (layout.itemPicker?.leadSpace ?: 0).toString()
            "item.tailSpace" -> (layout.itemPicker?.tailSpace ?: 0).toString()
            "item.listH" -> (layout.itemPicker?.listHeight ?: 0).toString()
            "row.id" -> data.itemRow?.let { "item-list:row:${it.id}" }.orEmpty()
            "row.icon" -> data.itemRow?.icon.orEmpty()
            "row.label" -> data.itemRow?.label.orEmpty()
            "row.action" -> if (data.itemRow?.added == true) "-" else "+"
            "row.actionBg" -> if (data.itemRow?.added == true) hex(shade(42, 0.18f)) else hex(shade(32, 0.12f))
            "added.removeId" -> data.addedItem?.let { "item-list:remove:${it.first}" }.orEmpty()
            "added.icon" -> data.addedItem?.second.orEmpty()
            "mode.id" -> data.colorMode?.let { "color:mode:${it.name}" }.orEmpty()
            "mode.label" -> data.colorMode?.name?.let(::label).orEmpty()
            "mode.fg" -> if (data.colorMode == openColorMode()) "#FFE6E6F0" else "#FF8A8AA0"
            "mode.bg" -> if (data.colorMode == openColorMode()) hex(shade(42, 0.18f)) else hex(shade(21, 0.07f))
            "channel.id" -> data.colorChannel?.let { "color:channel:${it.index}" }.orEmpty()
            "channel.label" -> data.colorChannel?.label.orEmpty()
            "channel.value" -> data.colorChannel?.let { channel ->
                if (layout.colorEditingChannel == channel.index && !layout.colorEditingHex) layout.colorEditingText
                else channel.value.toString()
            }.orEmpty()
            "color.mode" -> layout.openColorEntry?.pickerMode?.name?.let(::label).orEmpty()
            "color.hue" -> colorHsv()?.first?.toString().orEmpty()
            "color.saturation" -> colorHsv()?.second?.toString().orEmpty()
            "color.value" -> colorHsv()?.third?.toString().orEmpty()
            "color.preview" -> layout.openColorEntry?.value?.let(::colorHex).orEmpty()
            "color.alpha" -> ((layout.openColorEntry?.value?.a ?: 255) / 255f).toString()
            "color.hex" -> if (layout.colorEditingHex) layout.colorEditingText
                else layout.openColorEntry?.value?.let(::colorHex).orEmpty()
            "color.speed" -> "%.2f".format(layout.openColorEntry?.chromaSpeed ?: 1f)
            "color.speedProgress" ->
                (((layout.openColorEntry?.chromaSpeed ?: 1f) - 0.05f) / (8f - 0.05f)).coerceIn(0f, 1f).toString()
            "group.name" -> groupSetting?.group?.name?.let(::label).orEmpty()
            else -> ""
        }
    }

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

    /**
     * Whichever of the two labels contrasts harder against the accent wins. A weighted average
     * of the raw channels calls bright greens dark, because it never linearises them.
     */
    private fun accentTextHex(): String {
        val color = Colour.accent.liveColor(Colour.accent.value)
        val luminance = relativeLuminance(color.r, color.g, color.b)
        val againstDark = (luminance + 0.05f) / (relativeLuminance(0x14, 0x14, 0x1A) + 0.05f)
        val againstLight = (relativeLuminance(0xF2, 0xF2, 0xF8) + 0.05f) / (luminance + 0.05f)
        return if (againstDark >= againstLight) "#FF14141A" else "#FFF2F2F8"
    }

    private fun relativeLuminance(r: Int, g: Int, b: Int): Float {
        fun channel(value: Int): Float {
            val v = value / 255f
            return if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
        }
        return 0.2126f * channel(r) + 0.7152f * channel(g) + 0.0722f * channel(b)
    }

    private fun accentLiftHex(): String {
        val color = Colour.accent.liveColor(Colour.accent.value)
        fun lift(channel: Int) = (channel + (255 - channel) * 0.18f).toInt().coerceIn(0, 255)
        return "#%08X".format((255 shl 24) or (lift(color.r) shl 16) or (lift(color.g) shl 8) or lift(color.b))
    }

    private fun accentSoftHex(): String {
        val color = Colour.accent.liveColor(Colour.accent.value)
        return "#%08X".format((105 shl 24) or (color.r shl 16) or (color.g shl 8) or color.b)
    }

    private fun accentOverlayHex(): String {
        val color = Colour.accent.liveColor(Colour.accent.value)
        return "#%08X".format((18 shl 24) or (color.r shl 16) or (color.g shl 8) or color.b)
    }

    private fun sliderFg(): String {
        val color = Colour.accent.liveColor(Colour.accent.value)
        return "#%08X".format((255 shl 24) or ((color.r * 0.8f).toInt() shl 16) or ((color.g * 0.8f).toInt() shl 8) or (color.b * 0.8f).toInt())
    }

    private fun moduleCardHeight(module: Module): Int {
        val lines = moduleDescriptionLines(module).size
        return 24 + (lines * SIDEBAR_CARD_DESC_LINE_H).coerceAtLeast(SIDEBAR_CARD_DESC_LINE_H) + 13
    }

    private fun moduleDescriptionLines(module: Module): List<String> =
        wrapText(module.description, SIDEBAR_CARD_DESCRIPTION_W).ifEmpty { listOf("") }

    private fun wrapText(text: String, maxWidth: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val font = Font.getFont()
        val lines = mutableListOf<String>()
        var current = ""
        for (word in text.split(Regex("\\s+")).filter { it.isNotBlank() }) {
            val candidate = if (current.isBlank()) word else "$current $word"
            if (font.width(Font.styledText(candidate)) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotBlank()) lines += current
                current = word
            }
        }
        if (current.isNotBlank()) lines += current
        return lines
    }

}

private const val SIDEBAR_CARD_DESCRIPTION_W = 288
private const val SIDEBAR_CARD_DESC_LINE_H = 12

internal fun enumSelectedControlWidth(entry: EnumEntry<*>): Int =
    (enumLabelWidth(label(entry.value.name)) + ENUM_CONTROL_CHROME_W).coerceIn(42, 116)

internal fun enumDropdownWidth(entry: EnumEntry<*>): Int {
    val selectedW = enumSelectedControlWidth(entry)
    val widestOptionW = entry.constants
        .maxOfOrNull { enumLabelWidth(label(it.name)) + ENUM_OPTION_PADDING_W }
        ?: selectedW
    return widestOptionW.coerceAtLeast(selectedW).coerceIn(64, 180)
}

private const val ENUM_CONTROL_CHROME_W = 18
private const val ENUM_OPTION_PADDING_W = 10

private fun enumLabelWidth(text: String): Int =
    Font.getFont().width(Font.styledText(text))

internal fun moduleId(module: Module): String =
    "module:${module.name}"



internal fun categoryId(category: Module.Category): String =
    "category:${category.name}"

internal fun entryId(module: Module, entry: ConfigEntry<*>): String =
    "entry:${module.name}:${entry.name}"

// Module qualified, or every module with a "mode" enum would share one handler id.
internal fun enumOptionId(module: Module, entry: EnumEntry<*>, index: Int): String =
    "enum:${module.name}:${entry.name}:$index"

internal fun profileNameId(module: Module, index: Int): String =
    "profile-name:${module.name}:$index"

internal fun profileBindId(module: Module, index: Int): String =
    "profile-bind:${module.name}:$index"

internal fun profileRemoveId(module: Module, index: Int): String =
    "profile-del:${module.name}:$index"

internal fun profileAddId(module: Module): String =
    "profile-add:${module.name}"

internal fun presetId(name: String): String =
    "config:preset:$name"

internal fun label(name: String): String =
    name.replace('_', ' ').replaceFirstChar { it.uppercase() }

internal fun settingTemplate(setting: ClickGuiSettingItem?): String =
    when (setting) {
        is ClickGuiSettingItem.Group -> "group-header"
        is ClickGuiSettingItem.Entry -> entryTemplate(setting.entry)
        null -> "entry-row"
    }

internal fun entryTemplate(entry: ConfigEntry<*>): String =
    when (entry) {
        is BooleanEntry -> "boolean-entry"
        is ButtonEntry -> "button-entry"
        is HudEditEntry -> "button-entry"
        is EnumEntry<*> -> "enum-entry"
        is KeybindEntry -> "keybind-entry"
        is StringEntry -> "string-entry"
        is ColorEntry -> "color-entry"
        is ServerMappingEntry -> "server-mapping-entry"
        is ItemListEntry -> "item-list-entry"
        is ModuleListEntry -> "module-list-entry"
        is IntEntry -> if (entry.min != Int.MIN_VALUE && entry.max != Int.MAX_VALUE) "number-entry" else "entry-row"
        is FloatEntry -> if (entry.min != -Float.MAX_VALUE && entry.max != Float.MAX_VALUE) "number-entry" else "entry-row"
        is DoubleEntry -> if (entry.min != -Double.MAX_VALUE && entry.max != Double.MAX_VALUE) "number-entry" else "entry-row"
        is IntRangeEntry -> "range-entry"
        is FloatRangeEntry -> "range-entry"
        else -> "entry-row"
    }

internal fun entryValue(entry: ConfigEntry<*>): String =
    entryValue(entry, null)

internal fun entryValue(entry: ConfigEntry<*>, layout: ClickGuiLayoutContext?): String =
    when (entry) {
        is BooleanEntry -> if (entry.value) "On" else "Off"
        is IntEntry -> entry.value.toString()
        is FloatEntry -> "%.2f".format(entry.value)
        is DoubleEntry -> "%.2f".format(entry.value)
        is StringEntry -> if (layout?.activeStringEntry === entry) layout.activeStringText else entry.value
        is ColorEntry -> "#%02X%02X%02X%02X".format(entry.value.a, entry.value.r, entry.value.g, entry.value.b)
        is KeybindEntry -> if (layout?.listeningKeybind === entry) "..." else keyName(entry.value)
        is EnumEntry<*> -> label(entry.value.name)
        is IntRangeEntry -> "${entry.value.first} - ${entry.value.second}"
        is FloatRangeEntry -> "%.${entry.decimals}f - %.${entry.decimals}f".format(entry.value.first, entry.value.second)
        is ButtonEntry -> entry.label
        is HudEditEntry -> "Edit Position"
        is ItemListEntry -> "${entry.value.size}"
        is ModuleListEntry -> "${entry.value.size}"
        else -> entry.value.toString()
    }

internal fun entryProgress(entry: ConfigEntry<*>): String {
    val progress = when (entry) {
        is IntEntry -> (entry.value - entry.min).toFloat() / (entry.max - entry.min).coerceAtLeast(1).toFloat()
        is FloatEntry -> (entry.value - entry.min) / (entry.max - entry.min).coerceAtLeast(0.0001f)
        is DoubleEntry -> ((entry.value - entry.min) / (entry.max - entry.min).coerceAtLeast(0.0001)).toFloat()
        else -> 0f
    }
    return progress.coerceIn(0f, 1f).toString()
}

internal fun entryLowProgress(entry: ConfigEntry<*>): String {
    val progress = when (entry) {
        is IntRangeEntry -> (entry.value.first - entry.min).toFloat() / (entry.max - entry.min).coerceAtLeast(1).toFloat()
        is FloatRangeEntry -> (entry.value.first - entry.min) / (entry.max - entry.min).coerceAtLeast(0.0001f)
        else -> 0f
    }
    return progress.coerceIn(0f, 1f).toString()
}

internal fun entryHighProgress(entry: ConfigEntry<*>): String {
    val progress = when (entry) {
        is IntRangeEntry -> (entry.value.second - entry.min).toFloat() / (entry.max - entry.min).coerceAtLeast(1).toFloat()
        is FloatRangeEntry -> (entry.value.second - entry.min) / (entry.max - entry.min).coerceAtLeast(0.0001f)
        else -> 1f
    }
    return progress.coerceIn(0f, 1f).toString()
}

internal fun entryColor(entry: ConfigEntry<*>): String =
    when (entry) {
        is ColorEntry -> {
            val color = entry.liveColor(entry.value)
            "#%02X%02X%02X%02X".format(color.a, color.r, color.g, color.b)
        }
        else -> "#FFFFFFFF"
    }

internal fun keyName(keyCode: Int): String {
    if (KeybindEntry.isMouseButton(keyCode)) {
        return when (val button = KeybindEntry.mouseButton(keyCode)) {
            GLFW.GLFW_MOUSE_BUTTON_LEFT -> "Mouse 1"
            GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "Mouse 2"
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "Mouse 3"
            else -> "Mouse ${button + 1}"
        }
    }
    return when (keyCode) {
    GLFW.GLFW_KEY_UNKNOWN       -> "None"
    GLFW.GLFW_KEY_SPACE         -> "Space"
    GLFW.GLFW_KEY_ESCAPE        -> "Esc"
    GLFW.GLFW_KEY_ENTER,
    GLFW.GLFW_KEY_KP_ENTER      -> "Enter"
    GLFW.GLFW_KEY_BACKSPACE     -> "Back"
    GLFW.GLFW_KEY_TAB           -> "Tab"
    GLFW.GLFW_KEY_INSERT        -> "Insert"
    GLFW.GLFW_KEY_DELETE        -> "Del"
    GLFW.GLFW_KEY_HOME          -> "Home"
    GLFW.GLFW_KEY_END           -> "End"
    GLFW.GLFW_KEY_PAGE_UP       -> "PgUp"
    GLFW.GLFW_KEY_PAGE_DOWN     -> "PgDn"
    GLFW.GLFW_KEY_LEFT_SHIFT    -> "LShift"
    GLFW.GLFW_KEY_RIGHT_SHIFT   -> "RShift"
    GLFW.GLFW_KEY_LEFT_CONTROL,
    GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl"
    GLFW.GLFW_KEY_LEFT_ALT,
    GLFW.GLFW_KEY_RIGHT_ALT     -> "Alt"
    else -> GLFW.glfwGetKeyName(keyCode, 0)?.uppercase() ?: "K$keyCode"
    }
}
