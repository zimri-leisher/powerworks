package crafting

import item.BlockItemType
import item.IngotItemType
import item.ItemType
import item.RobotItemType
import item.weapon.WeaponItemType
import resource.ResourceList
import resource.ResourceType
import resource.resourceListOf
import serialization.ObjectList

private var nextId = 0

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
         * Whichever crafter types are able to make this recipe. For example, [Crafter.Type.DEFAULT].
         * Null means any
         */
        val validCrafterTypes: List<Crafter.Type>? = null,
        val category: RecipeCategory = RecipeCategory.WEAPONS) {

    val id = nextId++

    init {
        ALL.add(this)
    }

    companion object {
        @ObjectList
        val ALL = mutableListOf<Recipe>()

        val ERROR = Recipe(
                resourceListOf(ItemType.ERROR to 1),
                resourceListOf(ItemType.ERROR to 1),
                ItemType.ERROR,
                listOf(Crafter.Type.DEFAULT))

        val FARSEEKER = Recipe(resourceListOf(IngotItemType.IRON_INGOT to 32, IngotItemType.COPPER_INGOT to 32,
                ItemType.CABLE to 64, ItemType.CIRCUIT to 64, BlockItemType.ROBOT_FACTORY to 1),
                resourceListOf(BlockItemType.FARSEEKER to 1),
                BlockItemType.FARSEEKER,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM),
                RecipeCategory.MACHINES)

        val MINER = Recipe(
                resourceListOf(IngotItemType.IRON_INGOT to 6),
                resourceListOf(BlockItemType.MINER to 1),
                BlockItemType.MINER,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM),
                RecipeCategory.MACHINES)

        val SMELTER = Recipe(
                resourceListOf(IngotItemType.IRON_INGOT to 6),
                resourceListOf(BlockItemType.SMELTER to 1),
                BlockItemType.SMELTER,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM),
                RecipeCategory.MACHINES
        )

        val CHEST_SMALL = Recipe(
                resourceListOf(IngotItemType.IRON_INGOT to 2),
                resourceListOf(BlockItemType.CHEST_SMALL to 1),
                BlockItemType.CHEST_SMALL,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM))

        val CABLE = Recipe(
                resourceListOf(IngotItemType.COPPER_INGOT to 1),
                resourceListOf(ItemType.CABLE to 8),
                ItemType.CABLE,
                listOf(Crafter.Type.ITEM),
                RecipeCategory.MACHINE_PARTS)

        val CIRCUIT = Recipe(
                resourceListOf(IngotItemType.COPPER_INGOT to 1),
                resourceListOf(ItemType.CIRCUIT to 4),
                ItemType.CIRCUIT,
                listOf(Crafter.Type.ITEM),
                RecipeCategory.MACHINE_PARTS)

        val ROBOT = Recipe(
                resourceListOf(IngotItemType.IRON_INGOT to 8, ItemType.CIRCUIT to 8, ItemType.CABLE to 16),
                resourceListOf(RobotItemType.STANDARD to 1),
                RobotItemType.STANDARD,
                listOf(Crafter.Type.ROBOT),
                RecipeCategory.ROBOTS)

        val ITEM_CRAFTER = Recipe(
                resourceListOf(IngotItemType.IRON_INGOT to 8, IngotItemType.COPPER_INGOT to 8),
                resourceListOf(BlockItemType.CRAFTER to 1),
                BlockItemType.CRAFTER,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM),
                RecipeCategory.MACHINES)

        val ROBOT_CRAFTER = Recipe(
                resourceListOf(IngotItemType.IRON_INGOT to 16, ItemType.CIRCUIT to 16, ItemType.CABLE to 16,
                        BlockItemType.CRAFTER to 1),
                resourceListOf(BlockItemType.ROBOT_FACTORY to 1),
                BlockItemType.ROBOT_FACTORY,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM),
                RecipeCategory.MACHINES
        )

        val ITEM_PIPE = Recipe(
                resourceListOf(IngotItemType.IRON_INGOT to 2),
                resourceListOf(BlockItemType.ITEM_PIPE to 4),
                BlockItemType.ITEM_PIPE,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM),
                RecipeCategory.LOGISTICS
        )

        val FLUID_PIPE = Recipe(
                resourceListOf(IngotItemType.COPPER_INGOT to 2),
                resourceListOf(BlockItemType.FLUID_PIPE to 4),
                BlockItemType.FLUID_PIPE,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM),
                RecipeCategory.LOGISTICS
        )

        val MACHIINE_GUN = Recipe(
                resourceListOf(IngotItemType.COPPER_INGOT to 2, IngotItemType.IRON_INGOT to 2),
                resourceListOf(WeaponItemType.MACHINE_GUN to 1),
                WeaponItemType.MACHINE_GUN,
                listOf(Crafter.Type.WEAPON),
                RecipeCategory.WEAPONS
        )
    }
}