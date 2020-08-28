package screen.gui2

import item.Inventory
import level.entity.robot.BrainRobot
import screen.mouse.Mouse

class GuiBrainRobot(val brainRobot: BrainRobot) : Gui(ScreenLayer.WINDOWS) {
    init {
        define {
            onOpen {
                placement = Placement.Exact(Mouse.xPixel, Mouse.yPixel - heightPixels)
                gui.layout.recalculateExactPlacement(this)
            }
            background {
                dimensions = Dimensions.FitChildren.pad(4, 9)
                inventory(brainRobot.inventory, Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2))
                text("${brainRobot.user.displayName}'s BRAIN", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1))
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
            }
        }
    }
}