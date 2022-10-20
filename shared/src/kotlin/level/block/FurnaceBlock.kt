package level.block

import com.badlogic.gdx.Input
import fluid.FluidTank
import io.*
import item.Inventory
import item.OreItemType
import resource.*
import serialization.Id

class FurnaceBlock(xTile: Int, yTile: Int) : MachineBlock(MachineBlockType.FURNACE, xTile, yTile),
    ResourceContainerChangeListener {

    private constructor() : this(0, 0)

    // the internal inventory, not the internal tank
    @Id(23)
    val input = Inventory(1, 1)

    @Id(24)
    val output = FluidTank(100)

    @Id(25)
    var currentlySmelting: OreItemType? = null

    init {
        input.listeners.add(this)
    }

    override fun createNodes(): List<ResourceNode> {
        return listOf(ResourceNode(input, xTile, yTile), ResourceNode(output, xTile, yTile))
    }

    override fun onContainerClear(container: ResourceContainer) {
        if (container.id == input.id) {
            currentlySmelting = null
            on = false
        }
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
        if (container.id == input.id) {
            if (currentlySmelting == null) {
                currentlySmelting = resources[0].key as OreItemType
                on = true
            }
        }
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
        if (container.id == input.id) {
            if (currentlySmelting == null) {
                currentlySmelting = resources[0].key as OreItemType
                on = true
            }
        }
    }

    override fun update() {
        if (output.totalQuantity > 0) {
            // FIXME nodes.output(tank.currentFluidType!!, 1)
        }
        on = input.totalQuantity > 0 && output.totalQuantity < output.maxAmount
        super.update()
    }

    override fun onFinishWork() {
        if (currentlySmelting == null) {
            // some desync
            return
        }
        if (output.canAdd(resourceListOf(currentlySmelting!!.moltenForm to 1))) {
            if (input.remove(currentlySmelting!!, 1)) {
                output.add(currentlySmelting!!.moltenForm, 1)
            }
        }
        if (input.totalQuantity > 0) {
            // start smelting another item
            if (input.getQuantity(currentlySmelting!!) == 0) {
                currentlySmelting = input.toResourceList()[0].key as OreItemType
            }
            // do nothing, old item still has quantity
        } else {
            on = false
            currentlySmelting = null
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
}