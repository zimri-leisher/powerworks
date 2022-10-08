package level.block

import com.badlogic.gdx.Input
import fluid.FluidTank
import io.ControlEvent
import io.ControlEventType
import resource.ResourceNode
import serialization.Id

class FluidTankBlock(type: FluidTankBlockType, xTile: Int, yTile: Int) : Block(type, xTile, yTile) {

    @Id(20)
    val tank = FluidTank(type.maxAmount)

    override fun createNodes(): List<ResourceNode> {
        return listOf(ResourceNode(tank, xTile, yTile))
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