package level.block

import level.tile.OreTile
import main.Game

class MinerBlock(xTile: Int, yTile: Int) : MachineBlock(xTile, yTile, MachineBlockType.MINER, true) {
    override fun onFinishWork() {
        if(Game.currentLevel.getTile(xTile, yTile) is OreTile)
            println("mined an ore!")
        else
            println("tried to mine an ore but failed")
    }
}