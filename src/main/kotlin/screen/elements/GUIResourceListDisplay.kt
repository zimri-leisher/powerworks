package screen.elements

import resource.ResourceList

class GUIResourceListDisplay(parent: RootGUIElement, name: String, currentResources: ResourceList,
                             xAlignment: Alignment, yAlignment: Alignment, width: Int, height: Int,
                             open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { width * 16 + 4 }, { height * 16 + 4 }, open, layer) {

    var width = width
        set(value) {
            if (field != value) {
                children.clear()
                field = value
                createSlots()
                alignments.updateDimension()
            }
        }
    var height = height
        set(value) {
            if (field != value) {
                // TODO worry about the memory leakage here - whats going on when children gets cleared?
                children.clear()
                field = value
                createSlots()
                alignments.updateDimension()
            }
        }
    var currentResources = currentResources
        set(value) {
            field = value
            slots.forEach { it.list = value }
        }
    private val slots = mutableListOf<GUIResourceDisplaySlot>()

    init {
        alignments.width = { this.width * 16 + 4 }
        alignments.height = { this.height * 16 + 4 }
        createSlots()
    }

    private fun createSlots() {
        var i = 0
        for (x in 0 until width) {
            for (y in 0 until height) {
                GUIResourceDisplaySlot(this, name + " slot $i", x * GUIResourceDisplaySlot.WIDTH + 2, y * GUIResourceDisplaySlot.HEIGHT + 2, currentResources, i, open, layer + 1).apply { slots.add(this) }
                i++
            }
        }
    }
}