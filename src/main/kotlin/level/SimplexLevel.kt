package level

import level.block.Block
import level.tile.OreTile
import level.tile.OreTileType
import level.tile.Tile
import level.tile.TileType
import misc.Numbers.genRandom
import java.util.*


class SimplexLevel(info: LevelInfo) : Level(info) {

    override fun genTiles(xChunk: Int, yChunk: Int): Array<Tile> {
        val rand = Random(genRandom(xChunk.toLong(), yChunk.toLong()))
        val tiles = arrayOfNulls<Tile>(CHUNK_SIZE_TILES * CHUNK_SIZE_TILES)
        val xTile = xChunk shl CHUNK_TILE_EXP
        val yTile = yChunk shl CHUNK_TILE_EXP
        for (y in 0 until CHUNK_SIZE_TILES) {
            for (x in 0 until CHUNK_SIZE_TILES) {
                tiles[x + y * CHUNK_SIZE_TILES] = genTile(x + xTile, y + yTile, rand)
            }
        }
        return tiles as Array<Tile>
    }

    private fun genTile(xTile: Int, yTile: Int, rand: Random): Tile {
        for(t in OreTileType.ALL) {
            val noise = ((oreNoises.get(t)!!.getNoise(xTile.toDouble() / 24.0, yTile.toDouble() / 24.0) / .826) + 1) / 2 // 0 to 1
            if(noise < t.generationChance && rand.nextInt(t.scatter) == 0)
                return OreTile(t, xTile, yTile)
        }
        return Tile(TileType.GRASS, xTile, yTile)
    }

    override fun genBlocks(xChunk: Int, yChunk: Int): Array<Block?> {
        return arrayOfNulls(CHUNK_SIZE_TILES * CHUNK_SIZE_TILES)
    }
}