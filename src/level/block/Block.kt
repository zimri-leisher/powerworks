package level.block

import graphics.Renderer
import level.CHUNK_TILE_EXP
import level.Hitbox
import level.LevelObject
import main.Game
import java.io.DataOutputStream

abstract class Block(open val type: BlockType, yTile: Int, xTile: Int, hitbox: Hitbox = type.hitbox, requiresUpdate: Boolean = type.requiresUpdate) : LevelObject(xTile shl 4, yTile shl 4, hitbox, requiresUpdate) {

    val rotation = 0

    override fun onAddToLevel() {
        for (y in -1..1) {
            for (x in -1..1) {
                if (Math.abs(x) != Math.abs(y))
                    Game.currentLevel.getBlock(xTile + x, yTile + y)?.onAdjacentBlockAdd(this)
            }
        }
    }

    override fun onRemoveFromLevel() {
        for (y in -1..1) {
            for (x in -1..1) {
                if (Math.abs(x) != Math.abs(y))
                    Game.currentLevel.getBlock(xTile + x, yTile + y)?.onAdjacentBlockRemove(this)
            }
        }
    }

    override fun render() {
        Renderer.renderTexture(type.getTexture(rotation), xPixel - type.textureXPixelOffset, yPixel - type.textureYPixelOffset)
        super.render()
    }

    /**
     * When an adjacent block is removed
     */
    open fun onAdjacentBlockRemove(b: Block) {

    }

    /**
     * When an adjacent block is added
     */
    open fun onAdjacentBlockAdd(b: Block) {

    }

    override fun getCollision(xPixel: Int, yPixel: Int, predicate: ((LevelObject) -> Boolean)?): LevelObject? {
        // Check if a block is already present
        val nXTile = xPixel shr 4
        val nYTile = yPixel shr 4
        for (x in nXTile until (nXTile + type.widthTiles)) {
            for (y in nYTile until (nYTile + type.heightTiles)) {
                val c = Game.currentLevel.getChunk(x shr CHUNK_TILE_EXP, y shr CHUNK_TILE_EXP)
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
        // Checks for moving objects. Don't worry about blocks, because no block has a hitbox of over a tile
        return Game.currentLevel.getMovingObjectCollision(this, xPixel, yPixel, predicate)
    }

    override fun save(out: DataOutputStream) {
        super.save(out)
        out.writeInt(rotation)
        out.writeInt(type.id)
    }

    override fun toString(): String {
        return "Block at $xTile, $yTile, type: $type"
    }

    override fun equals(other: Any?): Boolean {
        return other is Block && other.xTile == xTile && other.yTile == yTile && other.type == type
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + rotation
        result = 31 * result + xTile
        result = 31 * result + yTile
        return result
    }

}