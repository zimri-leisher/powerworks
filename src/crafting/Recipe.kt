package crafting

import item.ItemType
import resource.ResourceList
import resource.ResourceType

class Recipe(
        /**
             * The resources this recipe needs
             */
            val consume: ResourceList,
        /**
             * The resources this recipe gives
             */
            val produce: ResourceList,
        /**
             * The resource to display this recipe as
             */
            val icon: ResourceType,
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

        val TEST = Recipe(ResourceList(ItemType.IRON_ORE to 1), ResourceList(ItemType.CHEST_SMALL to 1), ItemType.CHEST_SMALL)

        fun craft(resources: ResourceList): ResourceList? {
            return ALL.firstOrNull { resources.enoughIn(it.consume) }?.produce
        }
    }
}