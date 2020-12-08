package screen.attribute

import main.Game
import screen.element.ElementGuiHolder
import screen.gui.*

class AttributeOpenAtCenter(element: GuiElement, horizontalAlign: HorizontalAlign = HorizontalAlign.CENTER,
                            verticalAlign: VerticalAlign = VerticalAlign.CENTER, val order: Int = 0) : Attribute(element) {

    val guiHolder = ElementGuiHolder(GuiIngame.CenterMenuGroup.centerGroup, element.gui).apply { placement = Placement.VerticalList(0); open = false }

    init {
        val group = GuiIngame.CenterMenuGroup.centerGroup
        with(element.gui.parentElement) {
            eventListeners.add(GuiOpenListener {
                if(group.height + height > Game.HEIGHT) {
                    open = false
                    // dont allow opening guis if it would go past the border
                    return@GuiOpenListener
                }
                var index = 0
                for (existingChild in group.children) {
                    existingChild as ElementGuiHolder
                    val order = existingChild.guiToHold.parentElement.attributes.filterIsInstance<AttributeOpenAtCenter>().first().order
                    if (order > this@AttributeOpenAtCenter.order) {
                        break
                    }
                    index++
                }
                guiHolder.open = true
                group.add(guiHolder, index)
            })
            eventListeners.add(GuiCloseListener {
                guiHolder.open = false
                group.remove(guiHolder)
            })
        }
    }
}