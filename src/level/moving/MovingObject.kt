package level.moving

import level.CHUNK_TILE_EXP
import level.Hitbox
import level.LevelObject
import level.MovementListener
import main.Game
import java.io.DataOutputStream

const val DEFAULT_MAX_SPEED = 20
const val DEFAULT_DRAG = 4

abstract class MovingObject(xPixel: Int, yPixel: Int, hitbox: Hitbox) : LevelObject(xPixel, yPixel, hitbox, true) {

    /* Only allow setting of pixel values because otherwise it would cause infinite loop (unless I added a lot of boilerplate private values) */
    final override var xPixel = xPixel
        set(value) {
            val old = field
            field = value
            xTile = value shr 4
            xChunk = xTile shr CHUNK_TILE_EXP
            onMove(old, yPixel)
        }

    final override var yPixel = yPixel
        set(value) {
            val old = field
            field = value
            yTile = value shr 4
            yChunk = yTile shr CHUNK_TILE_EXP
            onMove(xPixel, old)
        }

    final override var xTile = xPixel shr 4
        private set

    final override var yTile = yPixel shr 4
        private set

    final override var xChunk = xTile shr CHUNK_TILE_EXP
        private set

    final override var yChunk = yTile shr CHUNK_TILE_EXP
        private set

    var xVel = 0
        set(value) {
            if (value > DEFAULT_MAX_SPEED || value < -DEFAULT_MAX_SPEED)
                field = DEFAULT_MAX_SPEED * (value / Math.abs(value))
            else
                field = value
        }
    var yVel = 0
        set(value) {
            if (value > DEFAULT_MAX_SPEED || value < -DEFAULT_MAX_SPEED)
                field = DEFAULT_MAX_SPEED * (value / Math.abs(value))
            else
                field = value
        }
    var dir = 0
    /**
     * The chunk that this object's coordinates are in
     */
    var currentChunk = Game.currentLevel.getChunk(xChunk, yChunk)
    /**
     * The chunks that this object's hitbox intersects but not the chunk that its coordinates are in
     */
    var intersectingChunks =
            if (hitbox == Hitbox.NONE)
                mutableListOf()
            else
                Game.currentLevel.getChunksFromPixelRectangle(hitbox.xStart + xPixel, hitbox.yStart + yPixel, hitbox.width, hitbox.height).toMutableList().
                        apply { remove(currentChunk) }
    val moveListeners = mutableListOf<MovementListener>()

    init {
        intersectingChunks.remove(currentChunk)
    }

    override fun update() {
        move()
    }

    fun setPosition(xPixel: Int, yPixel: Int) {
        val oXPixel = this.xPixel
        val oYPixel = this.yPixel
        this.xPixel = xPixel
        this.yPixel = yPixel
        onMove(oXPixel, oYPixel)
    }

    protected open fun onMove(pXPixel: Int, pYPixel: Int) {
        Game.currentLevel.updateChunk(this)
        moveListeners.forEach { it.onMove(this, pXPixel, pYPixel) }
    }

    open fun move() {
        if (xVel > 0)
            dir = 1
        if (xVel < 0)
            dir = 3
        if (yVel > 0)
            dir = 2
        if (yVel < 0)
            dir = 0
        val pXPixel = xPixel
        val pYPixel = yPixel
        val nXPixel = xPixel + xVel
        val nYPixel = yPixel + yVel
        var collisions: MutableSet<LevelObject>? = null
        if (xVel != 0 || yVel != 0) {
            val g = getCollision(nXPixel, nYPixel)
            if (g == null) {
                xPixel = nXPixel
                yPixel = nYPixel
            } else {
                collisions = mutableSetOf(g)
                if (nXPixel != xPixel) {
                    val o = getCollision(nXPixel, yPixel)
                    if (o == null) {
                        xPixel = nXPixel
                    } else {
                        collisions.add(o)
                    }
                }
                if (nYPixel != yPixel) {
                    val o = getCollision(xPixel, nYPixel)
                    if (o == null) {
                        yPixel = nYPixel
                    } else {
                        collisions.add(o)
                    }
                }
            }
            if (collisions != null) {
                collisions.forEach { it.onCollide(this); this.onCollide(it) }
            }
            if (pXPixel != xPixel || pYPixel != yPixel) {
                onMove(pXPixel, pYPixel)
            }
            xVel /= DEFAULT_DRAG
            yVel /= DEFAULT_DRAG
        }
    }

    override fun save(out: DataOutputStream) {
        out.writeInt(xVel)
        out.writeInt(yVel)
        out.writeInt(dir)
    }
}