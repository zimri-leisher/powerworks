package level

import level.block.Block
import level.tile.Tile
import java.util.*

abstract class Level(seed: Long) {
    val rand: Random = Random(seed)

    abstract fun getTiles(xChunk: Int, yChunk: Int): Array<Tile>

    abstract fun getBlocks(xChunk: Int, yChunk: Int): Array<Block?>

}