package item.weapon

import level.Hitbox
import level.moving.MovingObjectType

class ProjectileType(initializer: ProjectileType.() -> Unit = {}) {

    var damage = 50
    var speed = 5
    var lifetime = -1
    var hitbox = Hitbox.NONE

    init {
        initializer()
    }

    companion object {
        val SMALL_BULLET = ProjectileType {
            damage = 20
            lifetime = 120
            speed = 10
            hitbox = Hitbox.TILE
        }
    }
}