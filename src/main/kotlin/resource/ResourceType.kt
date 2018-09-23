package resource

import com.badlogic.gdx.graphics.g2d.TextureRegion

abstract class ResourceType {
    abstract val icon: TextureRegion
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