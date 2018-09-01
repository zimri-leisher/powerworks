package resource

import graphics.Texture

abstract class ResourceType {
    abstract val icon: Texture
    abstract val category: ResourceCategory
    abstract val name: String

    init {
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<ResourceType>()

        fun possibleResourceTypes(name: String) = ALL.filter { it.name.contains(name) }
    }
}