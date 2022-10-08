package screen.gui

import level.block.FurnaceBlock
import screen.ScreenLayer
import screen.attribute.AttributeResourceContainerLink
import screen.element.ElementFluidTank
import screen.element.ElementProgressBar
import screen.element.ElementResourceContainer

class GuiFurnaceBlock(block: FurnaceBlock) : Gui(ScreenLayer.MENU_1), PoolableGui {
    var block = block
        set(value) {
            if (field != value) {
                field = value
                input.container = value.input
                progressBar.maxProgress = value.type.maxWork
                output.tank = value.output
                containerLink.container = value.input
            }
        }

    lateinit var input: ElementResourceContainer
    lateinit var progressBar: ElementProgressBar
    lateinit var output: ElementFluidTank
    lateinit var containerLink: AttributeResourceContainerLink

    init {
        define {
            openAtCenter(0)
            openWithBrainInventory()
            keepInsideScreen()
            containerLink = linkToContainer(block.input)
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 9)
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                text("Furnace", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1)) { makeDraggable() }
                list(Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2)) {
                    input = inventory(block.input)
                    progressBar = progressBar(block.type.maxWork, { this@GuiFurnaceBlock.block.currentWork })
                    output = fluidTank(block.output)
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