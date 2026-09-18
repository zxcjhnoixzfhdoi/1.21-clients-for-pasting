package onl.luka.grizzly.config.entry

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

class EnumEntry<T : Enum<T>>(
    name: String,
    default: T,
    private val enumClass: Class<T>
) : ConfigEntry<T>(name, default) {

    /** Optional dynamic filter used by controls with conditionally valid choices. */
    var optionFilter: ((T) -> Boolean)? = null

    /** Constants currently available to the user. */
    val constants: List<T>
        get() {
            val filter = optionFilter ?: return enumClass.enumConstants.toList()
            return enumClass.enumConstants.filter(filter)
        }

    /** Set the value to the constant at the given index. */
    fun setByIndex(index: Int) {
        val consts = constants
        if (index in consts.indices) value = consts[index]
    }

    /** Advance to the next enum constant, wrapping around. */
    fun cycle() {
        val consts = constants
        if (consts.isEmpty()) return
        val idx = consts.indexOf(value)
        value = consts[(idx.coerceAtLeast(-1) + 1) % consts.size]
    }

    override fun toJson(): JsonElement = JsonPrimitive(value.name)
    override fun fromJson(element: JsonElement) {
        value = enumClass.enumConstants.firstOrNull { it.name == element.stringOrNull } ?: defaultValue
    }
}
