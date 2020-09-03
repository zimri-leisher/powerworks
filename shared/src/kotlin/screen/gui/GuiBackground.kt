package screen.gui

import screen.ScreenLayer

object GuiBackground : Gui(ScreenLayer.BACKGROUND, {
    placement = Placement.Origin
    dimensions = Dimensions.Fullscreen
}) {
    val backgroundElement = parentElement
    init {
        open = true
    }
}