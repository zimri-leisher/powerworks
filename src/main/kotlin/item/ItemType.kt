package item

import graphics.Image
import graphics.SyncAnimation
import graphics.Texture
import resource.ResourceCategory
import resource.ResourceType
import fluid.MoltenOreFluidType
import level.block.*

private var nextID = 0

open class ItemType(initializer: ItemType.() -> Unit = {}) : ResourceType {

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
        initializer()
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

        val TUBE = ItemType {
            name = "Item Transport Tube"
            icon = Image.Item.TUBE
            placedBlockID = BlockType.TUBE.id
            maxStack = 50
        }

        val CHEST_SMALL = ItemType {
            name = "Small Chest"
            icon = Image.Block.CHEST_SMALL
            placedBlockID = ChestBlockType.SMALL.id
            maxStack = 20
        }

        val CHEST_LARGE = ItemType {
            name = "Large Chest"
            icon = Image.Block.CHEST_LARGE
            placedBlockID = ChestBlockType.LARGE.id
            maxStack = 20
        }

        val FURNACE = ItemType {
            name = "Furnace"
            placedBlockID = MachineBlockType.FURNACE.id
            maxStack = 10
        }

        val SMALL_FLUID_TANK = ItemType {
            name = "Small Tank"
            placedBlockID = FluidTankBlockType.SMALL.id
        }

        val PIPE = ItemType {
            name = "Fluid Transport Pipe"
            icon = Image.Item.PIPE
            placedBlockID = BlockType.PIPE.id
            maxStack = 50
        }
    }
}

class OreItemType(initializer: OreItemType.() -> Unit) : ItemType() {

    var moltenForm = MoltenOreFluidType.MOLTEN_IRON

    init {
        maxStack = 100
        initializer()
    }

    companion object {
        val COPPER_ORE = OreItemType {
            name = "Copper Ore"
            moltenForm = MoltenOreFluidType.MOLTEN_COPPER
            icon = Image.Item.COPPER_ORE_ITEM
            maxStack = 100
        }

        val IRON_ORE = OreItemType {
            name = "Iron Ore"
            moltenForm = MoltenOreFluidType.MOLTEN_IRON
            icon = Image.Item.IRON_ORE_ITEM
            maxStack = 100
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