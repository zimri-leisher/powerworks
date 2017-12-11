package crafting

import inv.ItemType
import level.resource.ResourceType

typealias ResourceList = Map<ResourceType, Int>

fun ResourceList.enoughIn(other: ResourceList): Boolean {
    for ((k, v) in this) {
        if (!other.containsKey(k) || other.get(k)!! < v)
            return false
    }
    return true
}

class Recipe(val consume: ResourceList,
             val produce: ResourceList,
        /*
        * Whichever crafter types are able to make this recipe. For example, Crafters#PLAYER
        * Null means any
        */
             val validCrafters: List<Crafter>? = null) {

    init {
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<Recipe>()

        val TEST = Recipe(mapOf(Pair(ItemType.IRON_ORE, 1)), mapOf(Pair(ItemType.CHEST_SMALL, 1)))

        fun craft(resources: ResourceList): ResourceList? {
            return ALL.firstOrNull { resources.enoughIn(it.consume) }?.produce
        }
    }
}