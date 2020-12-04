package screen.attribute

import resource.ResourceContainer
import screen.ScreenManager
import screen.gui.GuiElement

class AttributeResourceContainerLink(element: GuiElement, container: ResourceContainer) : Attribute(element) {
    var container = container
        set(value) {
            field = value
            ScreenManager.resourceContainerDisplays[element.gui] = field
        }

    init {
        ScreenManager.resourceContainerDisplays[element.gui] = container
    }
}