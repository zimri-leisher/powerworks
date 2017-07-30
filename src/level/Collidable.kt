package level

interface Collidable {
    val hitbox: Hitbox

    fun getCollision(moveX: Int, moveY: Int): Boolean
}