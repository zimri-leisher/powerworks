package level.entity

import item.EntityItemType
import level.moving.MovingObjectType

open class EntityType<T : Entity>(initializer: () -> Unit = {}) : MovingObjectType<T>() {
    var moveSpeed = 1.0 // tiles per second

    init {
        requiresUpdate = true
        initializer()
        mass = hitbox.width * hitbox.height * density
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<EntityType<*>>()

        val ERROR = EntityType<DefaultEntity>()
    }
}