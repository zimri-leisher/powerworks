package level

import data.ConcurrentlyModifiableMutableList
import level.block.Block
import level.moving.MovingObject
import level.tile.Tile
import resource.ResourceNode
import serialization.Id
import serialization.Sparse

/**
 * This is just for holding data. Interaction with the level should be done through the [Level] and [LevelUpdate]s, not any of these
 */
data class Chunk(
    @Id(1)
    val xChunk: Int,
    @Id(2)
    val yChunk: Int
) {

    private constructor() : this(0, 0)

    @Id(4)
    var xTile = xChunk shl 3

    @Id(5)
    var yTile = yChunk shl 3
    var beingRendered = false

    @Id(6)
    var data = ChunkData()

    fun getBlock(xTile: Int, yTile: Int) = data.blocks[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]
    fun getResourceNode(xTile: Int, yTile: Int) =
        data.resourceNodes[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]

    fun getTile(xTile: Int, yTile: Int) = data.tiles[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES]
    fun setTile(tile: Tile) {
        data.tiles[(tile.xTile - xTile) + (tile.yTile - yTile) * CHUNK_SIZE_TILES] = tile
        data.modifiedTiles.add(tile)
    }

    fun setBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile, mainBlock: Boolean) {
        data.blocks[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = block
        if (mainBlock && block.type.requiresUpdate)
            addUpdateRequired(block)
    }

    fun removeBlock(block: Block, xTile: Int = block.xTile, yTile: Int = block.yTile, mainBlock: Boolean) {
        data.blocks[(xTile - this.xTile) + (yTile - this.yTile) * CHUNK_SIZE_TILES] = null
        if (mainBlock && block.type.requiresUpdate)
            removeUpdateRequired(block)
    }

    fun addMoving(m: MovingObject) {
        data.moving.add(m)
        data.moving.sortWith { o1, o2 -> o1.y.compareTo(o2.y) }
        if (m.type.requiresUpdate) {
            addUpdateRequired(m)
        }
    }

    fun removeMoving(m: MovingObject) {
        data.moving.remove(m)
        if (m.type.requiresUpdate)
            removeUpdateRequired(m)
    }

    fun addResourceNode(r: ResourceNode) {
        data.resourceNodes[(r.xTile - this.xTile) + (r.yTile - this.yTile) * CHUNK_SIZE_TILES] = r
    }

    fun removeResourceNode(r: ResourceNode) {
        data.resourceNodes[(r.xTile - this.xTile) + (r.yTile - this.yTile) * CHUNK_SIZE_TILES] = null
    }

    fun addUpdateRequired(levelObject: PhysicalLevelObject) {
        data.updatesRequired.add(levelObject)
    }

    fun removeUpdateRequired(levelObject: PhysicalLevelObject) {
        data.updatesRequired.remove(levelObject)
    }

    fun update() {
        for (node in data.resourceNodes) {
            if (node != null) {
//                node.update()
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
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Chunk

        if (xChunk != other.xChunk) return false
        if (yChunk != other.yChunk) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xChunk
        result = 31 * result + yChunk
        result = 31 * result + data.hashCode()
        return result
    }

}

data class ChunkData(
    // we don't send this one because it can just be generated from the seed
    var tiles: Array<Tile> = arrayOf(),
    @Id(2)
    var modifiedTiles: MutableList<Tile> = mutableListOf(),
    @Id(3)
    @Sparse
    var blocks: Array<Block?> = arrayOf(),
    @Id(4)
    var moving: MutableList<MovingObject> = mutableListOf(),
    @Id(5)
    var movingOnBoundary: MutableList<MovingObject> = mutableListOf(),
    @Id(6)
    var updatesRequired: ConcurrentlyModifiableMutableList<PhysicalLevelObject> = ConcurrentlyModifiableMutableList(),
    @Id(8)
    @Sparse
    var resourceNodes: Array<ResourceNode?> = arrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkData

        if (modifiedTiles != other.modifiedTiles) return false
        if (!blocks.contentEquals(other.blocks)) return false
        if (moving != other.moving) return false
        if (movingOnBoundary != other.movingOnBoundary) return false
        if (updatesRequired != other.updatesRequired) return false
        if (!resourceNodes.contentEquals(other.resourceNodes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = modifiedTiles.hashCode()
        result = 31 * result + blocks.contentHashCode()
        result = 31 * result + moving.hashCode()
        result = 31 * result + movingOnBoundary.hashCode()
        result = 31 * result + updatesRequired.hashCode()
        result = 31 * result + resourceNodes.contentHashCode()
        return result
    }

}