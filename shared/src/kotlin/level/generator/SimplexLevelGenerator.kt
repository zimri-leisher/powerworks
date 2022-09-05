package level.generator

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import level.*
import level.tile.OreTile
import level.tile.OreTileType
import level.tile.Tile
import level.tile.TileType
import misc.Numbers.genRandom
import serialization.Id
import java.util.*

class SimplexLevelGenerator(level: Level) : LevelGenerator(level) {

    private constructor() : this(LevelManager.EMPTY_LEVEL)

    @Id(3)
    val oreNoises = mutableMapOf<OreTileType, Noise>()

    init {
        val rand = Random(level.info.seed)
        for (t in OreTileType.ALL) {
            val newSeed = genRandom(level.info.seed, rand.nextInt(99).toLong())
            oreNoises.put(t, OpenSimplexNoise(newSeed))
        }
    }

    override fun generateChunk(xChunk: Int, yChunk: Int): Chunk {
        return Chunk(xChunk, yChunk).apply {
            data.tiles = generateTiles(xChunk, yChunk)
            data.blocks = arrayOfNulls(CHUNK_SIZE_TILES * CHUNK_SIZE_TILES)
            data.resourceNodes = arrayOfNulls(CHUNK_SIZE_TILES * CHUNK_SIZE_TILES)
        }
    }

    override fun generateTiles(xChunk: Int, yChunk: Int): Array<Tile> {
        val rand = Random(genRandom(xChunk.toLong(), yChunk.toLong()))
        val tiles = arrayOfNulls<Tile>(CHUNK_SIZE_TILES * CHUNK_SIZE_TILES)
        val xTile = xChunk shl CHUNK_TILE_EXP
        val yTile = yChunk shl CHUNK_TILE_EXP
        for (y in 0 until CHUNK_SIZE_TILES) {
            for (x in 0 until CHUNK_SIZE_TILES) {
                tiles[x + y * CHUNK_SIZE_TILES] = generateTile(x + xTile, y + yTile, rand)
            }
        }
        return tiles as Array<Tile>
    }

    private fun generateTile(xTile: Int, yTile: Int, rand: Random): Tile {
        for (t in OreTileType.ALL) {
            val noise = ((oreNoises.get(t)!!.getNoise(xTile.toDouble() / 24.0, yTile.toDouble() / 24.0) / .826) + 1) / 2 // 0 to 1
            if (noise < t.generationChance)
                return OreTile(t, xTile, yTile, level)
        }
        return Tile(TileType.GRASS, xTile, yTile, level)
    }
}