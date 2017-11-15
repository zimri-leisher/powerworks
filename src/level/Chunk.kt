package level

import level.block.Block
import level.moving.MovingObject
import level.node.InputNode
import level.node.OutputNode
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
    var droppedItem: MutableList<DroppedItem>? = null
    // One list for each resource type
    var outputNodes: Array<MutableList<OutputNode<*>>>? = null
    var inputNodes: Array<MutableList<InputNode<*>>>? = null
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

    fun addDroppedItem(d: DroppedItem) {
        droppedItem!!.add(d)
        addMoving(d)
    }

    fun removeDroppedItem(d: DroppedItem) {
        droppedItem!!.remove(d)
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

    fun addInputNode(i: InputNode<*>) {
        inputNodes!![i.resourceTypeID].add(i)
    }

    fun removeInputNode(i: InputNode<*>) {
        inputNodes!![i.resourceTypeID].remove(i)
    }

    fun addOutputNode(o: OutputNode<*>) {
        outputNodes!![o.resourceTypeID].add(o)
    }

    fun removeOutputNode(o: OutputNode<*>) {
        outputNodes!![o.resourceTypeID].remove(o)
    }

    fun update() {
        /* Assume is already loaded */
        val o = updatesRequired!!
        if (o.size > 0) {
            o.forEach { it.update() }
        } else if (!beingRendered && inputNodes!!.all { it.isEmpty() } && outputNodes!!.all { it.isEmpty() }) {
            unload()
        }
    }

    fun load(blocks: Array<Block?>, tiles: Array<Tile>) {
        this.blocks = blocks
        this.tiles = tiles
        this.moving = mutableListOf()
        this.updatesRequired = UpdateableOrganizer()
        this.movingOnBoundary = mutableListOf()
        this.droppedItem = mutableListOf()
        this.outputNodes = arrayOf(
                mutableListOf()
        )
        this.inputNodes = arrayOf(
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
        droppedItem = null
        outputNodes = null
        inputNodes = null
        parent.loadedChunks.remove(this)
    }

    override fun toString(): String {
        return "Chunk at $xChunk, $yChunk, loaded: $loaded"
    }

    override fun equals(other: Any?): Boolean {
        return other is Chunk && other.xChunk == xChunk && other.yChunk == this.yChunk
    }
}