package crafting

import item.BlockItemType
import item.ItemType
import item.RobotItemType
import resource.ResourceType

/**
 * Purely organizational categories for recipes. These will become tabs on the [screen.RecipeSelectorGUI]
 */
enum class RecipeCategory(val iconType: ResourceType, val categoryName: String) {
    MACHINE(BlockItemType.MINER, "Machine"),
    MISC(ItemType.ERROR,"Miscellaneous"),
    MACHINE_PARTS(ItemType.CIRCUIT, "Machine Parts"),
    BOT(RobotItemType.STANDARD, "Robots"),
    WIP3(ItemType.ERROR, "WIP3");

    val size get() = Recipe.ALL.filter { it.category == this }.size
    operator fun iterator() = Recipe.ALL.filter { it.category == this }.iterator()

    companion object {
        operator fun iterator() = values().iterator()
    }
}