package level.block

import com.badlogic.gdx.Input
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import fluid.FluidTank
import io.ControlEvent
import io.ControlEventType
import serialization.Id

class FluidTankBlock(type: FluidTankBlockType, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation) {

    @Id(20)
    val tank = containers.first { it is FluidTank } as FluidTank

    override fun onInteractOn(event: ControlEvent, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (event.type == ControlEventType.PRESS && !shift && !ctrl && !alt) {
            if (button == Input.Buttons.LEFT) {
                this.type.guiPool!!.toggle(this)
            }
        }
    }
}