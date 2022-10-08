package level.block

import com.badlogic.gdx.Input
import fluid.FluidTank
import fluid.MoltenOreFluidType
import io.ControlEvent
import io.ControlEventType
import item.Inventory
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import resource.ResourceNode
import serialization.Id

class SolidifierBlock(xTile: Int, yTile: Int) :
    MachineBlock(MachineBlockType.SOLIDIFIER, xTile, yTile), ResourceContainerChangeListener {

    @Id(23)
    val input = FluidTank(10)

    @Id(24)
    val output = Inventory(1, 1)

    @Id(25)
    var currentlySolidifying: MoltenOreFluidType? = null

    init {
        input.listeners.add(this)
    }

    override fun createNodes(): List<ResourceNode> {
        return listOf(ResourceNode(input, xTile, yTile + 1), ResourceNode(output, xTile, yTile))
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
        if (container.id == input.id) {
            if (input.currentAmount > 0) {
                currentlySolidifying = input.currentFluidType!! as MoltenOreFluidType
                on = true
            } else {
                on = false
                currentWork = 0
            }
        }
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
        if (container.id == input.id) {
            if (input.currentAmount > 0) {
                currentlySolidifying = input.currentFluidType!! as MoltenOreFluidType
                on = true
            } else {
                on = false
                currentWork = 0
            }
        }
    }

    override fun onContainerClear(container: ResourceContainer) {
        if (container.id == input.id) {
            currentlySolidifying = null
            on = false
            currentWork = 0
        }
    }

    override fun onInteractOn(
        event: ControlEvent,
        x: Int,
        y: Int,
        button: Int,
        shift: Boolean,
        ctrl: Boolean,
        alt: Boolean
    ) {
        if (event.type == ControlEventType.PRESS && !shift && !ctrl && !alt) {
            if (button == Input.Buttons.LEFT) {
                this.type.guiPool!!.toggle(this)
            }
        }
    }

    override fun onFinishWork() {
        if (currentlySolidifying == null) {
            return
        }
        if (output.add(currentlySolidifying!!.ingot)) {
            input.remove(currentlySolidifying!!)
        } else {
            currentWork = type.maxWork
        }
        if (input.currentAmount == 0) {
            currentlySolidifying = null
        }
    }
}
