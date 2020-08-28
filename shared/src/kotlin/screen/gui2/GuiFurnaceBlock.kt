package screen.gui2

import level.block.FurnaceBlock

class GuiFurnaceBlock(block: FurnaceBlock) : Gui(ScreenLayer.WINDOWS), PoolableGui {
    val block = block

    init {
        define {

        }
    }

    override fun canDisplay(obj: Any?) = obj is FurnaceBlock

    override fun display(obj: Any?) {
    }

    override fun isDisplaying(obj: Any?) = obj == block
}