package item

import fluid.MoltenOreFluidType
import graphics.*
import item.weapon.WeaponItemType
import level.block.*
import level.entity.EntityType
import level.entity.robot.RobotType
import resource.ResourceCategory
import resource.ResourceType

open class ItemType(initializer: ItemType.() -> Unit = {}) : ResourceType() {

    override var name = "Error"
    override var icon: Renderable = Texture(Image.Misc.ERROR)

    override val category
        get() = ResourceCategory.ITEM
    var maxStack = 100

    init {
        initializer()
        ALL.add(this)
    }

    override fun toString() = name

    companion object {
        val ALL = mutableListOf<ItemType>()

        val ERROR = ItemType {
            hidden = true
        }

        init {
            EntityItemType
            RobotItemType
            BlockItemType
            OreItemType
            IngotItemType
            WeaponItemType
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
        ALL.add(this)
        spawnedEntity.itemForm = this
    }

    companion object {
        val ALL = mutableListOf<EntityItemType>()
    }
}

class RobotItemType(initializer: RobotItemType.() -> Unit) : EntityItemType() {

    init {
        maxStack = 100
        initializer()
        ALL.add(this)
        spawnedEntity.itemForm = this
    }

    companion object {

        val ALL = mutableListOf<RobotItemType>()

        val STANDARD = RobotItemType {
            name = "Standard Robot"
            icon = Texture(ImageCollection.ROBOT[0])
            spawnedEntity = RobotType.STANDARD
        }
    }
}

class BlockItemType(initializer: BlockItemType.() -> Unit) : ItemType() {
    var placedBlock: BlockType<*> = BlockType.ERROR

    init {
        initializer()
        ALL.add(this)
        placedBlock.itemForm = this
    }

    companion object {

        val ALL = mutableListOf<BlockItemType>()

        val FARSEEKER = BlockItemType {
            name = "Farseeker"
            placedBlock = MachineBlockType.FARSEEKER
        }

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

        val ITEM_PIPE = BlockItemType {
            name = "Item Pipe"
            icon = Texture(Image.Item.TUBE)
            placedBlock = PipeBlockType.ITEM_PIPE
            maxStack = 300
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
            maxStack = 100
        }

        val SMALL_FLUID_TANK = BlockItemType {
            name = "Small Tank"
            placedBlock = FluidTankBlockType.SMALL
        }

        val FLUID_PIPE = BlockItemType {
            name = "Fluid Pipe"
            icon = Texture(Image.Item.PIPE)
            placedBlock = PipeBlockType.FLUID_PIPE
            maxStack = 50
        }

        val SOLIDIFIER = BlockItemType {
            name = "Molten Ore Solidifier"
            icon = Animation.SOLIDIFIER
            placedBlock = MachineBlockType.SOLIDIFIER
        }

        val ARMORY = BlockItemType {
            name = "Armory"
            icon = Texture(Image.Block.ARMORY)
            placedBlock = MachineBlockType.ARMORY
        }
    }
}

class OreItemType(initializer: OreItemType.() -> Unit) : ItemType() {

    var moltenForm = MoltenOreFluidType.MOLTEN_IRON

    init {
        maxStack = 100
        initializer()
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<OreItemType>()

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
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<IngotItemType>()

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