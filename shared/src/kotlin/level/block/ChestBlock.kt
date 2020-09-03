package level.block

import com.badlogic.gdx.Input
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import io.ControlEvent
import io.ControlEventType
import item.Inventory
import serialization.Id

class ChestBlock(override val type: ChestBlockType, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation) {

    @Id(20)
    val inventory = containers.filterIsInstance<Inventory>().first()

    override fun onInteractOn(event: ControlEvent, x: Int, y: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (event.type == ControlEventType.PRESS && !shift && !ctrl && !alt) {
            if (button == Input.Buttons.LEFT) {
                this.type.guiPool!!.toggle(this)
            }
        }
    }
}