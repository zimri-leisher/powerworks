package level.moving

import level.*
import misc.Numbers
import java.io.DataOutputStream


abstract class MovingObject(type: MovingObjectType<out MovingObject>, xPixel: Int, yPixel: Int, rotation: Int = 0, hitbox: Hitbox = type.hitbox) : LevelObject(type, xPixel, yPixel, rotation, hitbox, true) {
    override val type = type
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
            if (value > type.maxSpeed || value < -type.maxSpeed)
                field = type.maxSpeed * Numbers.sign(value)
            else
                field = value
        }
    var yVel = 0
        set(value) {
            if (value > type.maxSpeed || value < -type.maxSpeed)
                field = type.maxSpeed * Numbers.sign(value)
            else
                field = value
        }

    /**
     * The chunk that this object's coordinates are in
     */
    var currentChunk = Level.Chunks.get(xChunk, yChunk)
    /**
     * The chunks that this object's hitbox intersects but not the chunk that its coordinates are in
     */
    var intersectingChunks =
            if (hitbox == Hitbox.NONE)
                mutableListOf()
            else
                Level.Chunks.getFromPixelRectangle(hitbox.xStart + xPixel, hitbox.yStart + yPixel, hitbox.width, hitbox.height).toMutableList().apply { remove(currentChunk) }
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
        Level.Chunks.updateChunkOf(this)
        moveListeners.forEach { it.onMove(this, pXPixel, pYPixel) }
    }

    open fun move() {
        if (xVel != 0 || yVel != 0) {
            if (xVel > 0)
                rotation = 1
            if (xVel < 0)
                rotation = 3
            if (yVel > 0)
                rotation = 2
            if (yVel < 0)
                rotation = 0
            val pXPixel = xPixel
            val pYPixel = yPixel
            val nXPixel = xPixel + xVel
            val nYPixel = yPixel + yVel
            var collisions: MutableSet<LevelObject>? = null
            val g = getCollision(nXPixel, nYPixel)
            var xPixelOk = false
            var yPixelOk = false
            if (g == null) {
                xPixelOk = true
                yPixelOk = true
            } else {
                collisions = mutableSetOf(g)
                if (nXPixel != xPixel) {
                    val o = getCollision(nXPixel, yPixel)
                    if (o == null) {
                        xPixelOk = true
                    } else {
                        collisions.add(o)
                    }
                }
                if (nYPixel != yPixel) {
                    val o = getCollision(xPixel, nYPixel)
                    if (o == null) {
                        yPixelOk = true
                    } else {
                        collisions.add(o)
                    }
                }
            }
            if (collisions != null) {
                collisions.forEach { it.onCollide(this); this.onCollide(it) }
            }
            if(xPixelOk || type.ghost) {
                xPixel = nXPixel
            }
            if(yPixelOk || type.ghost) {
                yPixel = nYPixel
            }
            if (pXPixel != xPixel || pYPixel != yPixel) {
                onMove(pXPixel, pYPixel)
            }
            xVel /= type.drag
            yVel /= type.drag
        }
    }

    override fun save(out: DataOutputStream) {
        super.save(out)
        out.writeInt(xVel)
        out.writeInt(yVel)
    }
}