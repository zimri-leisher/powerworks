package misc

import java.util.*

private typealias AwtColor = java.awt.Color

enum class Color(val awtColor: AwtColor) {
    RED(AwtColor.RED), GREEN(AwtColor.GREEN), BLUE(AwtColor.BLUE), BLACK(AwtColor.BLACK), YELLOW(AwtColor.YELLOW),
    GRAY(AwtColor.GRAY), LIGHT_GRAY(AwtColor.LIGHT_GRAY), DARK_GRAY(AwtColor.DARK_GRAY);

    companion object {

        fun toColor(name: String): java.awt.Color? {
            val ret = values().firstOrNull {
                it.name.lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault())
                        || it.name.lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault())
                    .replace(" ", "_")
            }
            return ret?.awtColor
        }
    }
}