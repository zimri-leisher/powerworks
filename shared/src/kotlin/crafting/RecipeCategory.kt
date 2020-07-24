package crafting

import item.BlockItemType
import item.ItemType
import item.RobotItemType
import item.weapon.WeaponItemType
import resource.ResourceType

/**
 * Purely organizational categories for recipes. These will become tabs on the [screen.RecipeSelectorGUI]
 */
enum class RecipeCategory(val iconType: ResourceType, val categoryName: String) {
    MACHINES(BlockItemType.MINER, "Machines"),
    WEAPONS(WeaponItemType.MACHINE_GUN,"Weapons"),
    MACHINE_PARTS(ItemType.CIRCUIT, "Machine Parts"),
    ROBOTS(RobotItemType.STANDARD, "Robots"),
    LOGISTICS(BlockItemType.ITEM_PIPE, "Logistics");

    val size get() = Recipe.ALL.filter { it.category == this }.size
    operator fun iterator() = Recipe.ALL.filter { it.category == this }.iterator()

    companion object {
        operator fun iterator() = values().iterator()
    }
}