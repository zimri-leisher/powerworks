package level.entity

import item.EntityItemType
import level.moving.MovingObjectType

open class EntityType<T : Entity>(initializer: () -> Unit = {}) : MovingObjectType<T>() {
    var maxHealth = 100
    var moveSpeed = 1.0 // tiles per second

    init {
        requiresUpdate = true
        initializer()
        mass = hitbox.width * hitbox.height * density
    }

    companion object {
        val ERROR = EntityType<DefaultEntity>()
    }
}