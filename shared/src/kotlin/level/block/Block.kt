package level.block

import level.Level
import level.LevelObject
import level.getBlockAtTile
import level.getMovingObjectCollisions
import level.particle.ParticleEffect
import network.BlockReference
import network.LevelObjectReference
import resource.ResourceNode
import resource.getAttachedContainers
import serialization.Id
import kotlin.math.abs

abstract class Block(type: BlockType<out Block>, xTile: Int, yTile: Int, rotation: Int = 0) : LevelObject(type, xTile shl 4, yTile shl 4, rotation) {

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
    val nodes: MutableList<ResourceNode> = type.nodesTemplate.instantiate(xTile, yTile, rotation, id).toMutableList()

    init {
        containers = nodes.getAttachedContainers()
        containers.forEach { it.attachedLevelObject = this }
    }

    /**
     * Don't forget to call super.onAddToLevel() in subclasses overriding this so that the [onAdjacentBlockAdd] methods of adjacent blocks are called
     */
    override fun onAddToLevel() {
        ParticleEffect.BLOCK_PLACE.instantiate(this)
        nodes.forEach { level.add(it) }
        // loop through each block touching this one, accounting for width and height
        val adjacent = mutableSetOf<Block>()
        for (w in 0 until type.widthTiles) {
            for (h in 0 until type.heightTiles) {
                for (y in -1..1) {
                    for (x in -1..1) {
                        if (abs(x) != abs(y)) {
                            val b = level.getBlockAtTile(xTile + x + w, yTile + y + h)
                            if (b != null && b != this)
                                adjacent.add(b)
                        }
                    }
                }
            }
        }
        adjacent.forEach {
            it.onAdjacentBlockAdd(this)
        }
    }

    /**
     * Don't forget to call super.onRemoveFromLevel() in subclasses overriding this so that the onAdjacentBlockRemove methods of adjacent blocks are called
     */
    override fun onRemoveFromLevel() {
        nodes.forEach { level.remove(it) }
        // loop through each block touching this one, accounting for width and height
        val adjacent = mutableSetOf<Block>()
        for (w in 0 until type.widthTiles) {
            for (h in 0 until type.heightTiles) {
                for (y in -1..1) {
                    for (x in -1..1) {
                        if (abs(x) != abs(y)) {
                            val b = level.getBlockAtTile(xTile + x + w, yTile + y + h)
                            if (b != null && b != this)
                                adjacent.add(b)
                        }
                    }
                }
            }
        }
        adjacent.forEach {
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

    override fun update() {
        super.update()
    }

    override fun getCollisions(x: Int, y: Int, level: Level): Sequence<LevelObject> {
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

    override fun toReference(): LevelObjectReference {
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
        if (containers.size != other.containers.size) return false
        for (container in containers) {
            if (container !in other.containers) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + textures.hashCode()
        result = 31 * result + nodes.hashCode()
        result = 31 * result + containers.hashCode()
        return result
    }
}