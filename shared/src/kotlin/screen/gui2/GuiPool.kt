package screen.gui2

import screen.elements.BlockGUI

// this should 100 percent use generics to determine the block class. but it doesn't. because i can't figure it out. it almost works,
// but some block types just cant use it. who knows

/**
 * A class that stores instances of GUIs for blocks. They are instantiated on demand until they reach the [poolSize],
 * and then the previously instantiated GUIs start being repurposed to display new blocks using [PoolableGui.display]
 */
class GuiPool<W>(val newGuiFor: (any: Any?) -> W, val poolSize: Int = 5) where W : Gui, W : PoolableGui {
    var currentInstances = mutableMapOf<W, MutableList<Any?>>()

    /**
     * Opens a [BlockGUI] window that corresponds to the given [obj].
     * * If the size of [currentInstances] is less than [poolSize], it will instantiate a new one using the [newGuiFor]
     * lambda.
     * * If the size of [currentInstances] is equal to [poolSize], it will either repurpose an eligible GUI by calling
     * [BlockGUI.displayBlock] (eligibility determined by [BlockGUI.canDisplayBlock]), or create a new one and delete an
     * old one if there are no eligible old GUIs
     */
    fun open(obj: Any?) {
        val previouslyUsedGUI = currentInstances.entries.sortedBy { it.value.size }.firstOrNull { obj in it.value }?.key
        if(previouslyUsedGUI != null) {
            previouslyUsedGUI.open = false
            previouslyUsedGUI.display(obj)
            previouslyUsedGUI.open = true
        } else {
            // needs to be assigned one
            if (currentInstances.size < poolSize) {
                // can have a new one
                val newInstance = newGuiFor(obj)
                currentInstances.put(newInstance, mutableListOf(obj))
                newInstance.open = true
            } else {
                // can't have a new one
                // case one: there are no guis already created that are able to display this block
                if (currentInstances.none { it.key.canDisplay(obj) }) {
                    // remove the least used one
                    currentInstances.remove(currentInstances.entries.sortedBy { it.value.size }.first().key)
                    val newInstance = newGuiFor(obj)
                    newInstance.open = false
                    currentInstances.put(newInstance, mutableListOf(obj))
                    newInstance.open = true
                } else {
                    // two: there is a gui created that is capable of displaying the block
                    // find the least used one
                    val guiToUse = currentInstances.entries.sortedBy { it.value.size }.first { it.key.canDisplay(obj) }.apply { value.add(obj) }.key
                    guiToUse.open = false
                    guiToUse.display(obj)
                    guiToUse.open = true
                }
            }
        }
    }

    /**
     * Closes the GUI that is currently displaying [obj]. Does nothing if none are displaying it
     */
    fun close(obj: Any?) {
        currentInstances.entries.firstOrNull { it.key.isDisplaying(obj) }?.key?.open = false
    }

    /**
     * Toggles the GUI that is currently displaying [obj]. If none are displaying it, it [open]s a new window
     */
    fun toggle(obj: Any?) {
        val currentWindow = currentInstances.entries.firstOrNull { it.key.isDisplaying(obj) }?.key
        if(currentWindow != null) {
            currentWindow.open = !currentWindow.open
        } else {
            open(obj)
        }
    }
}