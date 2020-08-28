package screen.gui2

import level.block.ChestBlock

class GuiChestBlock(val block: ChestBlock) : Gui(ScreenLayer.WINDOWS), PoolableGui {

    lateinit var inventoryView: ElementInventory

    init {
        define {
            background {
                dimensions = Dimensions.FitChildren.pad(4, 9)
                inventoryView = inventory(block.inventory, Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2))
                text("Chest inventory", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1))
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
            }
        }
    }

    override fun canDisplay(obj: Any?) = obj is ChestBlock

    override fun display(obj: Any?) {
        obj as ChestBlock
        inventoryView.container = obj.inventory
    }

    override fun isDisplaying(obj: Any?) = obj == block
}