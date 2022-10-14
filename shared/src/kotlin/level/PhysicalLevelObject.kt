package level

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.ControlEvent
import main.DebugCode
import main.Game
import main.toColor
import serialization.Id
import java.util.*

abstract class PhysicalLevelObject protected constructor(
    type: PhysicalLevelObjectType<out PhysicalLevelObject>,
    @Id(2)
    open val x: Int,
    @Id(3)
    open val y: Int,
    /**
     * Whether or not the [Interactor] tool should allow clicking on this
     */
    @Id(4)
    var isInteractable: Boolean = true
) : LevelObject(type) {

    private constructor() : this(PhysicalLevelObjectType.ERROR, 0, 0)

    override val type = type

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
                val oldHitbox = field
                field = value
                onHitboxChange(oldHitbox)
            }
        }

    @Id(14)
    var rotation = 0
        set(value) {
            if (field != value) {
                val oldDir = field
                field = value
                onRotate(oldDir)
            }
        }

    @Id(991)
    var health = type.maxHealth
        set(value) {
            if (type.damageable) {
                field = value
            }
        }

    override fun render() {
        type.textures.render(this)
        if (health != type.maxHealth) {
            renderHealthBar()
        }
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES) {
            renderHitbox()
        }
    }

    fun renderHealthBar() {
        Renderer.renderFilledRectangle(
            x + hitbox.xStart - (14 - hitbox.width) / 2 - 1,
            y + hitbox.yStart - 7,
            16,
            7,
            TextureRenderParams(color = toColor(r = 0.5f, g = 0.5f, b = 0.5f))
        )
        Renderer.renderFilledRectangle(
            x + hitbox.xStart - (14 - hitbox.width) / 2,
            y + hitbox.yStart - 6,
            (14 * (health.toDouble() / type.maxHealth)).toInt(),
            5,
            TextureRenderParams(color = toColor(g = 1.0f))
        )
    }

    protected fun renderHitbox() {
        Renderer.renderFilledRectangle(
            x + hitbox.xStart,
            y + hitbox.yStart,
            hitbox.width,
            hitbox.height,
            TextureRenderParams(color = Color(0xFF0010))
        )
    }

    open fun onRotate(oldDir: Int) {
        //hitbox = Hitbox.rotate(type.hitbox, value)
    }

    open fun onHitboxChange(oldHitbox: Hitbox) {
    }

    /**
     * Called if this collides with something or something else collides with it
     */
    open fun onCollide(obj: PhysicalLevelObject) {
    }

    /**
     * @return a sequence of [PhysicalLevelObject]s that would collide with this [PhysicalLevelObject] if it were at [x], [y] in the
     * given [level] (defaults to [PhysicalLevelObject.level])
     */
    open fun getCollisions(x: Int, y: Int, level: Level = this.level): Sequence<PhysicalLevelObject> {
        if (hitbox == Hitbox.NONE)
            return emptySequence()
        return level.getCollisionsWith(hitbox, x, y).filter { it !== this }
    }

    /**
     * When the mouse is clicked on this
     */
    open fun onInteractOn(
        event: ControlEvent,
        x: Int,
        y: Int,
        button: Int,
        shift: Boolean,
        ctrl: Boolean,
        alt: Boolean
    ) {
    }

    /**
     * When the mouse is scrolled and is over this
     */
    open fun onScroll(dir: Int) {
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhysicalLevelObject

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