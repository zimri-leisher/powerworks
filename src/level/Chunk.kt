package level

import level.block.Block
import level.moving.MovingObject
import resource.ResourceNode
import level.tile.Tile
import misc.ConcurrentlyModifiableMutableList

class Chunk(val parent: Level, val xChunk: Int, val yChunk: Int) {

    var xTile = xChunk shl 3
    var yTile = yChunk shl 3
    var loaded = false
    var tiles: Array<Tile>? = null
    var blocks: Array<Block?>? = null
    var moving: MutableList<MovingObject>? = null
    var movingOnBoundary: MutableList<MovingObject>? = null
    var updatesRequired: ConcurrentlyModifiableMutableList<LevelObject>? = null
    var droppedItems: MutableList<DroppedItem>? = null
    var resourceNodes: Array<MutableList<ResourceNode<*>>>? = null
    var beingRendered = false

    /* Convenience methods. Assume it is loaded */
    fun getBlock(xTile: Int, yTile: Int) = blocks!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]

    fun getTile(xTile: Int, yTile: Int) = tiles!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]
    fun setTile(tile: Tile) {
        tiles!![(tile.xTile - xTile) + (tile.yTile - yTile) * CHUNK_SIZE_TILES] = tile
        /* Don't bother checking if it requires an update */
    }

    fun setBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile, mainBlock: Boolean) {
        blocks!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = block
        if (mainBlock && block.requiresUpdate)
            updatesRequired!!.add(block)
    }

    fun removeBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile, mainBlock: Boolean) {
        blocks!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = null
        if (mainBlock && block.requiresUpdate)
            updatesRequired!!.remove(block)
    }

    fun addDroppedItem(d: DroppedItem) {
        droppedItems!!.add(d)
        addMoving(d)
    }

    fun removeDroppedItem(d: DroppedItem) {
        droppedItems!!.remove(d)
        removeMoving(d)
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

    fun addResourceNode(r: ResourceNode<*>) {
        resourceNodes!![r.resourceTypeID].add(r)
    }

    fun removeResourceNode(r: ResourceNode<*>) {
        resourceNodes!![r.resourceTypeID].remove(r)
    }

    fun update() {
        /* Assume is already loaded */
        val o = updatesRequired!!
        if (o.size > 0) {
            o.forEach { it.update() }
        }
    }

    fun load(blocks: Array<Block?>, tiles: Array<Tile>) {
        this.blocks = blocks
        this.tiles = tiles
        this.moving = mutableListOf()
        this.updatesRequired = ConcurrentlyModifiableMutableList()
        this.movingOnBoundary = mutableListOf()
        this.droppedItems = mutableListOf()
        this.resourceNodes = arrayOf(
                mutableListOf()
        )
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
        droppedItems = null
        resourceNodes = null
        parent.loadedChunks.remove(this)
    }

    override fun toString(): String {
        return "Chunk at $xChunk, $yChunk, loaded: $loaded"
    }

    override fun equals(other: Any?): Boolean {
        return other is Chunk && other.xChunk == xChunk && other.yChunk == this.yChunk
    }
}