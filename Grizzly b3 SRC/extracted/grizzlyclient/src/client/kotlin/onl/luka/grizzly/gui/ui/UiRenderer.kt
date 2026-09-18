package onl.luka.grizzly.gui.ui

interface UiRenderer {
    val fontHeight: Float

    fun fontHeight(font: String?): Float = fontHeight
    fun textWidth(text: String): Float
    fun textWidth(text: String, font: String?): Float = textWidth(text)
    fun fill(rect: UiRect, color: Int, radius: Float = 0f)
    fun roundedFill(rect: UiRect, color: Int, radius: Float = 0f, corners: Int = 15) =
        fill(rect, color, radius)
    fun gradient(rect: UiRect, from: Int, to: Int) = fill(rect, from)
    fun border(rect: UiRect, color: Int, width: Float, radius: Float = 0f)
    fun text(text: String, x: Float, y: Float, color: Int)
    fun text(text: String, x: Float, y: Float, color: Int, font: String?) = text(text, x, y, color)
    fun text(
        text: String,
        x: Float,
        y: Float,
        color: Int,
        font: String?,
        shadow: Boolean,
        scale: Float,
    ) = text(text, x, y, color, font)
    /** Frosts whatever is already on screen behind the rect. No-op where blur is unavailable. */
    fun blur(rect: UiRect, radius: Float = 0f, tint: Int = 0, strength: Int = 6, corners: Int = 15) {}

    fun shadow(rect: UiRect, radius: Float = 0f, spread: Float = 6f, color: Int = 0x66000000) {}

    fun item(name: String, rect: UiRect) {}
    fun colorMap(rect: UiRect, hue: Float) {}
    fun clip(rect: UiRect)
    fun unclip()
}
