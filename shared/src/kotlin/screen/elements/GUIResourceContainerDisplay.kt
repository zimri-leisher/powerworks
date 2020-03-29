package screen.elements

import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType

class GUIResourceContainerDisplay(parent: RootGUIElement, name: String,
                                  xAlignment: Alignment, yAlignment: Alignment, width: Int, height: Int,
                                  container: ResourceContainer,
                                  open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { width * 16 }, { height * 16 }, open, layer), ResourceContainerChangeListener {

    var container = container
        set(value) {
            if(field != value) {
                field.listeners.remove(this)
                field = value
                value.listeners.add(this)
                listDisplay.currentResources = value.resourceList()
            }
        }
    var width = width
        set(value) {
            if (field != value) {
                field = value
                listDisplay.width = value
            }
        }
    var height = height
        set(value) {
            if (field != value) {
                field = value
                listDisplay.height = value
            }
        }

    val listDisplay = GUIResourceListDisplay(this, name + " list display", container.resourceList(), { 0 }, { 0 }, width, height)

    init {
        container.listeners.add(this)
    }

    override fun onContainerClear(container: ResourceContainer) {
        listDisplay.currentResources.clear()
    }

    override fun onContainerChange(container: ResourceContainer, resource: ResourceType, quantity: Int) {
        if (quantity < 0) {
            listDisplay.currentResources.remove(resource, -quantity)
        } else if (quantity > 0) {
            listDisplay.currentResources.add(resource, quantity)
        }
    }
}