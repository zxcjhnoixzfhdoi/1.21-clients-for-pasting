package onl.luka.grizzly.gui.ui

import onl.luka.grizzly.config.ConfigGroup
import onl.luka.grizzly.config.entry.ColorEntry
import onl.luka.grizzly.config.entry.ConfigEntry
import onl.luka.grizzly.config.entry.EnumEntry
import onl.luka.grizzly.config.entry.ItemListEntry
import onl.luka.grizzly.config.entry.KeybindEntry
import onl.luka.grizzly.config.entry.ModuleListEntry
import onl.luka.grizzly.config.entry.StringEntry
import onl.luka.grizzly.module.ModuleProfile
import onl.luka.grizzly.module.Module

data class ClickGuiLayoutContext(
    val categoryPositions: Map<Module.Category, Pair<Int, Int>>,
    val categoryOrder: List<Module.Category> = Module.Category.entries,
    val expandedModules: Set<Module>,
    val collapsedCategories: Set<Module.Category>,
    val selectedCategory: Module.Category? = null,
    val detailModule: Module? = null,
    val configX: Int = 10,
    val configY: Int = 30,
    val configCollapsed: Boolean = true,
    val presetName: String = "",
    val presetActive: Boolean = false,
    val presetCursor: Int = 0,
    val presetScrollPx: Int = 0,
    val presets: List<String> = emptyList(),
    val dropdownScroll: Float = 0f,
    val activeStringEntry: StringEntry? = null,
    /** Shares the string entry's text/cursor/scroll fields; only one field is ever focused. */
    val editingProfile: ModuleProfile? = null,
    val activeStringText: String = "",
    val activeStringCursor: Int = 0,
    val activeStringScrollPx: Int = 0,
    val listeningKeybind: KeybindEntry? = null,
    val openEnumEntry: EnumEntry<*>? = null,
    val openColorEntry: ColorEntry? = null,
    val colorModeOpen: Boolean = false,
    val colorEditingChannel: Int? = null,
    val colorEditingHex: Boolean = false,
    val colorEditingText: String = "",
    val openItemListEntry: ItemListEntry? = null,
    val openModuleListEntry: ModuleListEntry? = null,
    val itemPicker: ItemPickerState? = null,
    val modulePicker: ModulePickerState? = null,
    val metaPreset: String? = null,
    val meta: onl.luka.grizzly.config.PresetMetadata? = null,
    val metaActiveField: String = "",
    val metaText: String = "",
    val metaCursor: Int = 0,
    val metaScrollPx: Int = 0,
    val includeEntry: (Module, ConfigEntry<*>) -> Boolean,
)



/**
 * The picker rows are worked out by the screen, which owns the search text and the item
 * registry lookups, and handed over ready to draw. The blueprint only lays them out.
 */
data class ItemPickerRow(val id: String, val label: String, val icon: String, val added: Boolean)

data class ModulePickerRow(val name: String, val hidden: Boolean)

data class ModulePickerState(
    val header: String,
    val search: String,
    val searchActive: Boolean,
    val searchCursor: Int,
    val searchScroll: Int,
    val rows: List<ModulePickerRow>,
    val rowScroll: Int,
    val listHeight: Int,
    val leadSpace: Int,
    val tailSpace: Int,
)

data class ItemPickerState(
    val header: String,
    val search: String,
    val searchActive: Boolean,
    val searchCursor: Int,
    val searchScroll: Int,
    /** Only the rows inside the visible window; the spacers stand in for the rest. */
    val rows: List<ItemPickerRow>,
    val added: List<Pair<String, String>>,
    val rowScroll: Int,
    val addedScroll: Int,
    val listHeight: Int,
    val leadSpace: Int,
    val tailSpace: Int,
)

internal sealed interface ClickGuiSettingItem {
    data class Group(val group: ConfigGroup) : ClickGuiSettingItem
    data class Entry(val module: Module, val entry: ConfigEntry<*>) : ClickGuiSettingItem
}
