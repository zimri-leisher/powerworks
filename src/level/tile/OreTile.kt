package level.tile

import main.Game.currentLevel

class OreTile(type: OreTileType, xTile: Int, yTile: Int) : Tile(type, xTile, yTile) {

    override val type: OreTileType = type

    var amount = (type.maxAmount - type.minAmount) * Math.random() + 1 + type.minAmount
        set(value) {
            if(value < 1) {
                currentLevel.getChunk(xChunk, yChunk)
            }
        }
}