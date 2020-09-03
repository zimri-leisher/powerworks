package screen.gui

import level.block.MinerBlock
import screen.ScreenLayer
import screen.element.ElementProgressBar
import screen.element.ElementResourceContainer

class GuiMinerBlock(block: MinerBlock) : Gui(ScreenLayer.MENU), PoolableGui {

    var block = block
        set(value) {
            if (field != value) {
                field = value
                progressBar.maxProgress = field.type.maxWork
                resourceContainer.container = field.output
            }
        }
    lateinit var progressBar: ElementProgressBar
    lateinit var resourceContainer: ElementResourceContainer

    init {
        define {
            openAtMouse()
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 9)
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                text("Miner", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1)) { makeDraggable() }
                list(Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2)) {
                    resourceContainer = resourceContainerView(block.output, 1, 1)
                    progressBar = progressBar(block.type.maxWork, { this@GuiMinerBlock.block.currentWork })
                }
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
            }
        }
    }

    override fun canDisplay(obj: Any?) = obj is MinerBlock

    override fun display(obj: Any?) {
        block = obj as MinerBlock
    }

    override fun isDisplaying(obj: Any?) = obj == block

}