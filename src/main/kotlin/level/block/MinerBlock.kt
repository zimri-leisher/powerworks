package level.block

import level.Level
import level.tile.OreTile

class MinerBlock(xTile: Int, yTile: Int, rotation: Int) : MachineBlock(MachineBlockType.MINER, xTile, yTile, rotation) {

    override fun onFinishWork() {
        for (x in 0 until type.widthTiles) {
            for (y in 0 until type.heightTiles) {
                val tile = Level.Tiles.get(xTile + x, yTile + y)
                if (tile is OreTile) {
                    if (nodes.output(tile.type.minedItem, 1, false)) {
                        tile.amount -= 1
                        if (tile.amount == 0)
                            requiresUpdate = false
                        return
                    } else {
                        // keep on doing this until it is successful
                        currentWork = type.maxWork
                    }
                }
            }
        }

    }
}