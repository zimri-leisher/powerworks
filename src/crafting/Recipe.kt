package crafting

import level.resource.ResourceType

class Recipe(val resources: Map<ResourceType, Int>) {

    init {
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<Recipe>()
    }
}