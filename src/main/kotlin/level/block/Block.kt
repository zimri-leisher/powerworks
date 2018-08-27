package level.block

import graphics.LocalAnimation
import graphics.Renderer
import level.*
import level.particle.ParticleEffect
import resource.ResourceContainerGroup
import resource.ResourceNodeGroup
import java.io.DataOutputStream

abstract class Block(type: BlockType<out Block>, xTile: Int, yTile: Int, rotation: Int = 0) : LevelObject(type, xTile shl 4, yTile shl 4, rotation) {

    override val type = type

    /**
     * The reason this exists and the render method doesn't use the textures defined in the BlockType is because this allows for us to instantiate LocalAnimations
     * @see LocalAnimation
     */
    val textures: LevelObjectTextures

    /**
     * The specific local nodes as specified by BlockType.nodesTemplate
     *
     * For example, for a block of type MachineBlockType.MINER, this would consist of 1 node at (0, 0) relative, pointing up by default. It automatically takes rotation
     * into account, so, for example, if that same block were placed with a rotation of 1 (rotated 90 degrees clockwise), the node would be at (1, 0) relative, pointing right.
     */
    val nodes = ResourceNodeGroup("Block at $xTile, $yTile, type: $type's node group", type.nodesTemplate.instantiate(xTile, yTile, rotation))
    /**
     * The specific local containers as specified by BlockType.nodesTemplate
     *
     * For example, for a block of type ChestBlockType.SMALL_CHEST, this would consist of a single 8x3 inventory
     */
    val containers = ResourceContainerGroup(nodes.getAttachedContainers())

    init {
        // start local animations
        val newTextures = mutableListOf<LevelObjectTexture>()
        for (texture in type.textures) {
            if (texture.texture is LocalAnimation) {
                newTextures.add(LevelObjectTexture(LocalAnimation(texture.texture.animation, texture.texture.playing, texture.texture.speed), texture.xPixelOffset, texture.yPixelOffset))
            } else {
                newTextures.add(texture)
            }
        }
        textures = LevelObjectTextures(*newTextures.toTypedArray())
    }

    /**
     * Don't forget to call super.onAddToLevel() in subclasses overriding this so that the onAdjacentBlockAdd methods of adjacent blocks are called
     */
    override fun onAddToLevel() {
        ParticleEffect.BLOCK_PLACE.instantiate(this)
        nodes.forEach { Level.add(it) }
        // loop through each block touching this one, accounting for width and height
        val adjacent = mutableSetOf<Block>()
        for (w in 0 until type.widthTiles) {
            for (h in 0 until type.heightTiles) {
                for (y in -1..1) {
                    for (x in -1..1) {
                        if (Math.abs(x) != Math.abs(y)) {
                            val b = Level.Blocks.get(xTile + x + w, yTile + y + h)
                            if (b != null && b != this)
                                adjacent.add(b)
                        }
                    }
                }
            }
        }
        adjacent.forEach { it.onAdjacentBlockAdd(this) }
    }

    /**
     * Don't forget to call super.onRemoveFromLevel() in subclasses overriding this so that the onAdjacentBlockRemove methods of adjacent blocks are called
     */
    override fun onRemoveFromLevel() {
        nodes.forEach { Level.remove(it) }
        // loop through each block touching this one, accounting for width and height
        val adjacent = mutableSetOf<Block>()
        for (w in 0 until type.widthTiles) {
            for (h in 0 until type.heightTiles) {
                for (y in -1..1) {
                    for (x in -1..1) {
                        if (Math.abs(x) != Math.abs(y)) {
                            val b = Level.Blocks.get(xTile + x + w, yTile + y + h)
                            if (b != null && b != this)
                                adjacent.add(b)
                        }
                    }
                }
            }
        }
        adjacent.forEach { it.onAdjacentBlockRemove(this) }
    }

    override fun render() {
        val texture = textures[rotation]
        Renderer.renderTexture(texture.texture, xPixel - texture.xPixelOffset, yPixel - texture.yPixelOffset)
        super.render()

    }

    /**
     * When an adjacent block is removed from the level
     */
    open fun onAdjacentBlockRemove(b: Block) {

    }

    /**
     * When an adjacent block is added to the level
     */
    open fun onAdjacentBlockAdd(b: Block) {

    }

    override fun getCollision(xPixel: Int, yPixel: Int, predicate: ((LevelObject) -> Boolean)?): LevelObject? {
        // Check if a block is already present
        val nXTile = xPixel shr 4
        val nYTile = yPixel shr 4
        for (x in nXTile until (nXTile + type.widthTiles)) {
            for (y in nYTile until (nYTile + type.heightTiles)) {
                val c = Level.Chunks.get(x shr CHUNK_TILE_EXP, y shr CHUNK_TILE_EXP)
                val b = c.getBlock(x, y)
                if (b != null) {
                    if (predicate != null && !predicate(b)) {
                        continue
                    }
                    // check memory because we could be trying to put the same block down at the same place
                    if (b !== this) {
                        return b
                    }
                }
            }
        }
        // Checks for moving objects. Don't worry about blocks
        return Level.MovingObjects.getCollision(this, xPixel, yPixel, predicate)
    }

    override fun save(out: DataOutputStream) {
        super.save(out)
    }

    override fun toString(): String {
        return "Block at $xTile, $yTile, type: $type"
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other.javaClass == this.javaClass && other is Block && other.inLevel == inLevel && other.xTile == xTile && other.yTile == yTile && other.type == type
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + rotation
        result = 31 * result + xTile
        result = 31 * result + yTile
        return result
    }
}