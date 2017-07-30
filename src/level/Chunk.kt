package level

import level.block.Block
import level.moving.MovingObject
import level.tile.Tile

class Chunk(val xChunk: Int, val yChunk: Int) {

    var loaded = false
    val tiles: Array<Tile>? = null
    val blocks: Array<Block?>? = null
    val moving: ArrayList<MovingObject>? = null

    fun load() {
        if(!loaded) {

        }
    }
}