package level

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.PressType
import level.moving.MovingObject
import main.DebugCode
import main.Game
import screen.mouse.Tool.Companion.Interactor
import java.io.DataOutputStream

private var nextId = 0

abstract class LevelObject protected constructor(
        type: LevelObjectType<out LevelObject>,
        open val xPixel: Int, open val yPixel: Int,
        rotation: Int = 0,
        /**
         * Whether or not the [Interactor] tool should allow clicking on this
         */
        var isInteractable: Boolean = true) {

    open val type = type
    val id = nextId++

    var level = LevelManager.emptyLevel

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

    var hitbox = type.hitbox
        set(value) {
            if (field != value) {
                if(this is MovingObject) {
                    level.updateChunkOf(this)
                }
                field = value
            }
        }

    var rotation = rotation
        set(value) {
            if (field != value) {
                field = value
                //hitbox = Hitbox.rotate(type.hitbox, value)
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

    var requiresUpdate = type.requiresUpdate
        set(value) {
            val c = level.getChunkAt(xChunk, yChunk)
            if (field && !value) {
                field = value
                c.removeUpdateRequired(this)
            } else if (!field && value) {
                field = value
                c.addUpdateRequired(this)
            }
        }

    open fun render() {
        type.textures.render(this)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
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
     * @return a set of [LevelObject]s that would collide with this [LevelObject] if it were at [xPixel], [yPixel] in the
     * given [level] (defaults to [LevelObject.level]). Only considers other [LevelObject]s matching the given [predicate]
     */
    open fun getCollisions(xPixel: Int, yPixel: Int, predicate: (LevelObject) -> Boolean = { true }, level: Level = this.level): Set<LevelObject> {
        if (hitbox == Hitbox.NONE)
            return emptySet()
        return level.getCollisionsWith(hitbox, xPixel, yPixel, {it != this && predicate(it)})
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