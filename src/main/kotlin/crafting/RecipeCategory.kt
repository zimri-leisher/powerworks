package crafting

import item.ItemType
import resource.ResourceType

enum class RecipeCategory(val icon: ResourceType) {
    /* SHOULD REMAIN ONLY 5 OF THESE. TODO figure out why I said this */
    MACHINE(ItemType.MINER), MISC(ItemType.ERROR), MACHINE_PARTS(ItemType.CIRCUIT), WIP2(ItemType.ERROR), WIP3(ItemType.ERROR)
}