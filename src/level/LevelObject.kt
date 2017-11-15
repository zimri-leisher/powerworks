package level

import graphics.Renderer
import main.Game
import java.io.DataOutputStream

abstract class LevelObject protected constructor(open val xPixel: Int, open val yPixel: Int, val hitbox: Hitbox, requiresUpdate: Boolean = true) {

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

    open fun onCollide(o: LevelObject) {
    }

    /** X and Y pixel are where to check if this would collide with */
    open fun getCollision(xPixel: Int, yPixel: Int, predicate: ((LevelObject) -> Boolean)? = null): LevelObject? {
        if (hitbox == Hitbox.NONE)
            return null
        return Game.currentLevel.getCollision(this, xPixel, yPixel, predicate)
    }

    open fun save(out: DataOutputStream) {
        out.writeInt(xPixel)
        out.writeInt(yPixel)
    }
}