package item

import resource.ResourceCategory
import resource.ResourceType
import fluid.MoltenOreFluidType
import graphics.*
import level.block.*
import level.entity.EntityType
import level.entity.robot.RobotType

private var nextID = 0

open class ItemType(initializer: ItemType.() -> Unit = {}) : ResourceType() {

    override var name = "Error"
    override var icon: Renderable = Texture(Image.Misc.ERROR)

    override val category
        get() = ResourceCategory.ITEM
    var maxStack = 10

    init {
        initializer()
        ALL.add(this)
    }

    override fun toString() = name

    companion object {
        val ALL = mutableListOf<ItemType>()

        val ERROR = ItemType()

        init {
            EntityItemType
            RobotItemType
            BlockItemType
            OreItemType
            IngotItemType
        }

        val CIRCUIT = ItemType {
            name = "Circuit"
            maxStack = 100
            icon = Texture(Image.Item.CIRCUIT)
        }

        val CABLE = ItemType {
            name = "Cable"
            maxStack = 100
            icon = Texture(Image.Item.CABLE)
        }
    }
}

open class EntityItemType(initializer: EntityItemType.() -> Unit = {}) : ItemType() {
    var spawnedEntity: EntityType<*> = EntityType.ERROR

    init {
        initializer()
    }

    companion object {

    }
}

class RobotItemType(initializer: RobotItemType.() -> Unit) : EntityItemType() {

    init {
        maxStack = 100
        initializer()
    }

    companion object {
        val STANDARD = RobotItemType {
            name = "Standard Robot"
            icon = Texture(ImageCollection.ROBOT[0])
            spawnedEntity = RobotType.STANDARD
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
            icon = Animation.MINER
            placedBlock = MachineBlockType.MINER
        }

        val CRAFTER = BlockItemType {
            name = "Crafter"
            icon = Texture(Image.Block.CRAFTER)
            placedBlock = CrafterBlockType.ITEM_CRAFTER
        }

        val ROBOT_FACTORY = BlockItemType {
            name = "Robot Factory"
            icon = Texture(Image.Block.ROBOT_CRAFTER)
            placedBlock = CrafterBlockType.ROBOT_FACTORY
        }

        val TUBE = BlockItemType {
            name = "Item Transport Tube"
            icon = Texture(Image.Item.TUBE)
            placedBlock = BlockType.TUBE
            maxStack = 50
        }

        val CHEST_SMALL = BlockItemType {
            name = "Small Chest"
            icon = Texture(Image.Block.CHEST_SMALL)
            placedBlock = ChestBlockType.SMALL
            maxStack = 20
        }

        val CHEST_LARGE = BlockItemType {
            name = "Large Chest"
            icon = Texture(Image.Block.CHEST_LARGE)
            placedBlock = ChestBlockType.LARGE
            maxStack = 20
        }

        val FURNACE = BlockItemType {
            name = "Furnace"
            icon = Texture(Image.Block.FURNACE)
            placedBlock = MachineBlockType.FURNACE
            maxStack = 10
        }

        val SMALL_FLUID_TANK = BlockItemType {
            name = "Small Tank"
            placedBlock = FluidTankBlockType.SMALL
        }

        val PIPE = BlockItemType {
            name = "Fluid Transport Pipe"
            icon = Texture(Image.Item.PIPE)
            placedBlock = BlockType.PIPE
            maxStack = 50
        }

        val SOLIDIFIER = BlockItemType {
            name = "Molten Ore Solidifier"
            icon = Animation.SOLIDIFIER
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
            icon = Texture(Image.Item.COPPER_ORE_ITEM)
            maxStack = 100
        }

        val IRON_ORE = OreItemType {
            name = "Iron Ore"
            moltenForm = MoltenOreFluidType.MOLTEN_IRON
            icon = Texture(Image.Item.IRON_ORE_ITEM)
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
            icon = Texture(Image.Item.IRON_INGOT)
        }

        val COPPER_INGOT = IngotItemType {
            name = "Copper Ingot"
            icon = Texture(Image.Item.COPPER_INGOT)
        }
    }
}