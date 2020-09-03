package level.moving

import level.*
import misc.Geometry
import network.LevelObjectReference
import network.MovingObjectReference
import serialization.Id
import kotlin.math.*

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

    @Id(100)
    private var xPixelRemainder = 0.0

    final override var yPixel = yPixel
        set(value) {
            val old = field
            field = value
            yTile = value shr 4
            yChunk = yTile shr CHUNK_TILE_EXP
            onMove(xPixel, old)
        }

    @Id(101)
    private var yPixelRemainder = 0.0

    final override var xTile = xPixel shr 4
        private set

    final override var yTile = yPixel shr 4
        private set

    final override var xChunk = xTile shr CHUNK_TILE_EXP
        private set

    final override var yChunk = yTile shr CHUNK_TILE_EXP
        private set

    @Id(17)
    var xVel = 0.0
        set(value) {
            if (value > type.maxSpeed || value < -type.maxSpeed) {
                field = type.maxSpeed.toDouble() * value.sign
            } else if (value.absoluteValue < EPSILON) {
                field = 0.0
            } else {
                field = value
            }
        }

    @Id(18)
    var yVel = 0.0
        set(value) {
            if (value.absoluteValue > type.maxSpeed) {
                field = type.maxSpeed.toDouble() * value.sign
            } else if (value.absoluteValue < EPSILON) {
                field = 0.0
            } else {
                field = value
            }
        }

    /**
     * The chunks that this object's hitbox intersects but not the chunk that its coordinates are in
     */
    @Id(20)
    var intersectingChunks = mutableSetOf<Chunk>()

    @Id(21)
    val moveListeners = mutableListOf<MovementListener>()

    @Id(102)
    private var gettingUnstuck = false

    fun setPosition(xPixel: Int, yPixel: Int) {
        val oXPixel = this.xPixel
        val oYPixel = this.yPixel
        this.xPixel = xPixel
        this.yPixel = yPixel
        xPixelRemainder = 0.0
        yPixelRemainder = 0.0
        val dist = Geometry.distance(oXPixel, oYPixel, xPixel, yPixel)
        if (dist > 8) {
            println("TELEPORTING: $dist")
        }
    }

    override fun update() {
        super.update()
        move()
    }

    fun applyForce(angle: Double, mag: Double) {
        val accel = mag / type.mass
        xVel += cos(angle) * accel
        yVel += sin(angle) * accel
    }

    private fun isCollidingAlongXAxis(o: LevelObject): Boolean {
        val range = (yPixel + hitbox.yStart) until (yPixel + hitbox.yStart + hitbox.height)
        val otherRange = (o.yPixel + o.hitbox.yStart) until (o.yPixel + o.hitbox.yStart + o.hitbox.height)
        return range.any { it in otherRange }
    }

    private fun getNewVelocity(thisVel: Double, otherVel: Double, otherMass: Double): Double {
        val thisMomentum = type.mass * thisVel.absoluteValue
        val otherMomentum = otherMass * otherVel.absoluteValue
        val totalMomentum = otherMomentum + thisMomentum
        if (thisVel.absoluteValue < EPSILON) { // this is at rest
            // v2f = [2m1/(m1 + m2)]v1i, where this is object 2
            return (2 * otherMomentum / (totalMomentum)) * otherVel
        } else {
            // v1f = [(m1 - m2)/(m1 + m2)]v1i + [2m2/(m1 + m2)]v2, where this is object 1
            return ((thisMomentum - otherMomentum) / totalMomentum) * thisVel + ((2 * otherMomentum) / totalMomentum) * otherVel
        }
    }

    open fun push(other: MovingObject) {
        // want to find the side of the hitbox that we're colliding with
        // are we colliding in the x axis or the y axis?
        // if we're colliding along the x axis, then some of the y values of the range from yStart...yStart + height
        // should be the same
        val alongXAxis = isCollidingAlongXAxis(other)
        if (alongXAxis) {
            xVel = getNewVelocity(xVel, other.xVel, other.type.mass)
        } else {
            yVel = getNewVelocity(yVel, other.yVel, other.type.mass)
        }
    }

    override fun onCollide(obj: LevelObject) {

    }

    protected open fun onMove(pXPixel: Int, pYPixel: Int) {
        if(!inLevel)
            return
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
        if (oldChunk !== newChunk) {
            oldChunk.removeMoving(this)
        }
        newChunk.addMoving(this)
    }

    private fun updateRotation() {
        var angle = atan2(yVel, xVel)
        if (angle < 0) {
            angle += 2 * PI
        }
        rotation = when {
            angle.absoluteValue < PI / 4 -> 1
            angle in (PI / 4)..(3 * PI / 4) -> 0
            angle in (3 * PI / 4)..(5 * PI / 4) -> 3
            else -> 2
        }
    }

    open fun move() {
        if (xVel.absoluteValue > EPSILON || yVel.absoluteValue > EPSILON) {
            val currentCollisions = getCollisions(xPixel, yPixel)
            if (currentCollisions.any()) {
                if (!gettingUnstuck) {
                    // the first time we notice that it's stuck, stop all movement except for getting unstuck
                    xVel = 0.0
                    yVel = 0.0
                }
                gettingUnstuck = true
                for (collider in currentCollisions) {
                    applyForce(atan2(yPixel - collider.yPixel.toDouble(), xPixel - collider.xPixel.toDouble()), type.mass)
                }
                println("getting unstuck")
            } else {
                gettingUnstuck = false
            }
            updateRotation()
            // add fractional part of velocity
            xPixelRemainder += xVel - xVel.toInt()
            yPixelRemainder += yVel - yVel.toInt()
            // add integer of velocity and integer of remainder
            val nXPixel = xPixel + xVel.toInt() + xPixelRemainder.toInt()
            val nYPixel = yPixel + yVel.toInt() + yPixelRemainder.toInt()
            // remove integer of remainder
            xPixelRemainder -= xPixelRemainder.toInt()
            yPixelRemainder -= yPixelRemainder.toInt()
            if (gettingUnstuck) {
                // ignore all collisions
                xPixel = nXPixel
                yPixel = nYPixel
                currentCollisions.forEach {
                    /*
                    if (it is MovingObject) {
                        push(it)
                        it.push(this)
                    }
                     */
                    it.onCollide(this)
                    this.onCollide(it)
                }
            } else {
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
                        if (o.none()) {
                            xPixelOk = true
                        } else {
                            collisions.addAll(o)
                        }
                    }
                    if (nYPixel != yPixel) {
                        val o = getCollisions(xPixel, nYPixel)
                        if (o.none()) {
                            yPixelOk = true
                        } else {
                            collisions.addAll(o)
                        }
                    }
                }
                if (xPixelOk) {
                    xPixel = nXPixel
                }
                if (yPixelOk) {
                    yPixel = nYPixel
                }
                collisions?.forEach {
                    /*
                    if (it is MovingObject) {
                        push(it)
                        it.push(this)
                    }
                     */
                    it.onCollide(this)
                    this.onCollide(it)
                }
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
        if (hitbox != Hitbox.NONE && inLevel) {
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

    /**
     * @return this [LevelObject] as a [LevelObjectReference]
     * @see [LevelObjectReference]
     */
    override fun toReference(): LevelObjectReference {
        return MovingObjectReference(this)
    }

    companion object {
        const val EPSILON = 1e-3
    }
}