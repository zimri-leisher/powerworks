package screen.gui

import level.block.SolidifierBlock
import screen.ScreenLayer
import screen.attribute.AttributeResourceContainerLink
import screen.element.ElementFluidTank
import screen.element.ElementProgressBar
import screen.element.ElementResourceContainer

class GuiSolidifierBlock(block: SolidifierBlock) : Gui(ScreenLayer.MENU_1), PoolableGui {
    var block = block
        set(value) {
            if (field != value) {
                field = value
                inputTank.tank = value.input
                outputInventory.container = value.output
            }
        }

    lateinit var inputTank: ElementFluidTank
    lateinit var outputInventory: ElementResourceContainer
    lateinit var progressArrow: ElementProgressBar
    lateinit var containerLink: AttributeResourceContainerLink

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
                    inputTank = fluidTank(block.input)
                    progressArrow = progressBar(block.type.maxWork, { this@GuiSolidifierBlock.block.currentWork })
                    outputInventory = inventory(block.output)
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