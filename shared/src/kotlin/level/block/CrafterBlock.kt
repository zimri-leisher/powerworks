package level.block

import com.badlogic.gdx.Input
import crafting.Crafter
import crafting.Recipe
import io.ControlEvent
import io.ControlEventType
import resource.*
import routing.script.RoutingLanguage
import serialization.Id

open class CrafterBlock(override val type: CrafterBlockType<*>, xTile: Int, yTile: Int, rotation: Int) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener, Crafter {

    override val crafterType: Crafter.Type
        get() = type.crafterType

    @Id(23)
    var recipe: Recipe? = null
        set(value) {
            if (field != value) {
                field = value
                // clear behavior of input nodes
                inputNodes.forEach { it.behavior.allowIn.clearStatements(); it.behavior.forceIn.clearStatements() }
                if (field != null) {
                    val canCraft = enoughToCraft()
                    if (on && !canCraft) {
                        currentWork = 0
                    }
                    on = canCraft
                    inputNodes.forEach {
                        it.behavior.allowIn.addStatement(RoutingLanguage.TRUE, field!!.consume.keys.toList())
                        for ((type, quantity) in field!!.consume) {
                            it.behavior.forceIn.addStatement(RoutingLanguage.parse("quantity of $type < $quantity"), listOf(type))
                        }
                    }
                } else {
                    inputNodes.forEach { it.behavior.allowIn.addStatement(RoutingLanguage.FALSE); it.behavior.forceIn.addStatement(RoutingLanguage.FALSE) }
                }
            }
        }

    @Id(24)
    private var currentResources = ResourceList()

    @Id(25)
    private val inputNodes = nodes.filter { it.behavior.allowIn.possible()?.isEmpty() == true } // nodes that start out allowing all types out

    @Id(26)
    val inputContainer = inputNodes.getAttachedContainers().first()

    @Id(27)
    val outputContainer = nodes.filter { it.behavior.allowOut.possible()?.isEmpty() == true }.getAttachedContainers().first()

    init {
        inputContainer.listeners.add(this)
    }

    override fun onAddToLevel() {
        super.onAddToLevel()
        // we want to edit their behavior so that they only accept what a recipe needs. We'll set it to false for now because there is no recipe, and update
        // the behavior when we change the recipe
        inputNodes.forEach { it.behavior.allowIn.setStatement(RoutingLanguage.FALSE) }
    }

    override fun onContainerClear(container: ResourceContainer) {
        // basically, refresh the current resource list
        currentResources.clear()
        currentResources = containers.toResourceList()
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
        currentResources.addAll(resources)
        val canCraft = enoughToCraft()
        if (on && !canCraft) {
            currentWork = 0
        }
        on = canCraft
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
        currentResources.takeAll(resources)
        val canCraft = enoughToCraft()
        if (on && !canCraft) {
            currentWork = 0
        }
        on = canCraft
    }

    private fun enoughToCraft() = currentResources.containsAtLeastAll(recipe!!.consume)

    override fun onFinishWork() {
        if (recipe == null) {
            return
        }
        if (outputContainer.spaceFor(recipe!!.produce) && inputContainer.contains(recipe!!.consume)) {
            for ((type, quantity) in recipe!!.consume) {
                inputContainer.remove(type, quantity)
            }
            for ((type, quantity) in recipe!!.produce) {
                outputContainer.add(type, quantity)
            }
        } else {
            currentWork = type.maxWork
        }
    }

    override fun onInteractOn(event: ControlEvent, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (event.type == ControlEventType.PRESS && !shift && !ctrl && !alt) {
            if (button == Input.Buttons.LEFT) {
                this.type.guiPool!!.toggle(this)
            }
        }
    }
}