package level

import resource.ResourceContainer
import resource.ResourceNetwork
import serialization.*

private var nextId = 0

open class LevelObjectType<T : LevelObject>(initializer: LevelObjectType<T>.() -> Unit = {}) {
    @ObjectIdentifier
    val id = nextId++
    var requiresUpdate = false

    init {
        initializer()
        ALL.add(this)
    }

    companion object {
        @ObjectList
        val ALL = mutableListOf<LevelObjectType<*>>()

        val RESOURCE_CONTAINER = LevelObjectType<ResourceContainer> {
            requiresUpdate = true
        }

        val RESOURCE_NETWORK = LevelObjectType<ResourceNetwork> {
            requiresUpdate = true
        }

        val ERROR = LevelObjectType<LevelObject>()
    }
}