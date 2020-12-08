package screen.gui

import level.block.ArmoryBlock
import screen.ScreenLayer
import screen.element.ElementProgressBar

class GuiArmoryBlock(block: ArmoryBlock) : Gui(ScreenLayer.MENU_1), PoolableGui {
    var block = block
        set(value) {
            if (field != value) {
                field = value
                progressBar.maxProgress = block.type.maxWork
            }
        }

    lateinit var progressBar: ElementProgressBar

    init {
        define {
            openAtCenter(0)
            keepInsideScreen()
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 9)
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                text("Armory", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1))
                list(Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2)) {
                    progressBar = progressBar(block.type.maxWork, { this@GuiArmoryBlock.block.currentWork })
                }
            }
        }
    }

    override fun canDisplay(obj: Any?) = obj is ArmoryBlock

    override fun display(obj: Any?) {
        block = obj as ArmoryBlock
    }

    override fun isDisplaying(obj: Any?) = block == obj
}