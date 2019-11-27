package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import data.ConcurrentlyModifiableMutableList
import level.block.Block
import level.moving.MovingObject
import level.tile.Tile
import resource.ResourceNode

/**
 * This is just for holding data. Interaction with the level should be done through the [Level] object and not any of these
 */
class Chunk(
        @Tag(1)
        val xChunk: Int,
        @Tag(2)
        val yChunk: Int) {

    @Tag(4)
    var xTile = xChunk shl 3
    @Tag(5)
    var yTile = yChunk shl 3
    var beingRendered = false

    @Tag(6)
    var data = ChunkData()

    fun getBlock(xTile: Int, yTile: Int) = data.blocks[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]

    fun getTile(xTile: Int, yTile: Int) = data.tiles[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]
    fun setTile(tile: Tile) {
        data.tiles[(tile.xTile - xTile) + (tile.yTile - yTile) * CHUNK_SIZE_TILES] = tile
        data.modifiedTiles.add(tile)
        /* Don't bother checking if it requires an update */
    }

    fun setBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile, mainBlock: Boolean) {
        data.blocks[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = block
        if (mainBlock && block.requiresUpdate)
            addUpdateRequired(block)
    }

    fun removeBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile, mainBlock: Boolean) {
        data.blocks[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = null
        if (mainBlock && block.requiresUpdate)
            removeUpdateRequired(block)
    }

    fun addDroppedItem(d: DroppedItem) {
        data.droppedItems.add(d)
        addMoving(d)
    }

    fun removeDroppedItem(d: DroppedItem) {
        data.droppedItems.remove(d)
        removeMoving(d)
    }

    fun addMoving(m: MovingObject) {
        data.moving.add(m)
        data.moving.sortWith(Comparator { o1, o2 -> o1.yPixel.compareTo(o2.yPixel) })
        if (m.requiresUpdate) {
            addUpdateRequired(m)
        }
    }

    fun removeMoving(m: MovingObject) {
        data.moving.remove(m)
        if (m.requiresUpdate)
            removeUpdateRequired(m)
    }

    fun addResourceNode(r: ResourceNode) {
        data.resourceNodes.elementAt(r.resourceCategory.ordinal).add(r)
    }

    fun removeResourceNode(r: ResourceNode) {
        data.resourceNodes.elementAt(r.resourceCategory.ordinal).remove(r)
    }

    fun addUpdateRequired(levelObject: LevelObject) {
        data.updatesRequired.add(levelObject)
    }

    fun removeUpdateRequired(levelObject: LevelObject) {
        data.updatesRequired.remove(levelObject)
    }

    fun update() {
        for (nodeGroup in data.resourceNodes) {
            for (node in nodeGroup) {
                node.update()
            }
        }
        val o = data.updatesRequired
        if (o.size > 0) {
            o.forEach { it.update() }
        }
    }

    override fun toString(): String {
        return "Chunk at $xChunk, $yChunk"
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

data class ChunkData(
        // we don't send this one because it can just be generated from the seed
        var tiles: Array<Tile> = arrayOf(),
        @Tag(2)
        var modifiedTiles: MutableList<Tile> = mutableListOf(),
        @Tag(3)
        var blocks: Array<Block?> = arrayOf(),
        @Tag(4)
        var moving: MutableList<MovingObject> = mutableListOf(),
        @Tag(5)
        var movingOnBoundary: MutableList<MovingObject> = mutableListOf(),
        @Tag(6)
        var updatesRequired: ConcurrentlyModifiableMutableList<LevelObject> = ConcurrentlyModifiableMutableList(),
        @Tag(7)
        var droppedItems: MutableList<DroppedItem> = mutableListOf(),
        @Tag(8)
        var resourceNodes: MutableList<MutableList<ResourceNode>> = mutableListOf()) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkData

        if (!tiles.contentEquals(other.tiles)) return false
        if (modifiedTiles != other.modifiedTiles) return false
        if (!blocks.contentEquals(other.blocks)) return false
        if (moving != other.moving) return false
        if (movingOnBoundary != other.movingOnBoundary) return false
        if (updatesRequired != other.updatesRequired) return false
        if (droppedItems != other.droppedItems) return false
        if (resourceNodes != other.resourceNodes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tiles.contentHashCode()
        result = 31 * result + modifiedTiles.hashCode()
        result = 31 * result + blocks.contentHashCode()
        result = 31 * result + moving.hashCode()
        result = 31 * result + movingOnBoundary.hashCode()
        result = 31 * result + updatesRequired.hashCode()
        result = 31 * result + droppedItems.hashCode()
        result = 31 * result + resourceNodes.hashCode()
        return result
    }
}