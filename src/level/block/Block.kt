package level.block

import graphics.Renderer
import level.CHUNK_TILE_EXP
import level.Hitbox
import level.LevelObject
import main.Game
import java.io.DataOutputStream

open class Block(xTile: Int, yTile: Int, open val type: BlockType, hitbox: Hitbox = type.hitbox, requiresUpdate: Boolean = type.requiresUpdate) : LevelObject(xTile shl 4, yTile shl 4, hitbox, requiresUpdate) {

    val rotation = 0

    override fun render() {
        Renderer.renderTexture(type.getTexture(rotation), xPixel - type.textureXPixelOffset, yPixel - type.textureYPixelOffset)
        super.render()
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
                    if (predicate != null && !predicate(b))
                        continue
                    if (b != this)
                        return b
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

}