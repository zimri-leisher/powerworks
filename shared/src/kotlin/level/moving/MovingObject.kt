package level.moving

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.*
import misc.Numbers
import serialization.Id

abstract class MovingObject(type: MovingObjectType<out MovingObject>, xPixel: Int, yPixel: Int, rotation: Int = 0) : LevelObject(type, xPixel, yPixel, rotation, true) {
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

    // tags start here because of superclass tags

    @Id(17)
    var xVel = 0
        set(value) {
            if (value > type.maxSpeed || value < -type.maxSpeed)
                field = type.maxSpeed * Numbers.sign(value)
            else
                field = value
        }

    @Id(18)
    var yVel = 0
        set(value) {
            if (value > type.maxSpeed || value < -type.maxSpeed)
                field = type.maxSpeed * Numbers.sign(value)
            else
                field = value
        }

    /**
     * The chunks that this object's hitbox intersects but not the chunk that its coordinates are in
     */
    @Id(20)
    var intersectingChunks = mutableSetOf<Chunk>()

    @Id(21)
    val moveListeners = mutableListOf<MovementListener>()

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
        val oldChunk = level.getChunkFromPixel(pXPixel, pYPixel)
        val newChunk = level.getChunkAt(xChunk, yChunk)
        if (oldChunk != newChunk) {
            onChangeChunks(oldChunk, newChunk)
        } else {
            updateIntersectingChunks()
        }
        moveListeners.forEach { it.onMove(this, pXPixel, pYPixel) }
    }

    /**
     * Updates the [Chunk]s that this thinks it is in. All [MovingObject] store the chunk they are in and any chunks
     * that its [Hitbox] intersects, and they need to be updated when it moves or its [Hitbox] changes
     */
    fun onChangeChunks(oldChunk: Chunk, newChunk: Chunk) {
        if (level == LevelManager.EMPTY_LEVEL)
            return
        updateIntersectingChunks()
        oldChunk.removeMoving(this)
        newChunk.addMoving(this)
    }

    open fun move() {
        if (xVel != 0 || yVel != 0) {
            if (yVel > 0)
                rotation = 0
            else if (xVel > 0)
                rotation = 1
            else if (yVel < 0)
                rotation = 2
            else if (xVel < 0)
                rotation = 3
            val pXPixel = xPixel
            val pYPixel = yPixel
            val nXPixel = xPixel + xVel
            val nYPixel = yPixel + yVel
            var collisions: MutableSet<LevelObject>? = null
            val g = getCollisions(nXPixel, nYPixel).toMutableSet()
            var xPixelOk = false
            var yPixelOk = false
            if (g.isEmpty()) {
                xPixelOk = true
                yPixelOk = true
            } else {
                collisions = g
                if (nXPixel != xPixel) {
                    val o = getCollisions(nXPixel, yPixel)
                    if (o.isEmpty()) {
                        xPixelOk = true
                    } else {
                        collisions.addAll(o)
                    }
                }
                if (nYPixel != yPixel) {
                    val o = getCollisions(xPixel, nYPixel)
                    if (o.isEmpty()) {
                        yPixelOk = true
                    } else {
                        collisions.addAll(o)
                    }
                }
            }
            collisions?.forEach { it.onCollide(this); this.onCollide(it) }
            if (xPixelOk) {
                xPixel = nXPixel
            }
            if (yPixelOk) {
                yPixel = nYPixel
            }
            if (pXPixel != xPixel || pYPixel != yPixel) {
                onMove(pXPixel, pYPixel)
            }
            xVel /= type.drag
            yVel /= type.drag
        }
    }

    override fun onAddToLevel() {
        val newChunk = level.getChunkAt(xChunk, yChunk)
        onChangeChunks(newChunk, newChunk)
    }

    override fun onHitboxChange() {
        updateIntersectingChunks()
    }

    private fun updateIntersectingChunks() {
        if (hitbox != Hitbox.NONE) {
            val newIntersectingChunks = level.getChunksFromPixelRectangle(
                    hitbox.xStart + xPixel, hitbox.yStart + yPixel, hitbox.width, hitbox.height).toMutableSet()
            newIntersectingChunks.remove(level.getChunkAt(xChunk, yChunk))
            if (intersectingChunks != newIntersectingChunks) {
                intersectingChunks.forEach { it.data.movingOnBoundary.remove(this) }
                newIntersectingChunks.forEach { it.data.movingOnBoundary.add(this) }
                intersectingChunks = newIntersectingChunks
            }
        }
    }
}