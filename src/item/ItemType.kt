package item

import graphics.Image
import graphics.SyncAnimation
import graphics.Texture
import level.block.BlockTemplate
import level.block.ChestBlockTemplate
import level.block.CrafterBlockTemplate
import level.block.MachineBlockTemplate
import resource.ResourceType

private var nextID = 0

open class ItemType(init: ItemType.() -> Unit) : ResourceType {

    var name = "Error"
    override var texture: Texture = Image.Misc.ERROR
    var placedBlockID = BlockTemplate.ERROR.id
    var maxStack = 10


    val placedBlock: BlockTemplate<*>
        get() = BlockTemplate.ALL.first { it.id == placedBlockID }

    val id = nextID++

    override val typeID = ResourceType.ITEM

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

        val ERROR = ItemType {
            name = "Error"
        }

        val MINER = ItemType {
            name = "Miner"
            texture = SyncAnimation.MINER.images[0]
            placedBlockID = MachineBlockTemplate.MINER.id
        }

        val CRAFTER = ItemType {
            name = "Crafter"
            texture = Image.Block.CRAFTER
            placedBlockID = CrafterBlockTemplate.ITEM_CRAFTER.id
        }

        val IRON_ORE = ItemType {
            name = "Iron Ore"
            texture = Image.Item.IRON_ORE_ITEM
            maxStack = 100
        }

        val TUBE = ItemType {
            name = "Item Transport Tube"
            texture = Image.Item.TUBE_ITEM
            placedBlockID = BlockTemplate.TUBE.id
            maxStack = 50
        }

        val CHEST_SMALL = ItemType {
            name = "Small Chest"
            texture = Image.Block.CHEST_SMALL
            placedBlockID = ChestBlockTemplate.CHEST_SMALL.id
            maxStack = 20
        }

        val CHEST_LARGE = ItemType {
            name = "Large Chest"
            texture = Image.Block.CHEST_SMALL
            placedBlockID = ChestBlockTemplate.CHEST_LARGE.id
            maxStack = 20
        }

        val COPPER_ORE = ItemType {
            name = "Copper Ore"
            texture = Image.Item.COPPER_ORE_ITEM
            maxStack = 100
        }
    }
}