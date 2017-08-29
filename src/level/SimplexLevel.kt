package level

import level.block.Block
import level.tile.*


class SimplexLevel(width: Int, height: Int, seed: Long) : Level(seed, width, height) {

    private var singleOre: SimplexNoise

    init {
        singleOre = SimplexNoise(100, 0.5, genRandom(seed, 99))
    }

    private fun genRandom(seed: Long, seed2: Long): Long {
        return ((seed / 7).toDouble() * (seed2 % 31).toDouble() * 0.55349).toLong()
    }

    private fun scatter(scatter: Int): Boolean {
        if (rand.nextInt(scatter) == 0) {
            return true
        }
        return false
    }

    override fun genTiles(xChunk: Int, yChunk: Int): Array<Tile> {
        val tiles = arrayOfNulls<Tile>(CHUNK_SIZE * CHUNK_SIZE)
        val xTile = xChunk shl 3
        val yTile = yChunk shl 3
        for (y in 0..CHUNK_SIZE - 1) {
            for (x in 0..CHUNK_SIZE - 1) {
                val singleOreNoise = 1 + singleOre.getNoise(x + xTile, y + yTile)
                if (singleOreNoise < IRON_ORE_THRESHOLD) {
                    tiles[x + y * CHUNK_SIZE] = Tile(TileTypes.GRASS, x + xTile, y + yTile)
                } else if (singleOreNoise < IRON_ORE_MAX_THRESHOLD) {
                    if (scatter(IRON_ORE_SCATTER)) {
                        tiles[x + y * CHUNK_SIZE] = OreTile(OreTileTypes.GRASS_IRON_ORE, x + xTile, y + yTile)
                    } else {
                        tiles[x + y * CHUNK_SIZE] = Tile(TileTypes.GRASS, x + xTile, y + yTile)
                    }
                } else {
                    tiles[x + y * CHUNK_SIZE] = Tile(TileTypes.GRASS, x + xTile, y + yTile)
                }
            }
        }
        return tiles.requireNoNulls()
    }

    override fun genBlocks(xChunk: Int, yChunk: Int): Array<Block?> {
        return arrayOfNulls(CHUNK_SIZE * CHUNK_SIZE)
    }

    companion object {
        internal val IRON_ORE_THRESHOLD = 1.41
        internal val IRON_ORE_MAX_THRESHOLD = 2.0
        internal val IRON_ORE_SCATTER = 5
    }
}