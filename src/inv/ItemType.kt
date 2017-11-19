package inv

import graphics.Image
import graphics.Texture
import level.block.BlockType
import level.block.ChestBlockType
import level.block.MachineBlockType
import level.resource.ResourceType

private var nextID = 0

sealed class ItemType(val name: String, val texture: Texture, private val placedBlockID: Int = BlockType.ERROR.id, val maxStack: Int) : ResourceType {

    object ERROR : ItemType("Error", Image.ERROR, maxStack = 5)

    object TEST : ItemType("Test", Image.ERROR, BlockType.ERROR.id, 5)

    object MINER : ItemType("Miner", Image.MINER_ITEM_TEMP, MachineBlockType.MINER.id, 10)

    object IRON_ORE : ItemType("Iron Ore", Image.IRON_ORE_ITEM, maxStack = 100)

    object CHEST_SMALL : ItemType("Small Chest", Image.ERROR, ChestBlockType.CHEST_SMALL.id, 20)

    val placedBlock: BlockType
        get() = BlockType.getByID(placedBlockID)!!

    val id = nextID++

    init {
        ALL.add(this)
    }

    override fun toString() = name

    override fun equals(other: Any?): Boolean {
        return other is ItemType && other.id == id
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {
        val ALL = mutableListOf<ItemType>()
    }
}

sealed class WeaponItemType(name: String, texture: Texture, placedBlockID: Int, maxStack: Int) : ItemType(name, texture, placedBlockID, maxStack) {

}