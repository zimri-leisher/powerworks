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
        /**
        * Whichever crafter types are able to make this recipe. For example, Crafters#PLAYER
        * Null means any
        */
        val validCrafters: List<Crafter>? = null,
        val category: RecipeCategory = RecipeCategory.MISC) {

    init {
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<Recipe>()

        val ERROR = Recipe(ResourceList(ItemType.ERROR to 1), ResourceList(ItemType.CHEST_SMALL to 2), ItemType.ERROR)
    }
}