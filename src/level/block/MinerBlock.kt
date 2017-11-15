package level.block

import inv.ItemType
import level.node.OutputNode
import level.resource.ResourceType
import level.tile.OreTile
import main.Game

class MinerBlock(xTile: Int, yTile: Int) : MachineBlock(xTile, yTile, MachineBlockType.MINER, true) {

    val out: OutputNode<ItemType> = OutputNode(xTile, yTile, rotation, null, ResourceType.ITEM)

    override fun onFinishWork() {
        val tile = Game.currentLevel.getTile(xTile, yTile)
        if(tile is OreTile) {
            out.output(tile.type.minedItem, 1)
            tile.amount -= 1
        }
    }
}