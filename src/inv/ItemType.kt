package inv

import graphics.Image
import graphics.Texture
import level.block.BlockType
import level.block.ChestBlockType
import level.block.MachineBlockType
import level.resource.ResourceType

private var nextID = 0

open class ItemType(val name: String, val texture: Texture, val stretchTexture: Boolean = true, private val placedBlockID: Int = BlockType.ERROR.id, val maxStack: Int) : ResourceType {

    constructor(parent: ItemType) : this(parent.name, parent.texture, parent.stretchTexture, parent.placedBlockID, parent.maxStack)

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

        val ERROR = ItemType("Error",
                Image.ERROR,
                maxStack = 5)

        val TEST = ItemType("Test",
                Image.ERROR,
                placedBlockID = BlockType.ERROR.id,
                maxStack = 5)

        val MINER = ItemType("Miner",
                Image.MINER_ITEM_TEMP,
                placedBlockID = MachineBlockType.MINER.id,
                maxStack = 10)

        val IRON_ORE = ItemType("Iron Ore",
                Image.IRON_ORE_ITEM,
                maxStack = 100)

        val TUBE = ItemType("Item Transport Tube",
                Image.TUBE_ITEM,
                false,
                BlockType.TUBE.id,
                50)

        val CHEST_SMALL = ItemType("Small Chest",
                Image.ERROR,
                placedBlockID = ChestBlockType.CHEST_SMALL.id,
                maxStack = 20)
    }
}