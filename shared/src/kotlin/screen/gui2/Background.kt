package screen.gui2

object Background : Gui(ScreenLayer.BACKGROUND, {
    placement = Placement.Origin
    dimensions = Dimensions.Fullscreen
}) {
    val backgroundElement = parentElement
}