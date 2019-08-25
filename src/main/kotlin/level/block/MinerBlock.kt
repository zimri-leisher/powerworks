package level.block

import level.Level
import level.tile.OreTile

class MinerBlock(xTile: Int, yTile: Int, rotation: Int) : MachineBlock(MachineBlockType.MINER, xTile, yTile, rotation) {
    override fun onFinishWork() {
        for (x in 0 until type.widthTiles) {
            for (y in 0 until type.heightTiles) {
                val tile = Level.Tiles.get(xTile + x, yTile + y)
                if (tile is OreTile) {
                    // fill up the internal inventory
                    if (containers.add(tile.type.minedItem, 1)) {
                        tile.amount -= 1
                    }
                    // if it was already full or if it wasn't, this still happens
                    /*
                    if (!nodes.output(tile.type.minedItem, 1)) {
                        currentWork = type.maxWork
                    } else {
                        // stop this from sending more than 1 ore
                        return
                    }
                    */
                }
            }
        }
    }
}