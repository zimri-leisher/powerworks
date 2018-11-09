package resource

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.Renderable

abstract class ResourceType {
    abstract val icon: Renderable
    abstract val category: ResourceCategory
    abstract val name: String

    init {
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<ResourceType>()

        fun possibleResourceTypes(name: String) = ALL.filter { it.name.contains(name) }
        /**
         * This ignores case
         */
        fun getType(name: String) = ALL.firstOrNull { it.name.toLowerCase() == name.toLowerCase() }
    }
}