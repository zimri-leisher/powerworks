package level.block

import com.badlogic.gdx.Input
import io.ControlEvent
import io.ControlEventType
import item.Inventory
import resource.ResourceNode
import serialization.Id

class ChestBlock(override val type: ChestBlockType, xTile: Int, yTile: Int) : Block(type, xTile, yTile) {

    private constructor() : this(ChestBlockType.SMALL, 0, 0)

    @Id(20)
    val inventory = Inventory(type.invWidth, type.invHeight)

    override fun createNodes(): List<ResourceNode> {
        return listOf(ResourceNode(inventory, xTile, yTile))
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