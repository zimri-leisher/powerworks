package level

import graphics.Image
import resource.ResourceNode
import resource.ResourceType
import serialization.ObjectList


open class PhysicalLevelObjectType<T : PhysicalLevelObject>(initializer: PhysicalLevelObjectType<T>.() -> Unit = {}) :
    LevelObjectType<T>() {
    var textures = PhysicalLevelObjectTextures(Image.Misc.ERROR)
    var hitbox = Hitbox.NONE
    var resourceForm: ResourceType? = null
    var maxHealth = 100
    var damageable = true

    init {
        initializer()
        ALL.add(this)
    }

    companion object {
        @ObjectList
        val ALL = mutableListOf<PhysicalLevelObjectType<*>>()

        val ERROR = PhysicalLevelObjectType<PhysicalLevelObject>()

        val RESOURCE_NODE = PhysicalLevelObjectType<ResourceNode> {
            damageable = false
        }
    }
}