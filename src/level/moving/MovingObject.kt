package level.moving

import level.Collidable
import level.Hitbox
import level.MovementListener
import main.Game

const val DEFAULT_MAX_SPEED = 20
const val DEFAULT_DRAG = 4

abstract class MovingObject(xPixel: Int, yPixel: Int, hitbox: Hitbox) : Collidable(xPixel, yPixel, true, hitbox) {

    /* Only allow setting of pixel values because otherwise it would cause infinite loop (unless I added a lot of boilerplate private values) */
    final override var xPixel = xPixel
        protected set(value) {
            val old = field
            field = value
            xTile = value shr 4
            xChunk = xTile shr 3
            onMove(old, yPixel)
        }

    final override var yPixel = yPixel
        protected set(value) {
            val old = field
            field = value
            yTile = value shr 4
            yChunk = yTile shr 3
            onMove(xPixel, old)
        }

    final override var xTile = xPixel shr 4
        private set

    final override var yTile = yPixel shr 4
        private set

    final override var xChunk = xTile shr 3
        private set

    final override var yChunk = yTile shr 3
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
    var currentChunk = Game.currentLevel.getChunk(xChunk, yChunk)
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
        if(xVel != 0 || yVel != 0) {
            if (!getCollision(xVel, yVel)) {
                xPixel += xVel
                yPixel += yVel
            } else {
                if (!getCollision(xVel, 0)) {
                    xPixel += xVel
                }
                if (!getCollision(0, yVel)) {
                    yPixel += yVel
                }
            }
            if (pXPixel != xPixel || pYPixel != yPixel) {
                onMove(pXPixel, pYPixel)
            }
            xVel /= DEFAULT_DRAG
            yVel /= DEFAULT_DRAG
        }
    }
}