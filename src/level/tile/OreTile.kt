package level.tile

import main.Game
import main.Game.currentLevel

class OreTile(type: OreTileType, xTile: Int, yTile: Int) : Tile(type, xTile, yTile) {
    var amount = (type.maxAmount - type.minAmount) * Math.random() + 1 + type.minAmount
        set(value) {
            if(value < 1) {
                currentLevel.getChunk(xChunk, yChunk)
            }
        }
}