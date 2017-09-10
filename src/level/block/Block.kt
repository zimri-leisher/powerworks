package level.block

import graphics.Renderer
import level.Collidable
import main.Game
import java.io.DataOutputStream

class Block(xTile: Int, yTile: Int, val type: BlockType) : Collidable(xTile shl 4, yTile shl 4, type.requiresUpdate, type.hitbox) {

    val rotation = 0

    override fun render() {
        Renderer.renderTexture(type.getTexture(rotation), xPixel + type.textureXPixelOffset, yPixel + type.textureYPixelOffset)
    }

    override fun getCollision(moveX: Int, moveY: Int): Boolean {
        val c = Game.currentLevel.getChunk(xChunk, yChunk)
        for(x in xTile until xTile + type.widthTiles) {
            for(y in yTile until yTile + type.heightTiles) {
                if(c.getBlock(x, y) != null) {
                    return true
                }
            }
        }
        return super.getCollision(moveX, moveY)
    }

    override fun save(out: DataOutputStream) {
        super.save(out)
        out.writeInt(type.id)
    }

}