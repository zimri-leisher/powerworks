package screen.gui2

import level.block.MinerBlock
import screen.mouse.Mouse

class GuiMinerBlock(block: MinerBlock) : Gui(ScreenLayer.WINDOWS), PoolableGui {

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
            onOpen {
                placement = Placement.Exact(Mouse.xPixel, Mouse.yPixel - heightPixels)
                gui.layout.recalculateExactPlacement(this)
            }
            background {
                dimensions = Dimensions.FitChildren.pad(4, 4)
                list(Placement.Align.Center) {
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