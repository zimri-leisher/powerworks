package level.block

import com.badlogic.gdx.Input
import crafting.Crafter
import crafting.Recipe
import io.PressType
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import resource.ResourceType
import screen.CrafterBlockGUI

open class CrafterBlock(override val type: CrafterBlockType, xTile: Int, yTile: Int, rotation: Int) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener, Crafter {

    override val crafterType: Crafter.Type
        get() = type.crafterType

    val gui = CrafterBlockGUI(this)
    var recipe: Recipe? = null

    private var currentResources = ResourceList()

    init {
        containers.forEach { container ->
            container.listeners.add(this)
            // only allow input if there is a recipe
            container.typeRule = { recipe != null }
            // only allow addition if there are less ingredients than required
            container.additionRule = { resource, quantity ->
                resource in recipe!!.consume && container.getQuantity(resource) + quantity <= recipe!!.consume.getQuantity(resource)
            }
        }
    }

    override fun onRemoveFromLevel() {
        super.onRemoveFromLevel()
        gui.open = false
    }

    override fun onContainerClear(container: ResourceContainer) {
        // basically, refresh the current resource list
        currentResources.clear()
        currentResources = containers.toList()
    }

    override fun onContainerChange(container: ResourceContainer, resource: ResourceType, quantity: Int) {
        if (quantity < 0) {
            currentResources.remove(resource, -quantity)
        } else if (quantity > 0) {
            currentResources.add(resource, quantity)
        }
        val canCraft = enoughToCraft()
        if (on && !canCraft) {
            currentWork = 0
        }
        on = canCraft
    }

    private fun enoughToCraft() = recipe?.consume?.enoughIn(currentResources) == true

    override fun onFinishWork() {
        if (nodes.canOutput(recipe!!.produce, mustContainEnough = false)) {
            if (containers.remove(recipe!!.consume)) {
                nodes.output(recipe!!.produce, mustContainEnough = false)
            }
        } else {
            currentWork = type.maxWork
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED && button == Input.Buttons.LEFT) {
            gui.toggle()
        }
    }
}