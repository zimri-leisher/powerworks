package screen.elements

import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType

class GUIResourceContainerDisplay(parent: RootGUIElement, name: String, val container: ResourceContainer<*>,
                                  xAlignment: () -> Int, yAlignment: () -> Int, val width: Int, val height: Int,
                                  open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { width * 16 + 4 }, { height * 16 + 4 }, open, layer), ResourceContainerChangeListener {

    val listDisplay = GUIResourceListDisplay(this, name + " list display", container.toList(), { 0 }, { 0 }, width, height)

    init {
        container.listeners.add(this)
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
        listDisplay.currentResources.clear()
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        if (quantity < 0) {
            listDisplay.currentResources.remove(resource, -quantity)
        } else if (quantity > 0) {
            listDisplay.currentResources.add(resource, quantity)
        }
    }
}