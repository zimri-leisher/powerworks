package level.block

import com.badlogic.gdx.Input
import fluid.FluidTank
import fluid.MoltenOreFluidType
import io.PressType
import item.Inventory
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import serialization.Id

class SolidifierBlock(xTile: Int, yTile: Int, rotation: Int) : MachineBlock(MachineBlockType.SOLIDIFIER, xTile, yTile, rotation), ResourceContainerChangeListener {

    @Id(23)
    val tank = containers.first { it is FluidTank } as FluidTank

    @Id(24)
    val out = containers.first { it is Inventory } as Inventory

    @Id(25)
    var currentlySolidifying: MoltenOreFluidType? = null

    init {
        containers.forEach { it.listeners.add(this) }
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
        if (container == tank) {
            if (tank.currentAmount > 0) {
                currentlySolidifying = tank.currentFluidType!! as MoltenOreFluidType
                on = true
            } else {
                on = false
                currentWork = 0
            }
        }
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
        if (container == tank) {
            if (tank.currentAmount > 0) {
                currentlySolidifying = tank.currentFluidType!! as MoltenOreFluidType
                on = true
            } else {
                on = false
                currentWork = 0
            }
        }
    }

    override fun onContainerClear(container: ResourceContainer) {
        if (container == tank) {
            currentlySolidifying = null
            on = false
            currentWork = 0
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.RELEASED && !shift && !ctrl && !alt && button == Input.Buttons.LEFT) {
            this.type.guiPool!!.toggle(this)
        }
    }

    override fun onFinishWork() {
        if (out.add(currentlySolidifying!!.ingot)) {
            tank.remove(currentlySolidifying!!)
        }
        if (tank.currentAmount == 0) {
            currentlySolidifying = null
        }
    }
}
