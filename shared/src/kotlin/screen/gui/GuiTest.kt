package screen.gui

import screen.ScreenLayer

object GuiTest : Gui(ScreenLayer.OVERLAY) {
    init {
        define {
            dimensions = Dimensions.Exact(100, 100)
            background {
                dimensions = Dimensions.MatchParent
                makeResizable()
            }
        }
    }
}