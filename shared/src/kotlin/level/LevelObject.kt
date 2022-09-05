package level

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.ControlEvent
import main.DebugCode
import main.Game
import main.toColor
import network.LevelObjectReference
import player.team.Team
import resource.ResourceContainerGroup
import serialization.Id
import serialization.Input
import serialization.Output
import serialization.Serializer
import java.util.*

abstract class LevelObject protected constructor(
        type: LevelObjectType<out LevelObject>,
        open val x: Int,
        open val y: Int,
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
                    beforeRemoveFromLevel(field)
                    beforeAddToLevel(value)
                }
                val oldLevel = field
                field = value
                if (field == LevelManager.EMPTY_LEVEL) {
                    inLevel = false
                } else if (inLevel) {
                    // on add to new level
                    afterAddToLevel(value)
                    afterRemoveFromLevel(oldLevel)
                }
            }
        }

    @Id(8)
    open val xTile = x shr 4

    @Id(9)
    open val yTile = y shr 4

    @Id(11)
    open val xChunk = xTile shr CHUNK_TILE_EXP

    @Id(12)
    open val yChunk = yTile shr CHUNK_TILE_EXP

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
        set(value) {
            if (type.damageable) {
                field = value
            }
        }

    @Id(992)
    var team = Team.NEUTRAL

    @Id(993)
    var containers: ResourceContainerGroup = emptyList()

    /**
     * If this has been added to a [Level] (one that isn't [LevelManager.EMPTY_LEVEL])
     */
    @Id(15)
    open var inLevel = false
        set(value) {
            if (field && !value) {
                beforeRemoveFromLevel(level)
                field = false
                afterRemoveFromLevel(level)
            } else if (!field && value) {
                if (level == LevelManager.EMPTY_LEVEL) {
                    throw IllegalStateException("Cannot add a LevelObject to the empty level")
                }
                beforeAddToLevel(level)
                field = true
                afterAddToLevel(level)
            }
        }

    @Id(16)
    var requiresUpdate = type.requiresUpdate
        set(value) {
            val c = level.getChunkAtChunk(xChunk, yChunk)
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
            renderHealthBar()
        }
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES) {
            renderHitbox()
        }
    }

    fun renderHealthBar() {
        Renderer.renderFilledRectangle(x + hitbox.xStart - (14 - hitbox.width) / 2 - 1, y + hitbox.yStart - 7, 16, 7, TextureRenderParams(color = toColor(r = 0.5f, g = 0.5f, b = 0.5f)))
        Renderer.renderFilledRectangle(x + hitbox.xStart - (14 - hitbox.width) / 2, y + hitbox.yStart - 6, (14 * (health.toDouble() / type.maxHealth)).toInt(), 5, TextureRenderParams(color = toColor(g = 1.0f)))
    }

    open fun beforeAddToLevel(newLevel: Level) {
    }

    /**
     * When this gets put in the level
     * Called when [inLevel] is changed to true, should usually be from Level.add
     */
    open fun afterAddToLevel(oldLevel: Level) {
    }

    open fun beforeRemoveFromLevel(newLevel: Level) {
    }

    /**
     * When this gets taken out of the level
     * Called when [inLevel] is changed to false, should usually be from Level.remove
     */
    open fun afterRemoveFromLevel(oldLevel: Level) {
    }

    protected fun renderHitbox() {
        Renderer.renderFilledRectangle(x + hitbox.xStart, y + hitbox.yStart, hitbox.width, hitbox.height, TextureRenderParams(color = Color(0xFF0010)))
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
    open fun onCollide(obj: LevelObject) {
    }

    /**
     * @return a sequence of [LevelObject]s that would collide with this [LevelObject] if it were at [x], [y] in the
     * given [level] (defaults to [LevelObject.level])
     */
    open fun getCollisions(x: Int, y: Int, level: Level = this.level): Sequence<LevelObject> {
        if (hitbox == Hitbox.NONE)
            return emptySequence()
        return level.getCollisionsWith(hitbox, x, y).filter { it !== this }
    }

    /**
     * When the mouse is clicked on this
     */
    open fun onInteractOn(event: ControlEvent, x: Int, y: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
    }

    /**
     * When the mouse is scrolled and is over this
     */
    open fun onScroll(dir: Int) {
    }

    abstract fun toReference(): LevelObjectReference

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelObject

        if (x != other.x) return false
        if (y != other.y) return false
        if (type != other.type) return false
        if (hitbox != other.hitbox) return false
        if (rotation != other.rotation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
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
        output.writeInt(obj.x)
        output.writeInt(obj.y)
        output.writeInt(obj.rotation)
        super.write(obj, output)
    }

    override fun instantiate(input: Input): R {
        val levelObjectType = input.read(LevelObjectType::class.java)
        return levelObjectType.instantiate(input.readInt(), input.readInt(), input.readInt()) as R
    }
}