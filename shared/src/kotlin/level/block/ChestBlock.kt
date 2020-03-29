package level.block

import com.badlogic.gdx.Input
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import io.PressType
import item.Inventory
import serialization.Id

class ChestBlock(override val type: ChestBlockType, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation) {

    @Id(20)
    val inv = containers.filterIsInstance<Inventory>().first()

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            if (button == Input.Buttons.LEFT) {
                this.type.guiPool!!.toggle(this)
            }
        }
    }
}