package level

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.PressType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import main.DebugCode
import main.Game
import java.io.DataOutputStream

abstract class LevelObject protected constructor(
        open val type: LevelObjectType<out LevelObject>,
        open val xPixel: Int, open val yPixel: Int,
        rotation: Int = 0,
        /**
         * Should be the default (unrotated) instance of the hitbox.
         */
        hitbox: Hitbox = type.hitbox,
        requiresUpdate: Boolean = type.requiresUpdate,
        /**
         * Whether or not the INTERACTOR tool should allow clicking on this
         */
        var isInteractable: Boolean = true) {

    open val xTile = xPixel shr 4
    open val yTile = yPixel shr 4
    open val xChunk = xTile shr CHUNK_TILE_EXP
    open val yChunk = yTile shr CHUNK_TILE_EXP

    var mouseOn = false
        set(value) {
            if (value && !field) {
                onMouseEnter()
                field = value
            } else if (!value && field) {
                field = value
                onMouseLeave()
            }
        }

    var hitbox = Hitbox.rotate(hitbox, rotation)
        private set

    var rotation = rotation
        set(value) {
            if (field != value) {
                field = value
                hitbox = Hitbox.rotate(type.hitbox, value)
            }
        }

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
            val c = Level.Chunks.get(xChunk, yChunk)
            if (field && !value) {
                c.removeUpdateRequired(this)
            } else if (!field && value) {
                c.addUpdateRequired(this)
            }
        }

    open fun render() {
        if (type.requiresRender) {
            type.textures.render(this)
            if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
                renderHitbox()
        }
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
        Renderer.renderFilledRectangle(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height, TextureRenderParams(color = Color(0xFF0010)))
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

    /**
     * Checks if this level object would have a collision at the given coordinates, excludes all collisions that would happen with a level object not matching the given predicate
     * @param predicate if null, all level objects are checked
     * */
    open fun getCollision(xPixel: Int, yPixel: Int, predicate: ((LevelObject) -> Boolean)? = null): LevelObject? {
        if (hitbox == Hitbox.NONE)
            return null
        return Level.getCollision(this, xPixel, yPixel, predicate)
    }

    /**
     * When the mouse is clicked on this
     */
    open fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
    }

    /**
     * When the mouse is scrolled and is over this
     */
    open fun onScroll(dir: Int) {
    }

    /**
     * When the mouse enters the rectangle defined by xPixel, yPixel, widthPixels, heightPixels. Called even if it's on the bottom
     */
    open fun onMouseEnter() {
    }

    /**
     * When the mouse leaves the rectangle defined by xPixel, yPixel, widthPixels, heightPixels. Called even if it's on the bottom layer
     */
    open fun onMouseLeave() {
    }

    /**
     * Should write all
     */
    open fun save(out: DataOutputStream) {
        out.writeInt(type.id)
        out.writeInt(xPixel)
        out.writeInt(yPixel)
        out.writeInt(rotation)
    }

}