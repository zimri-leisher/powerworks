package level.block

import level.getTileAt
import level.tile.OreTile
import resource.give
import resource.output

class MinerBlock(xTile: Int, yTile: Int, rotation: Int) : MachineBlock(MachineBlockType.MINER, xTile, yTile, rotation) {

    override fun onFinishWork() {
        for (x in 0 until type.widthTiles) {
            for (y in 0 until type.heightTiles) {
                val tile = level.getTileAt(xTile + x, yTile + y)
                if (tile is OreTile) {
                    // fill up the internal inventory
                    if (containers.give(tile.type.minedItem, 1)) {
                        tile.amount -= 1
                        return
                    }
                }
            }
        }
        currentWork = type.maxWork
    }
}