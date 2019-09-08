package crafting

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import item.BlockItemType
import item.IngotItemType
import item.ItemType
import item.RobotItemType
import resource.ResourceList
import resource.ResourceType

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
        val category: RecipeCategory = RecipeCategory.MISC) {

    val id = nextId++

    init {
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<Recipe>()

        val ERROR = Recipe(
                ResourceList(ItemType.ERROR to 1),
                ResourceList(ItemType.ERROR to 1),
                ItemType.ERROR,
                listOf(Crafter.Type.DEFAULT))

        val CHEST_SMALL = Recipe(
                ResourceList(IngotItemType.IRON_INGOT to 2),
                ResourceList(BlockItemType.CHEST_SMALL to 1),
                BlockItemType.CHEST_SMALL,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM))

        val CABLE = Recipe(
                ResourceList(IngotItemType.COPPER_INGOT to 1),
                ResourceList(ItemType.CABLE to 8),
                ItemType.CABLE,
                listOf(Crafter.Type.ITEM),
                RecipeCategory.MACHINE_PARTS)

        val CIRCUIT = Recipe(
                ResourceList(IngotItemType.COPPER_INGOT to 1),
                ResourceList(ItemType.CIRCUIT to 4),
                ItemType.CIRCUIT,
                listOf(Crafter.Type.ITEM),
                RecipeCategory.MACHINE_PARTS)

        val ROBOT = Recipe(
                ResourceList(IngotItemType.IRON_INGOT to 8, ItemType.CIRCUIT to 8, ItemType.CABLE to 16),
                ResourceList(RobotItemType.STANDARD to 1),
                RobotItemType.STANDARD,
                listOf(Crafter.Type.ROBOT),
                RecipeCategory.BOT)

        val CRAFTER = Recipe(
                ResourceList(IngotItemType.IRON_INGOT to 4, ItemType.CIRCUIT to 8, ItemType.CABLE to 8),
                ResourceList(BlockItemType.CRAFTER to 1),
                BlockItemType.CRAFTER,
                listOf(Crafter.Type.DEFAULT, Crafter.Type.ITEM),
                RecipeCategory.MACHINE)
    }
}

class RecipeSerializer : Serializer<Recipe>() {
    override fun write(kryo: Kryo, output: Output, `object`: Recipe) {
        output.writeInt(`object`.id)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Recipe>): Recipe {
        return Recipe.ALL.first { it.id == input.readInt() }
    }
}