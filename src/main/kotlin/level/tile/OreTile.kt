package level.tile

import level.Level

class OreTile(override val type: OreTileType, xTile: Int, yTile: Int) : Tile(type, xTile, yTile) {

    var amount = ((type.maxAmount - type.minAmount) * Math.random() + 1 + type.minAmount).toInt()
        set(value) {
            if (value < 1) {
                Level.Chunks.get(xChunk, yChunk).setTile(Tile(type.backgroundType, xTile, yTile))
                field = value
            }
        }
}