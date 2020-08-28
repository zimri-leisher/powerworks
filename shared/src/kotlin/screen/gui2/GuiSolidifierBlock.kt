package screen.gui2

import level.block.SolidifierBlock
import main.toColor
import screen.mouse.Mouse

class GuiSolidifierBlock(block: SolidifierBlock) : Gui(ScreenLayer.WINDOWS), PoolableGui {
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
            onOpen {
                placement = Placement.Exact(Mouse.xPixel, Mouse.yPixel - heightPixels)
                gui.layout.recalculateExactPlacement(this)
            }
            background {
                dimensions = Dimensions.FitChildren.pad(4, 10)
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                text("Solidifier", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1))
                list(Placement.Align.Center.offset(0, -3), padding = 1) {
                    inputTank = fluidTank(block.tank)
                    progressArrow = progressBar(block.currentWork, {this@GuiSolidifierBlock.block.type.maxWork})
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