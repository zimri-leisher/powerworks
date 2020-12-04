package screen.gui

import level.entity.robot.BrainRobot
import screen.ScreenLayer

class GuiBrainRobot(val brainRobot: BrainRobot) : Gui(ScreenLayer.MENU) {

    init {
        define {
            linkToContainer(brainRobot.inventory)
            openAtMouse()
            keepInsideScreen()
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 9)
                inventory(brainRobot.inventory, Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2))
                text("${brainRobot.user.displayName}'s BRAIN", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1)) { makeDraggable() }
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
            }
        }
    }
}