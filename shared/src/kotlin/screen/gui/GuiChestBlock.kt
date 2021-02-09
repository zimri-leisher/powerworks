package screen.gui

import level.block.ChestBlock
import screen.ScreenLayer
import screen.attribute.AttributeResourceContainerLink
import screen.element.ElementResourceContainer

class GuiChestBlock(val block: ChestBlock) : Gui(ScreenLayer.MENU_1), PoolableGui {

    lateinit var inventoryView: ElementResourceContainer
    lateinit var containerLink: AttributeResourceContainerLink

    init {
        define {
            containerLink = linkToContainer(block.inventory)
            openAtCenter(0)
            openWithBrainInventory()
            keepInsideScreen()
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 9)
                inventoryView = inventory(block.inventory, Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2))
                text("Chest", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1)) { makeDraggable() }
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
            }
        }
    }

    override fun canDisplay(obj: Any?) = obj is ChestBlock

    override fun display(obj: Any?) {
        obj as ChestBlock
        inventoryView.container = obj.inventory
        containerLink.container = obj.inventory
    }

    override fun isDisplaying(obj: Any?) = obj == block
}