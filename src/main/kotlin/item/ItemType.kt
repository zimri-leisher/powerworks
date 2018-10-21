package item

import graphics.Image
import resource.ResourceCategory
import resource.ResourceType
import fluid.MoltenOreFluidType
import graphics.Animation
import level.LevelObjectType
import level.block.*

private var nextID = 0

open class ItemType(initializer: ItemType.() -> Unit = {}) : ResourceType() {

    val id = nextID++
    override var name = "Error"
    override var icon = Image.Misc.ERROR

    override val category
        get() = ResourceCategory.ITEM
    var maxStack = 10

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

        val CIRCUIT = ItemType {
            name = "Circuit"
            maxStack = 100
            icon = Image.Item.CIRCUIT
        }

        val CABLE = ItemType {
            name = "Cable"
            maxStack = 100
            icon = Image.Item.CABLE
        }
    }
}

class BlockItemType(initializer: BlockItemType.() -> Unit): ItemType() {
    var placedBlock: BlockType<*> = BlockType.ERROR

    init {
        initializer()
    }

    companion object {
        val MINER = BlockItemType {
            name = "Miner"
            icon = Animation.MINER[0]
            placedBlock = MachineBlockType.MINER
        }

        val CRAFTER = BlockItemType {
            name = "Crafter"
            icon = Image.Block.CRAFTER
            placedBlock = CrafterBlockType.ITEM_CRAFTER
        }

        val ROBOT_FACTORY = BlockItemType {
            name = "Robot Factory"
            icon = Image.Block.ROBOT_CRAFTER
            placedBlock = CrafterBlockType.ROBOT_FACTORY
        }

        val TUBE = BlockItemType {
            name = "Item Transport Tube"
            icon = Image.Item.TUBE
            placedBlock = BlockType.TUBE
            maxStack = 50
        }

        val CHEST_SMALL = BlockItemType {
            name = "Small Chest"
            icon = Image.Block.CHEST_SMALL
            placedBlock = ChestBlockType.SMALL
            maxStack = 20
        }

        val CHEST_LARGE = BlockItemType {
            name = "Large Chest"
            icon = Image.Block.CHEST_LARGE
            placedBlock = ChestBlockType.LARGE
            maxStack = 20
        }

        val FURNACE = BlockItemType {
            name = "Furnace"
            icon = Image.Block.FURNACE
            placedBlock = MachineBlockType.FURNACE
            maxStack = 10
        }

        val SMALL_FLUID_TANK = BlockItemType {
            name = "Small Tank"
            placedBlock = FluidTankBlockType.SMALL
        }

        val PIPE = BlockItemType {
            name = "Fluid Transport Pipe"
            icon = Image.Item.PIPE
            placedBlock = BlockType.PIPE
            maxStack = 50
        }

        val SOLIDIFIER = BlockItemType {
            name = "Molten Ore Solidifier"
            icon = Animation.SOLIDIFIER[0]
            placedBlock = MachineBlockType.SOLIDIFIER
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