package level

import graphics.Renderer
import main.Game
import java.io.DataOutputStream

abstract class LevelObject protected constructor(xPixel: Int, yPixel: Int, val hitbox: Hitbox, requiresUpdate: Boolean = true) {

    open val xPixel: Int = xPixel
    open val yPixel: Int = yPixel
    open val xTile = xPixel shr 4
    open val yTile = yPixel shr 4
    open val xChunk = xTile shr 3
    open val yChunk = yTile shr 3

    var requiresUpdate: Boolean = requiresUpdate
        set(value) {
            val c = Game.currentLevel.getChunk(xChunk, yChunk)
            if (field && !value) {
                c.updatesRequired!!.remove(this)
            } else if (!field && value) {
                c.updatesRequired!!.add(this)
            }
        }

    open fun render() {
        if(Game.RENDER_HITBOXES)
            renderHitbox()
    }

    protected fun renderHitbox() {
        Renderer.renderFilledRectangle(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height, 0xFF0010)
    }

    open fun update() {

    }

    /** X and Y pixel are where to check if this would collide with */
    open fun getCollision(xPixel: Int, yPixel: Int): Boolean {
        if (hitbox == Hitbox.NONE)
            return false
        return Game.currentLevel.getCollision(this, xPixel, yPixel)
    }

    open fun save(out: DataOutputStream) {
        out.writeInt(xPixel)
        out.writeInt(yPixel)
    }
}