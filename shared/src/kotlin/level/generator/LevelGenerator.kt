package level.generator

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import data.ConcurrentlyModifiableMutableList
import level.Chunk
import level.Level
import level.LevelData
import level.LevelManager
import level.block.Block
import level.tile.Tile
import serialization.Id

/**
 * A stateless class to generate [LevelData], e.g. [Chunk]s and their constituents: [Tile]s, [Block]s, etc.
 *
 * Stateless, in this context, means that previously calling any method here should not affect the results of any other method,
 * no matter when or in what order they are called. This is important so that levels can be generated the same every time,
 * especially in the case of [Tile]s, which are not sent over the network unless they are modified, and instead are just
 * generated from this every time.
 */
abstract class LevelGenerator(
    @Id(1)
    val level: Level
) {

    private constructor() : this(LevelManager.EMPTY_LEVEL)

    abstract fun generateChunk(xChunk: Int, yChunk: Int): Chunk

    abstract fun generateTiles(xChunk: Int, yChunk: Int): Array<Tile>

    fun generateChunks(): Array<Chunk> {
        val chunks: Array<Chunk?> = arrayOfNulls(level.widthChunks * level.heightChunks)
        for (y in 0 until level.heightChunks) {
            for (x in 0 until level.widthChunks) {
                chunks[x + y * level.widthChunks] = generateChunk(x, y)
            }
        }
        return chunks.requireNoNulls()
    }

    fun generateData() = LevelData(
        ConcurrentlyModifiableMutableList(),
        ConcurrentlyModifiableMutableList(),
        generateChunks(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf()
    )

}