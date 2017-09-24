package level

import level.block.Block
import level.moving.MovingObject
import level.tile.Tile

class UpdateableOrganizer {

    var beingTraversed = false

    val elements = mutableListOf<LevelObject>()

    val toAdd = mutableListOf<LevelObject>()

    val toRemove = mutableListOf<LevelObject>()

    fun add(l: LevelObject) {
        if (beingTraversed)
            toAdd.add(l)
        else
            elements.add(l)
    }

    fun remove(l: LevelObject) {
        if (beingTraversed)
            toRemove.add(l)
        else
            elements.remove(l)
    }

    val size
        get() = elements.size + toAdd.size - toRemove.size

    fun forEach(f: (LevelObject) -> Unit) {
        beingTraversed = true
        elements.forEach(f)
        beingTraversed = false
        elements.addAll(toAdd)
        toAdd.clear()
        elements.removeAll(toRemove)
        toRemove.clear()
    }
}

class Chunk(val parent: Level, val xChunk: Int, val yChunk: Int) {

    var xTile = xChunk shl 3
    var yTile = yChunk shl 3
    var loaded = false
    var tiles: Array<Tile>? = null
    var blocks: Array<Block?>? = null
    var moving: MutableList<MovingObject>? = null
    var movingOnBoundary: MutableList<MovingObject>? = null
    var updatesRequired: UpdateableOrganizer? = null
    var beingRendered = false

    /* Convenience methods. Assume it is loaded */
    fun getBlock(xTile: Int, yTile: Int) = blocks!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]

    fun getTile(xTile: Int, yTile: Int) = tiles!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]
    fun setTile(tile: Tile) {
        tiles!![(tile.xTile - xTile) + (tile.yTile - yTile) * CHUNK_SIZE_TILES] = tile
        /* Don't bother checking if it requires an update */
    }

    fun setBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile) {
        blocks!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = block
        if (block.requiresUpdate)
            updatesRequired!!.add(block)
    }

    fun removeBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile) {
        blocks!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = null
        if (block.requiresUpdate)
            updatesRequired!!.remove(block)
    }

    fun addMoving(m: MovingObject) {
        moving!!.add(m)
        moving!!.sortedBy { it.yPixel }
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
        val o = updatesRequired!!
        if (o.size > 0) {
            o.forEach { it.update() }
        } else if (!beingRendered) {
            unload()
        }
    }

    fun load(blocks: Array<Block?>, tiles: Array<Tile>) {
        this.blocks = blocks
        this.tiles = tiles
        this.moving = mutableListOf()
        this.updatesRequired = UpdateableOrganizer()
        this.movingOnBoundary = mutableListOf()
        loaded = true
        parent.loadedChunks.add(this)
    }

    fun unload() {
        blocks = null
        tiles = null
        moving = null
        updatesRequired = null
        loaded = false
        movingOnBoundary = null
        parent.loadedChunks.remove(this)
    }

    override fun toString(): String {
        return "Chunk at $xChunk, $yChunk, loaded: $loaded"
    }

    override fun equals(other: Any?): Boolean {
        return other is Chunk && other.xChunk == xChunk && other.yChunk == this.yChunk
    }
}