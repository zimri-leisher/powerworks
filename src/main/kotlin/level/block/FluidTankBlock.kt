package level.block

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import fluid.FluidTank
import io.PressType

class FluidTankBlock(type: FluidTankBlockType, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation) {

    @Tag(20)
    val tank = containers.first { it is FluidTank } as FluidTank

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.RELEASED) {
            this.type.guiPool!!.toggle(this)
        }
    }
}