package level.block

import com.badlogic.gdx.Input
import io.ControlEvent
import io.ControlEventType
import item.Inventory
import level.getTileAtTile
import level.tile.OreTile
import resource.*
import serialization.Id

class MinerBlock(xTile: Int, yTile: Int) : MachineBlock(MachineBlockType.MINER, xTile, yTile) {

    @Id(23)
    val output = Inventory(1, 1)

    lateinit var node: ResourceNode

    override fun createNodes(): List<ResourceNode> {
        return listOf(ResourceNode(output, xTile, yTile))
    }

    override fun onFinishWork() {
        for (x in 0 until type.widthTiles) {
            for (y in 0 until type.heightTiles) {
                val tile = level.getTileAtTile(xTile + x, yTile + y)
                if (tile is OreTile) {
                    val transaction = ResourceTransaction(SourceContainer(), output, resourceListOf(tile.type.minedItem to 1))
                    // fill up the internal inventory
                    if (output.add(tile.type.minedItem, 1)) {
                        tile.amount -= 1
                        return
                    } else {
                        currentWork = type.maxWork
                    }
                }
            }
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