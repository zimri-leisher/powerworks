package level.tile

import level.Level
import level.getChunkAt

class OreTile(override val type: OreTileType, xTile: Int, yTile: Int, level: Level) : Tile(type, xTile, yTile, level) {

    var amount = ((type.maxAmount - type.minAmount) * Math.random() + 1 + type.minAmount).toInt()
        set(value) {
            if (value < 1) {
                level.getChunkAt(xChunk, yChunk).setTile(Tile(type.backgroundType, xTile, yTile, level))
                field = value
            }
        }
}