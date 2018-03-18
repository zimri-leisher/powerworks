package level.block

import crafting.Recipe
import item.Inventory
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import resource.ResourceType

class CrafterBlock(override val type: CrafterBlockTemplate, xTile: Int, yTile: Int, rotation: Int, recipe: Recipe? = null) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener {

    private val containers = nodes.getAttachedContainers()
    var recipe = recipe
        set(value) {
            field = value
            if (value != null) {
                containers.forEach { it.rule = { it in value.consume } }
            } else {
                containers.forEach { it.rule = { false } }
            }
        }

    val currentResources = ResourceList()

    init {
        containers.forEach { it.listeners.add(this) }
    }

    override fun onContainerAdd(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
    }

    override fun onContainerRemove(inv: Inventory, resource: ResourceType, quantity: Int) {
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
    }

    override fun onContainerChange(container: ResourceContainer<*>) {
        currentResources.clear()
        containers.forEach { currentResources.addAll(it.toList()) }
        val canCraft = canCraft()
        if (on && !canCraft) {
            currentWork = 0
        }
        on = canCraft
    }

    fun canCraft() = recipe?.consume?.enoughIn(currentResources) == true

    override fun onFinishWork() {
        val consume = recipe!!.consume
        outer@ for ((r, q) in consume) {
            for (c in containers) {
                if (c.remove(r, q))
                    continue@outer
            }
        }
        nodes.output(recipe!!.produce)
    }
}