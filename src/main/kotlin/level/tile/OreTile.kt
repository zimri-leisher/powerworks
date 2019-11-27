package level.tile

import level.Level
import level.getChunkAt
import level.LevelManager

class OreTile(override val type: OreTileType, xTile: Int, yTile: Int, level: Level) : Tile(type, xTile, yTile, level) {

    private constructor() : this(OreTileType.GRASS_IRON_ORE, 0, 0, LevelManager.EMPTY_LEVEL)

    var amount = ((type.maxAmount - type.minAmount) * Math.random() + 1 + type.minAmount).toInt()
        set(value) {
            if (value < 1) {
                level.getChunkAt(xChunk, yChunk).setTile(Tile(type.backgroundType, xTile, yTile, level))
                field = value
            }
        }
}