package level.block

import crafting.Crafter
import crafting.Recipe
import io.*
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import resource.ResourceType
import screen.CraftingBlockGUI

class CrafterBlock(override val type: CrafterBlockType, xTile: Int, yTile: Int, rotation: Int) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener, ControlPressHandler, Crafter {

    override val crafterType: Int
        get() = type.craftingType

    val crafterGUI = CraftingBlockGUI(this)
    var recipe: Recipe? = null

    private var currentResources = ResourceList()

    init {
        containers.forEach { container ->
            container.listeners.add(this)
            // only allow input if there is a recipe
            container.typeRule = { this.recipe != null }
            // only allow addition if there are less ingredients than required
            container.additionRule = { resource, quantity -> this.recipe != null && resource in this.recipe!!.consume && container.getQuantity(resource) + quantity <= this.recipe!!.consume.getQuantity(resource) }
        }
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_THIS, Control.INTERACT)
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
        if (containers.remove(recipe!!.consume))
            nodes.output(recipe!!.produce, false)
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.PRESSED && p.control == Control.INTERACT) {
            crafterGUI.toggle()
        }
    }
}