package level.block

import com.badlogic.gdx.Input
import io.ControlEvent
import io.ControlEventType
import item.Inventory
import item.ItemType
import item.OreItemType
import resource.*
import serialization.Id

class SmelterBlock(xTile: Int, yTile: Int) : MachineBlock(MachineBlockType.SMELTER, xTile, yTile),
    ResourceContainerChangeListener {
    @Id(23)
    val input = Inventory(1, 1)

    @Id(24)
    val output = Inventory(1, 1)

    @Id(25)
    var currentlySmelting: OreItemType? = null

    init {
        input.listeners.add(this)
    }

    override fun createNodes(): List<ResourceNode> {
        return listOf(ResourceNode(input, xTile, yTile + 1), ResourceNode(output, xTile, yTile))
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
        on = input.totalQuantity > 1 && !(output[0] != null && output[0]!!.quantity >= (output[0]!!.type as ItemType).maxStack)
        super.update()
    }

    override fun onFinishWork() {
        if (currentlySmelting == null) {
            // some desync
            return
        }
        if (output.canAdd(resourceListOf(currentlySmelting!!.moltenForm.ingot to 1))) {
            if (input.remove(currentlySmelting!!, 2)) {
                output.add(currentlySmelting!!.moltenForm.ingot, 1)
            }
        }
        if (input.totalQuantity > 1) {
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