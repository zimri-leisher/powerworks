package level

import data.ConcurrentlyModifiableMutableList
import level.block.Block
import level.moving.MovingObject
import level.tile.Tile
import resource.ResourceNode
import java.util.*
import kotlin.Comparator

/**
 * This is just for holding data. Interaction with the level should be done through the Level object and not any of these
 */
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
    var resourceNodes: MutableList<MutableList<ResourceNode>>? = null
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
            addUpdateRequired(block)
    }

    fun removeBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile, mainBlock: Boolean) {
        blocks!![(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = null
        if (mainBlock && block.requiresUpdate)
            removeUpdateRequired(block)
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
        moving!!.sortWith(Comparator { o1, o2 -> o1.yPixel.compareTo(o2.yPixel) })
        if (m.requiresUpdate) {
            addUpdateRequired(m)
        }
    }

    fun removeMoving(m: MovingObject) {
        moving!!.remove(m)
        if (m.requiresUpdate)
            removeUpdateRequired(m)
    }

    fun addResourceNode(r: ResourceNode) {
        resourceNodes!!.elementAt(r.resourceCategory.ordinal).add(r)
    }

    fun removeResourceNode(r: ResourceNode) {
        resourceNodes!!.elementAt(r.resourceCategory.ordinal).remove(r)
    }

    fun addUpdateRequired(levelObject: LevelObject) {
        updatesRequired!!.add(levelObject)
    }

    fun removeUpdateRequired(levelObject: LevelObject) {
        updatesRequired!!.remove(levelObject)
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
        this.moving = Collections.synchronizedList(mutableListOf())
        this.updatesRequired = ConcurrentlyModifiableMutableList()
        this.movingOnBoundary = Collections.synchronizedList(mutableListOf())
        this.droppedItems = mutableListOf()
        this.resourceNodes = Collections.synchronizedList(mutableListOf(
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
        ))
        loaded = true
        parent.loadedChunks.add(this)
    }

    fun unload() {
        blocks = null
        tiles = null
        if (moving != null) {
            synchronized(moving!!) {
                moving = null
            }
        }
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

    override fun hashCode(): Int {
        var result = xChunk
        result = 31 * result + yChunk
        return result
    }
}