package level

import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import graphics.Renderer
import graphics.TextureRenderParams
import io.PressType
import level.moving.MovingObject
import main.DebugCode
import main.Game
import screen.mouse.Tool.Companion.Interactor
import java.io.DataOutputStream
import java.util.*

abstract class LevelObject protected constructor(
        type: LevelObjectType<out LevelObject>,
        open val xPixel: Int,
        open val yPixel: Int,
        rotation: Int = 0,
        /**
         * Whether or not the [Interactor] tool should allow clicking on this
         */
        @Tag(4)
        var isInteractable: Boolean = true) {

    private constructor() : this(LevelObjectType.ERROR, 0, 0)

    open val type = type

    @Tag(5)
    val id = UUID.randomUUID()

    @Tag(6)
    var level: Level = LevelManager.EMPTY_LEVEL

    @Tag(8)
    open val xTile = xPixel shr 4
    @Tag(9)
    open val yTile = yPixel shr 4
    @Tag(11)
    open val xChunk = xTile shr CHUNK_TILE_EXP
    @Tag(12)
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

    @Tag(13)
    var hitbox = type.hitbox
        set(value) {
            if (field != value) {
                if(this is MovingObject) {
                    level.updateChunkOf(this)
                }
                field = value
            }
        }

    @Tag(14)
    var rotation = rotation
        set(value) {
            if (field != value) {
                field = value
                //hitbox = Hitbox.rotate(type.hitbox, value)
            }
        }

    /**
     * If this has been added to a [Level] (one that isn't [LevelManager.EMPTY_LEVEL])
     */
    @Tag(15)
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

    @Tag(16)
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

class LevelObjectSerializerFactory : SerializerFactory<LevelObjectSerializer> {
    override fun newSerializer(kryo: Kryo, type: Class<*>): LevelObjectSerializer {
        return LevelObjectSerializer(kryo, type as Class<out LevelObject>)
    }

    override fun isSupported(type: Class<*>): Boolean {
        return LevelObject::class.java.isAssignableFrom(type)
    }
}

class LevelObjectSerializer(kryo: Kryo, clazz: Class<out LevelObject>) : TaggedFieldSerializer<LevelObject>(kryo, clazz) {

    override fun writeHeader(kryo: Kryo, output: Output, `object`: LevelObject) {
        output.writeInt(`object`.type.id)
        output.writeInt(`object`.xPixel)
        output.writeInt(`object`.yPixel)
        output.writeInt(`object`.rotation)
    }

    override fun create(kryo: Kryo, input: Input, type: Class<out LevelObject>): LevelObject {
        val id = input.readInt()
        val levelObjectType = LevelObjectType.ALL.first { it.id == id }
        return levelObjectType.instantiate(input.readInt(), input.readInt(), input.readInt())
    }
}