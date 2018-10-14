package crafting

import item.BlockItemType
import item.IngotItemType
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
        val iconType: ResourceType,
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

        val ERROR = Recipe(ResourceList(ItemType.ERROR to 1), ResourceList(ItemType.ERROR to 1), ItemType.ERROR)

        val CHEST_SMALL = Recipe(ResourceList(IngotItemType.IRON_INGOT to 2), ResourceList(BlockItemType.CHEST_SMALL to 1), BlockItemType.CHEST_SMALL)

        val CABLE = Recipe(ResourceList(IngotItemType.COPPER_INGOT to 1), ResourceList(ItemType.CABLE to 8), ItemType.CABLE, category = RecipeCategory.MACHINE_PARTS)

        val CIRCUIT = Recipe(ResourceList(IngotItemType.COPPER_INGOT to 1), ResourceList(ItemType.CIRCUIT to 4), ItemType.CIRCUIT, category = RecipeCategory.MACHINE_PARTS)

        //val ROBOT = Recipe(ResourceList(IngotItemType.IRON_INGOT to 8, ItemType.CIRCUIT to 8, ItemType.CABLE to 16), ResourceList())

        val CRAFTER = Recipe(ResourceList(IngotItemType.IRON_INGOT to 4, ItemType.CIRCUIT to 8, ItemType.CABLE to 8), ResourceList(BlockItemType.CRAFTER to 1), BlockItemType.CRAFTER, category = RecipeCategory.MACHINE)
    }
}