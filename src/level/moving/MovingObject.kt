package level.moving

import level.Hitbox
import level.LevelObject
import level.MovementListener
import main.Game

const val DEFAULT_MAX_SPEED = 20
const val DEFAULT_DRAG = 4

abstract class MovingObject(xPixel: Int, yPixel: Int, hitbox: Hitbox) : LevelObject(xPixel, yPixel, hitbox, true) {

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
    var intersectingChunks = if (hitbox == Hitbox.NONE) mutableListOf() else Game.currentLevel.getChunksFromPixelRectangle(hitbox.xStart + xPixel, hitbox.yStart + yPixel, hitbox.width, hitbox.height).toMutableList()
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
        if (xVel != 0 || yVel != 0) {
            if (!getCollision(nXPixel, nYPixel)) {
                xPixel = nXPixel
                yPixel = nYPixel
            } else {
                if (nXPixel != xPixel)
                    if (!getCollision(nXPixel, yPixel)) {
                        xPixel = nXPixel
                    }
                if (nYPixel != yPixel)
                    if (!getCollision(xPixel, nYPixel)) {
                        yPixel = nYPixel
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