package level

abstract class Collidable(xPixel: Int, yPixel: Int, requiresUpdate: Boolean = true, val hitbox: Hitbox) : LevelObject(xPixel, yPixel, requiresUpdate) {

    var beingRendered = false

    open fun getCollision(moveX: Int, moveY: Int): Boolean {
        return hitbox != Hitbox.NONE
    }
}