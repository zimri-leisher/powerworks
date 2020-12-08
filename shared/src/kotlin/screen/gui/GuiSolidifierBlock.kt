package screen.gui

import level.block.SolidifierBlock
import screen.ScreenLayer
import screen.element.ElementFluidTank
import screen.element.ElementInventory
import screen.element.ElementProgressBar

class GuiSolidifierBlock(block: SolidifierBlock) : Gui(ScreenLayer.MENU_1), PoolableGui {
    var block = block
        set(value) {
            if (field != value) {
                field = value
                inputTank.tank = value.tank
                outputInventory.container = value.out
            }
        }

    lateinit var inputTank: ElementFluidTank
    lateinit var outputInventory: ElementInventory
    lateinit var progressArrow: ElementProgressBar

    init {
        define {
            openAtCenter(0)
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 10)
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                text("Solidifier", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1)) { makeDraggable() }
                list(Placement.Align.Center.offset(0, -3), padding = 1) {
                    makeDraggable()
                    inputTank = fluidTank(block.tank)
                    progressArrow = progressBar(block.type.maxWork, { this@GuiSolidifierBlock.block.currentWork })
                    outputInventory = inventory(block.out)
                }
            }
        }
    }

    override fun canDisplay(obj: Any?) = obj is SolidifierBlock

    override fun display(obj: Any?) {
        block = obj as SolidifierBlock
    }

    override fun isDisplaying(obj: Any?) = obj == block

}