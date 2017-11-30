package level

import graphics.Renderer
import main.Game
import java.io.DataOutputStream

abstract class LevelObject protected constructor(open val xPixel: Int, open val yPixel: Int, val hitbox: Hitbox, requiresUpdate: Boolean = true) {

    open val xTile = xPixel shr 4
    open val yTile = yPixel shr 4
    open val xChunk = xTile shr 3
    open val yChunk = yTile shr 3

    /**
     * If this has been added to the level
     */
    open var inLevel = false
        set(value) {
            if (field && !value) {
                field = value
                onRemoveFromLevel()
            } else if (!field && value) {
                field = value
                onAddToLevel()
            }
        }

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
        if (Game.RENDER_HITBOXES)
            renderHitbox()
    }

    /**
     * When this gets put in the level
     * Called when inLevel is changed to true, should usually be from Level.add
     */
    open fun onAddToLevel() {
    }

    /**
     * When this gets taken out of the level
     * Called when inLevel is changed to false, should usually be from Level.remove
     */
    open fun onRemoveFromLevel() {
    }

    protected fun renderHitbox() {
        Renderer.renderFilledRectangle(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height, 0xFF0010)
    }

    /**
     * Only called if type.requiresUpdate is true
     */
    open fun update() {
    }

    /**
     * Called if this collides with something or something else collides with it
     */
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