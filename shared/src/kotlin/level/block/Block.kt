package level.block

import level.Level
import level.PhysicalLevelObject
import level.getBlockAtTile
import level.getMovingObjectCollisions
import level.particle.ParticleEffect
import network.BlockReference
import network.LevelObjectReference
import resource.ResourceNode
import serialization.Id

abstract class Block(type: BlockType<out Block>, xTile: Int, yTile: Int) :
    PhysicalLevelObject(type, xTile shl 4, yTile shl 4) {

    override val type = type

    /**
     * The reason this exists and the render method doesn't use the textures defined in the [BlockType] is because this allows for us to
     * have animations that are local to this block (that aren't in sync with every other animation of the same type)
     */
    @Id(17)
    val textures = type.textures

    /**
     * The specific local nodes as specified by [BlockType.nodesTemplate]
     *
     * For example, for a block of type [MachineBlockType.MINER], this would consist of 1 node at (0, 0) relative, pointing up by default. It automatically takes rotation
     * into account, so, for example, if that same block were placed with a rotation of 1 (rotated 90 degrees clockwise), the node would be at (1, 0) relative, pointing right.
     */
    @Id(18)
    var nodes = listOf<ResourceNode>()

    open fun createNodes(): List<ResourceNode> {
        return listOf()
    }

    override fun onRotate(oldDir: Int) {
        super.onRotate(oldDir)
        nodes.forEach { level.remove(it) }
        nodes = createNodes()
        nodes.forEach { level.add(it) }
    }

    fun getAdjacentBlocks(): Set<Block> {
        val ret = mutableSetOf<Block>()
        // this one covers the strip of blocks above and below this block
        // includes corners
        for (w in -1..type.widthTiles) {
            val blockAbove = level.getBlockAtTile(xTile + w, yTile - 1)
            if (blockAbove != null && blockAbove != this) {
                ret.add(blockAbove)
            }
            val blockBelow = level.getBlockAtTile(xTile + w, yTile + type.heightTiles + 1)
            if (blockBelow != null && blockBelow != this) {
                ret.add(blockBelow)
            }
        }
        // covers strip of blocks to left and right, exclude corners
        for (h in 0 until type.heightTiles) {
            val blockLeft = level.getBlockAtTile(xTile - 1, yTile + h)
            if (blockLeft != null && blockLeft != this) {
                ret.add(blockLeft)
            }
            val blockRight = level.getBlockAtTile(xTile + type.widthTiles + 1, yTile + h)
            if (blockRight != null && blockRight != this) {
                ret.add(blockRight)
            }
        }
        return ret
    }

    /**
     * Don't forget to call super.onAddToLevel() in subclasses overriding this so that the [onAdjacentBlockAdd] methods of adjacent blocks are called
     */
    override fun afterAddToLevel(oldLevel: Level) {
        ParticleEffect.BLOCK_PLACE.instantiate(this)
        nodes = createNodes()
        nodes.forEach { level.add(it) }
        getAdjacentBlocks().forEach {
            it.onAdjacentBlockAdd(this)
        }
    }

    /**
     * Don't forget to call super.onRemoveFromLevel() in subclasses overriding this so that the onAdjacentBlockRemove methods of adjacent blocks are called
     */
    override fun afterRemoveFromLevel(oldLevel: Level) {
        nodes.forEach { oldLevel.remove(it) }
        getAdjacentBlocks().forEach {
            it.onAdjacentBlockRemove(this)
        }
        type.guiPool?.close(this)
    }

    /**
     * Called when an adjacent block is removed from the level
     */
    open fun onAdjacentBlockRemove(b: Block) {

    }

    /**
     * Called when an adjacent block is added to the level
     */
    open fun onAdjacentBlockAdd(b: Block) {

    }

    override fun getCollisions(x: Int, y: Int, level: Level): Sequence<PhysicalLevelObject> {
        // Check if a block is already present
        val nXTile = x shr 4
        val nYTile = y shr 4
        return level.getMovingObjectCollisions(hitbox, x, y) +
                (nXTile until (nXTile + type.widthTiles)).asSequence()
                    .flatMap { x ->
                        (nYTile until (nYTile + type.heightTiles)).asSequence().map { x to it }
                    }
                    .map { level.getBlockAtTile(it.first, it.second) }
                    .filterNotNull()
                    .filter { it !== this }
    }

    override fun toReference(): BlockReference {
        return BlockReference(this)
    }

    override fun toString(): String {
        return "${javaClass.simpleName} at $xTile, $yTile, type: $type"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Block

        if (textures != other.textures) return false
        if (nodes.size != other.nodes.size) return false
        for (node in nodes) {
            if (node !in other.nodes) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + textures.hashCode()
        result = 31 * result + nodes.hashCode()
        return result
    }
}