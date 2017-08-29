package level

import java.awt.Point

abstract class Collidable(xPixel: Int, yPixel: Int, requiresUpdate: Boolean = true, /** Used to determine whether this needs rendering */ val textureCorners: Array<Point>, val hitbox: Hitbox) : LevelObject(xPixel, yPixel, requiresUpdate) {

    open fun getCollision(moveX: Int, moveY: Int): Boolean {
        return hitbox != Hitbox.NONE
    }
}