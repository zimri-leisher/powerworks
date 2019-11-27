package level.generator

import level.Chunk
import level.Level
import level.LevelManager
import level.tile.Tile

class EmptyLevelGenerator(level: Level) : LevelGenerator(level) {

    private constructor() : this(LevelManager.EMPTY_LEVEL)

    override fun generateChunk(xChunk: Int, yChunk: Int): Chunk {
        return Chunk(xChunk, yChunk)
    }

    override fun generateTiles(xChunk: Int, yChunk: Int): Array<Tile> {
        return arrayOf()
    }
}