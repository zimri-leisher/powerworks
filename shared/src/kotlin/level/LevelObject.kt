package level

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.PressType
import main.DebugCode
import main.Game
import network.LevelObjectReference
import player.team.Team
import serialization.Id
import serialization.Input
import serialization.Output
import serialization.Serializer
import java.util.*

abstract class LevelObject protected constructor(
        type: LevelObjectType<out LevelObject>,
        open val xPixel: Int,
        open val yPixel: Int,
        rotation: Int = 0,
        /**
         * Whether or not the [Interactor] tool should allow clicking on this
         */
        @Id(4)
        var isInteractable: Boolean = true) {

    private constructor() : this(LevelObjectType.ERROR, 0, 0)

    open val type = type

    @Id(5)
    var id = UUID.randomUUID()!!

    @Id(6)
    var level: Level = LevelManager.EMPTY_LEVEL
        set(value) {
            if (field != value) {
                if (inLevel) {
                    // on remove from old level
                    onRemoveFromLevel()
                }
                field = value
                if (field == LevelManager.EMPTY_LEVEL) {
                    inLevel = false
                } else if (inLevel) {
                    // on add to new level
                    onAddToLevel()
                }
            }
        }

    @Id(8)
    open val xTile = xPixel shr 4

    @Id(9)
    open val yTile = yPixel shr 4

    @Id(11)
    open val xChunk = xTile shr CHUNK_TILE_EXP

    @Id(12)
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

    @Id(13)
    var hitbox = type.hitbox
        set(value) {
            if (field != value) {
                onHitboxChange()
                field = value
            }
        }

    @Id(14)
    var rotation = rotation
        set(value) {
            if (field != value) {
                field = value
                //hitbox = Hitbox.rotate(type.hitbox, value)
            }
        }

    @Id(991)
    var health = type.maxHealth

    @Id(992)
    var team = Team.NEUTRAL

    /**
     * If this has been added to a [Level] (one that isn't [LevelManager.EMPTY_LEVEL])
     */
    @Id(15)
    open var inLevel = false
        set(value) {
            if (field && !value) {
                field = value
                onRemoveFromLevel()
            } else if (!field && value) {
                if (level == LevelManager.EMPTY_LEVEL) {
                    throw IllegalStateException("Cannot add a LevelObject to the empty level")
                }
                field = value
                onAddToLevel()
            }
        }

    @Id(16)
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
        if (health != type.maxHealth) {

        }
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES)
            renderHitbox()
    }

    /**
     * When this gets put in the level
     * Called when [inLevel] is changed to true, should usually be from Level.add
     */
    open fun onAddToLevel() {
    }

    /**
     * When this gets taken out of the level
     * Called when [inLevel] is changed to false, should usually be from Level.remove
     */
    open fun onRemoveFromLevel() {
    }

    protected fun renderHitbox() {
        Renderer.renderFilledRectangle(xPixel + hitbox.xStart, yPixel + hitbox.yStart, hitbox.width, hitbox.height, TextureRenderParams(color = Color(0xFF0010)))
    }

    open fun onHitboxChange() {
    }

    /**
     * Only called if [requiresUpdate] is true
     */
    open fun update() {
    }

    /**
     * Called if this collides with something or something else collides with it
     */
    open fun onCollide(o: LevelObject) {
    }

    /**
     * @return a sequence of [LevelObject]s that would collide with this [LevelObject] if it were at [xPixel], [yPixel] in the
     * given [level] (defaults to [LevelObject.level])
     */
    open fun getCollisions(xPixel: Int, yPixel: Int, level: Level = this.level): Sequence<LevelObject> {
        if (hitbox == Hitbox.NONE)
            return emptySequence()
        return level.getCollisionsWith(hitbox, xPixel, yPixel).filter { it !== this }
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
     * When the mouse enters the rectangle defined by xPixel, yPixel, widthPixels, heightPixels. Called even if it's on the bottom layer
     */
    open fun onMouseEnter() {
    }

    /**
     * When the mouse leaves the rectangle defined by xPixel, yPixel, widthPixels, heightPixels. Called even if it's on the bottom layer
     */
    open fun onMouseLeave() {
    }

    abstract fun toReference(): LevelObjectReference

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelObject

        if (xPixel != other.xPixel) return false
        if (yPixel != other.yPixel) return false
        if (type != other.type) return false
        if (hitbox != other.hitbox) return false
        if (rotation != other.rotation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xPixel
        result = 31 * result + yPixel
        result = 31 * result + type.hashCode()
        result = 31 * result + hitbox.hashCode()
        result = 31 * result + rotation
        return result
    }
}

class LevelObjectSerializer<R : LevelObject> : Serializer.Tagged<R>(false) {

    override fun write(obj: Any, output: Output) {
        obj as LevelObject
        output.write(obj.type)
        output.writeInt(obj.xPixel)
        output.writeInt(obj.yPixel)
        output.writeInt(obj.rotation)
        super.write(obj, output)
    }

    override fun instantiate(input: Input): R {
        val levelObjectType = input.read(LevelObjectType::class.java)
        return levelObjectType.instantiate(input.readInt(), input.readInt(), input.readInt()) as R
    }
}