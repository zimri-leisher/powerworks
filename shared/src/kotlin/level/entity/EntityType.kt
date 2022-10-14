package level.entity

import item.EntityItemType
import item.ItemType
import level.moving.MovingObjectType
import serialization.ObjectList

open class EntityType<T : Entity>(initializer: () -> Unit = {}) : MovingObjectType<T>() {
    var moveSpeed = 1.0 // tiles per second
    var itemForm = EntityItemType.ERROR
    var placedClass: Class<T>? = null

    fun spawn(x: Int, y: Int): T {
        val ctor =
            placedClass!!.constructors.firstOrNull { it.parameterTypes[0] == Integer.TYPE && it.parameterTypes[1] == Integer.TYPE }
                ?: throw Exception("Entity $name with placed class $placedClass does not implement (x, y) constructor")
        return ctor.newInstance(x, y) as T
    }

    init {
        requiresUpdate = true
        initializer()
        mass = hitbox.width * hitbox.height * density
        ALL.add(this)
    }

    companion object {
        @ObjectList
        val ALL = mutableListOf<EntityType<*>>()

        val ERROR = EntityType<DefaultEntity>()
    }
}