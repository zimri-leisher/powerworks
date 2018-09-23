package level.block

import crafting.Crafter
import crafting.Recipe
import io.*
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import resource.ResourceType
import screen.CrafterBlockGUI

class CrafterBlock(override val type: CrafterBlockType, xTile: Int, yTile: Int, rotation: Int) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener, Crafter {

    override val crafterType: Int
        get() = type.craftingType

    val crafterGUI = CrafterBlockGUI(this)
    var recipe: Recipe? = null

    private var currentResources = ResourceList()

    init {
        containers.forEach { container ->
            container.listeners.add(this)
            // only allow input if there is a recipe
            container.typeRule = { recipe != null }
            // only allow addition if there are less ingredients than required
            container.additionRule = { resource, quantity -> recipe != null && resource in recipe!!.consume && container.getQuantity(resource) + quantity <= recipe!!.consume.getQuantity(resource) }
        }
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
        // basically, refresh the current resource list
        currentResources.clear()
        currentResources = containers.toList()
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        if (quantity < 0) {
            currentResources.remove(resource, -quantity)
        } else if (quantity > 0) {
            currentResources.add(resource, quantity)
        }
        val canCraft = canCraft()
        if (on && !canCraft) {
            currentWork = 0
        }
        on = canCraft
    }

    fun canCraft() = recipe?.consume?.enoughIn(currentResources) == true

    override fun onFinishWork() {
        if (nodes.canOutput(recipe!!.produce)) {
            if (containers.remove(recipe!!.consume))
                nodes.output(recipe!!.produce, mustContainEnough = false)
        } else {
            currentWork = type.maxWork
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            crafterGUI.toggle()
        }
    }
}