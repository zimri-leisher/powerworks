package screen

import level.block.Block
import screen.elements.BlockGUI
import screen.elements.GUIWindow

// this should 100 percent use generics to determine the block class. but it doesn't. because i can't figure it out. it almost works,
// but some block types just cant use it. who knows

// TODO make it abstract for any gui you want so we can have, for example, inv guis being pooled
/**
 * A class that stores instances of GUIs for blocks. They are instantiated on demand until they reach the [maxGUICount],
 * and then the previously instantiated GUIs start being repurposed to display new blocks using [BlockGUI.displayBlock]
 */
class BlockGUIPool<W>(val guiTemplate: (block: Block) -> W, val maxGUICount: Int = 5) where W : GUIWindow, W : BlockGUI {
    var currentInstances = mutableMapOf<W, MutableList<Block>>()

    /**
     * Opens a [BlockGUI] window that corresponds to the given [block].
     * * If the size of [currentInstances] is less than [maxGUICount], it will instantiate a new one using the [guiTemplate]
     * lambda.
     * * If the size of [currentInstances] is equal to [maxGUICount], it will either repurpose an eligible GUI by calling
     * [BlockGUI.displayBlock] (eligibility determined by [BlockGUI.canDisplayBlock]), or create a new one and delete an
     * old one if there are no eligible old GUIs
     */
    fun open(block: Block) {
        val previouslyUsedGUI = currentInstances.entries.sortedBy { it.value.size }.firstOrNull { block in it.value }?.key
        if(previouslyUsedGUI != null) {
            previouslyUsedGUI.open = false
            previouslyUsedGUI.displayBlock(block)
            previouslyUsedGUI.open = true
        } else {
            // needs to be assigned one
            if (currentInstances.size < maxGUICount) {
                // can have a new one
                val newInstance = guiTemplate(block)
                currentInstances.put(newInstance, mutableListOf(block))
                newInstance.open = true
            } else {
                // can't have a new one
                // case one: there are no guis already created that are able to display this block
                if (currentInstances.none { it.key.canDisplayBlock(block) }) {
                    // remove the least used one
                    currentInstances.remove(currentInstances.entries.sortedBy { it.value.size }.first().key)
                    val newInstance = guiTemplate(block)
                    currentInstances.put(newInstance, mutableListOf(block))
                    newInstance.open = true
                } else {
                    // two: there is a gui created that is capable of displaying the block
                    // find the least used one
                    val guiToUse = currentInstances.entries.sortedBy { it.value.size }.first { it.key.canDisplayBlock(block) }.apply { value.add(block) }.key
                    guiToUse.open = false
                    guiToUse.displayBlock(block)
                    guiToUse.open = true
                }
            }
        }
    }

    /**
     * Closes the GUI that is currently displaying [block]. Does nothing if none are displaying it
     */
    fun close(block: Block) {
        currentInstances.entries.firstOrNull { it.key.isDisplayingBlock(block) }?.key?.open = false
    }

    /**
     * Toggles the GUI that is currently displaying [block]. If none are displaying it, it [open]s a new window
     */
    fun toggle(block: Block) {
        val currentWindow = currentInstances.entries.firstOrNull { it.key.isDisplayingBlock(block) }?.key
        if(currentWindow != null) {
            currentWindow.toggle()
        } else {
            open(block)
        }
    }
}