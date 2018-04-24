package level.block

import crafting.Crafter
import crafting.Recipe
import io.*
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import resource.ResourceType
import screen.CraftingBlockGUI

class CrafterBlock(override val type: CrafterBlockType, xTile: Int, yTile: Int, rotation: Int, recipe: Recipe? = null) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener, ControlPressHandler, Crafter {

    override val crafterType: Int
        get() = type.craftingType

    val crafterGUI = CraftingBlockGUI(this)
    var recipe = recipe
        set(value) {
            field = value
            if (value != null) {
                // enable inputting of resources
                containers.forEach { it.typeRule = { true } }
                containers.forEach { container -> container.additionRule = { resource, quantity -> resource in value.consume && container.getQuantity(resource) + quantity <= value.consume.getQuantity(resource) } }
            } else {
                // disable inputting
                containers.forEach { it.typeRule = { false } }
            }
        }

    val currentResources = ResourceList()

    init {
        containers.forEach { it.listeners.add(this); it.typeRule = { false } }
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_THIS, Control.INTERACT)
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
        currentResources.clear()
        containers.forEach { currentResources.addAll(it.toList()) }
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