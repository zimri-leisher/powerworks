package item

import graphics.Image
import graphics.SyncAnimation
import graphics.Texture
import level.block.BlockType
import level.block.ChestBlockType
import level.block.CrafterBlockType
import level.block.MachineBlockType
import resource.ResourceCategory
import resource.ResourceType

private var nextID = 0

open class ItemType(init: ItemType.() -> Unit = {}) : ResourceType {

    val id = nextID++
    override var name = "Error"
    override var icon: Texture = Image.Misc.ERROR

    override val category
        get() = ResourceCategory.ITEM
    // we do this because if we call the block type itself there will be a recursive error (both need each other to be initialized)
    var placedBlockID = BlockType.ERROR.id
    var maxStack = 10

    val placedBlock: BlockType<*>
        get() = BlockType.ALL.first { it.id == placedBlockID }

    init {
        init()
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

        val ERROR = ItemType()

        val MINER = ItemType {
            name = "Miner"
            icon = SyncAnimation.MINER.images[0]
            placedBlockID = MachineBlockType.MINER.id
        }

        val CRAFTER = ItemType {
            name = "Crafter"
            icon = Image.Block.CRAFTER
            placedBlockID = CrafterBlockType.ITEM_CRAFTER.id
        }

        val IRON_ORE = ItemType {
            name = "Iron Ore"
            icon = Image.Item.IRON_ORE_ITEM
            maxStack = 100
        }

        val TUBE = ItemType {
            name = "Item Transport Tube"
            icon = Image.Item.TUBE
            placedBlockID = BlockType.TUBE.id
            maxStack = 50
        }

        val CHEST_SMALL = ItemType {
            name = "Small Chest"
            icon = Image.Block.CHEST_SMALL
            placedBlockID = ChestBlockType.CHEST_SMALL.id
            maxStack = 20
        }

        val CHEST_LARGE = ItemType {
            name = "Large Chest"
            icon = Image.Block.CHEST_LARGE
            placedBlockID = ChestBlockType.CHEST_LARGE.id
            maxStack = 20
        }

        val FURNACE = ItemType {
            name = "Furnace"
            icon = Image.Misc.ERROR
            placedBlockID = MachineBlockType.FURNACE.id
            maxStack = 10
        }

        val COPPER_ORE = ItemType {
            name = "Copper Ore"
            icon = Image.Item.COPPER_ORE_ITEM
            maxStack = 100
        }

        val PIPE = ItemType {
            name = "Fluid Transport Pipe"
            icon = Image.Item.PIPE
            placedBlockID = BlockType.PIPE.id
            maxStack = 50
        }
    }
}

class IngotItemType(initializer: IngotItemType.() -> Unit) : ItemType() {

    init {
        maxStack = 200
        initializer()
    }

    companion object {
        val IRON_INGOT = IngotItemType {
            name = "Iron Ingot"
            icon = Image.Item.IRON_INGOT
        }

        val COPPER_INGOT = IngotItemType {
            name = "Copper Ingot"
            icon = Image.Item.COPPER_INGOT
        }
    }
}