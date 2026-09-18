package onl.luka.grizzly.module

import com.google.gson.JsonObject
import onl.luka.grizzly.config.entry.KeybindEntry

/**
 * One saved configuration of a module, with its own key. A module always has at least the base
 * profile; extra ones hold a full copy of every setting, so binding a key to each gives you
 * "G for normal, H for telly" without the two fighting over the same values.
 *
 * [values] is only the snapshot taken when the profile was last left. Whichever profile is
 * active reads and writes the module's live entries instead, so editing in the GUI edits the
 * profile you are currently on.
 */
class ModuleProfile(
    var name: String,
    val keybind: KeybindEntry,
    var values: JsonObject = JsonObject(),
)
