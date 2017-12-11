package level.tile

import main.Game.currentLevel

class OreTile(override val type: OreTileType, xTile: Int, yTile: Int) : Tile(type, xTile, yTile) {

    var amount = ((type.maxAmount - type.minAmount) * Math.random() + 1 + type.minAmount).toInt()
        set(value) {
            if (value < 1) {
                currentLevel.getChunk(xChunk, yChunk).setTile(Tile(type.backgroundType, xTile, yTile))
                field = value
            }
        }
}