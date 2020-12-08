package screen.gui

import level.block.FurnaceBlock
import screen.ScreenLayer
import screen.element.ElementFluidTank
import screen.element.ElementProgressBar
import screen.element.ElementResourceContainer

class GuiFurnaceBlock(block: FurnaceBlock) : Gui(ScreenLayer.MENU_1), PoolableGui {
    var block = block
        set(value) {
            if (field != value) {
                field = value
                input.container = value.queue
                progressBar.maxProgress = value.type.maxWork
                output.tank = value.tank
            }
        }

    lateinit var input: ElementResourceContainer
    lateinit var progressBar: ElementProgressBar
    lateinit var output: ElementFluidTank

    init {
        define {
            openAtCenter(0)
            keepInsideScreen()
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 9)
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                text("Furnace", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1)) { makeDraggable() }
                list(Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2)) {
                    input = inventory(block.queue)
                    progressBar = progressBar(block.type.maxWork, { this@GuiFurnaceBlock.block.currentWork })
                    output = fluidTank(block.tank)
                }
            }
        }
    }

    override fun canDisplay(obj: Any?) = obj is FurnaceBlock

    override fun display(obj: Any?) {
        block = obj as FurnaceBlock
    }

    override fun isDisplaying(obj: Any?) = obj == block
}