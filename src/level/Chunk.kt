package level

import level.block.Block
import level.moving.MovingObject
import level.tile.Tile

class Chunk(val parent: Level, val xChunk: Int, val yChunk: Int) {

    var xTile = xChunk shl 3
    var yTile = yChunk shl 3
    var loaded = false
    var tiles: Array<Tile>? = null
    var blocks: Array<Block?>? = null
    var moving: MutableList<MovingObject>? = null
    var collidables: MutableList<Collidable>? = null
    var updatesRequired: MutableList<LevelObject>? = null
    var beingRendered = false

    /* Convenience methods. Assume it is loaded */
    fun getBlock(xTile: Int, yTile: Int) = blocks!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE]

    fun getTile(xTile: Int, yTile: Int) = tiles!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE]
    fun setTile(tile: Tile) {
        tiles!![(tile.xTile - xTile) + (tile.yTile - yTile) * CHUNK_SIZE] = tile
        /* Don't bother checking if it requires an update */
    }

    fun setBlock(block: Block) {
        blocks!![(block.xTile - xTile) + (block.yTile - yTile) * CHUNK_SIZE] = block
        if (block.requiresUpdate)
            updatesRequired!!.add(block)
    }

    fun removeBlock(block: Block) {
        blocks!![(block.xTile - xTile) + (block.yTile - yTile) * CHUNK_SIZE] = null
        if (block.requiresUpdate)
            updatesRequired!!.remove(block)
    }

    fun addMoving(m: MovingObject) {
        moving!!.add(m)
        if (m.requiresUpdate)
            updatesRequired!!.add(m)
    }

    fun removeMoving(m: MovingObject) {
        moving!!.remove(m)
        if (m.requiresUpdate)
            updatesRequired!!.remove(m)
    }

    fun update() {
        /* Assume is already loaded */
        if (updatesRequired!!.size > 0)
            updatesRequired!!.forEach { it.update() }
        else if (!beingRendered) {
            unload()
        }
    }

    fun load(blocks: Array<Block?>, tiles: Array<Tile>) {
        println("loading chunk at $xChunk, $yChunk")
        this.blocks = blocks
        this.tiles = tiles
        this.moving = mutableListOf()
        this.updatesRequired = mutableListOf()
        loaded = true
        parent.loadedChunks.add(this)
    }

    fun unload() {
        println("unloading chunk at $xChunk, $yChunk")
        blocks = null
        tiles = null
        moving = null
        updatesRequired = null
        loaded = false
        parent.loadedChunks.remove(this)
    }
}