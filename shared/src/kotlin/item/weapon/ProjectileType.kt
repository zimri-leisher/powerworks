package item.weapon

import level.Hitbox

private var nextId = 0

class ProjectileType(initializer: ProjectileType.() -> Unit = {}) {

    val id = nextId++
    var damage = 50
    var speed = 5
    var lifetime = -1
    var hitbox = Hitbox.NONE

    init {
        initializer()
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<ProjectileType>()

        val ERROR = ProjectileType {}

        val SMALL_BULLET = ProjectileType {
            damage = 20
            lifetime = 120
            speed = 5
            hitbox = Hitbox.BULLET
        }
    }
}