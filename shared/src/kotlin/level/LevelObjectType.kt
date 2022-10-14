package level

import resource.ResourceContainer
import resource.ResourceNetwork
import serialization.*

private var nextId = 0

open class LevelObjectType<T : LevelObject>(initializer: LevelObjectType<T>.() -> Unit = {}) {
    @ObjectIdentifier
    val id = nextId++
    var requiresUpdate = false

    /**
     * User-friendly name of this type
     */
    var name = "Error"

    init {
        initializer()
        ALL.add(this)
    }

    companion object {
        @ObjectList
        val ALL = mutableListOf<LevelObjectType<*>>()

        val RESOURCE_CONTAINER = LevelObjectType<ResourceContainer> {
            requiresUpdate = true
            name = "Resource Container"
        }

        val RESOURCE_NETWORK = LevelObjectType<ResourceNetwork<*>> {
            requiresUpdate = true
            name = "Resource Network"
        }

        val ERROR = LevelObjectType<LevelObject>()
    }
}