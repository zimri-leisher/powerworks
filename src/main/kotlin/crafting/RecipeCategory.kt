package crafting

import item.BlockItemType
import item.ItemType
import resource.ResourceType

/**
 * Purely organizational categories for recipes. These will become tabs on the RecipeSelectorGUI
 */
enum class RecipeCategory(val iconType: ResourceType, val categoryName: String) {
    MACHINE(BlockItemType.MINER, "Machine"),
    MISC(ItemType.ERROR,"Miscellaneous"),
    MACHINE_PARTS(ItemType.CIRCUIT, "Machine Parts"),
    WIP2(ItemType.ERROR, "WIP2"),
    WIP3(ItemType.ERROR, "WIP3");

    operator fun iterator() = Recipe.ALL.filter { it.category == this }.iterator()

    companion object {
        operator fun iterator() = values().iterator()
    }
}