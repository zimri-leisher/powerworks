package level.block

import level.tile.OreTile
import main.Game

class MinerBlock(xTile: Int, yTile: Int) : MachineBlock(xTile, yTile, MachineBlockType.MINER, true) {

    override fun onFinishWork() {
        for(x in 0 until type.widthTiles) {
            for(y in 0 until type.heightTiles) {
                val tile = Game.currentLevel.getTile(xTile + x, yTile + y)
                if(tile is OreTile) {
                    /*
                    if(.output(tile.type.minedItem, 1)) {
                        tile.amount -= 1
                        if(tile.amount == 0)
                            requiresUpdate = false
                    }
                    return
                    */
                }
            }
        }

    }
}