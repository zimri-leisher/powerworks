package level.block

import audio.Sound
import crafting.Crafter
import fluid.FluidTank
import fluid.MoltenOreFluidType
import graphics.Animation
import graphics.Image
import item.Inventory
import item.OreItemType
import level.Hitbox
import level.LevelObjectTexture
import level.LevelObjectTextures
import level.LevelObjectType
import level.pipe.PipeBlock
import level.tube.TubeBlock
import resource.ResourceCategory
import resource.ResourceNode

open class BlockType<T : Block>(initializer: BlockType<T>.() -> Unit = {}) : LevelObjectType<T>() {
    var textures = LevelObjectTextures(LevelObjectTexture(Image.Misc.ERROR))
    var name = "Error"
    var widthTiles = 1
    var heightTiles = 1

    var nodesTemplate = BlockNodesTemplate.NONE

    init {
        instantiate = { xPixel, yPixel, rotation -> DefaultBlock(this as BlockType<DefaultBlock>, xPixel shr 4, yPixel shr 4, rotation) as T }
        hitbox = Hitbox.TILE
        initializer()
        ALL.add(this)
    }

    protected fun template(closure: () -> List<ResourceNode<*>>) = BlockNodesTemplate(widthTiles, heightTiles, closure())

    override fun toString() = name

    companion object {

        val ALL = mutableListOf<BlockType<*>>()

        val ERROR = BlockType<DefaultBlock>()

        val TUBE = BlockType<TubeBlock> {
            name = "Tube"
            textures = LevelObjectTextures(LevelObjectTexture(Image.Block.TUBE_2_WAY_VERTICAL))
            instantiate = { xPixel, yPixel, _ -> TubeBlock(xPixel shr 4, yPixel shr 4) }
        }
        val PIPE = BlockType<PipeBlock> {
            name = "Pipe"
            textures = LevelObjectTextures(LevelObjectTexture(Image.Block.PIPE_2_WAY_VERTICAL))
            instantiate = { xPixel, yPixel, _ -> PipeBlock(xPixel shr 4, yPixel shr 4) }
        }
    }

}

open class MachineBlockType<T : MachineBlock>(initializer: MachineBlockType<T>.() -> Unit = {}) : BlockType<T>() {
    /**
     * Power consumption multiplier, inverse of this
     */
    var efficiency = 1f
    var speed = 1f
    var maxWork = 200
    var loop = true
    var onSound: Sound? = null

    var defaultOn = false

    init {
        requiresUpdate = true
        initializer()
    }

    companion object {

        val MINER = MachineBlockType<MinerBlock> {
            name = "Miner"
            instantiate = { xPixel, yPixel, rotation -> MinerBlock(xPixel shr 4, yPixel shr 4, rotation) }
            textures = LevelObjectTextures(LevelObjectTexture(Animation.MINER[0]))
            widthTiles = 2
            heightTiles = 2
            hitbox = Hitbox.TILE2X2
            defaultOn = true
            val internalInventory = Inventory(1, 1)
            internalInventory.additionRule = { _, _ -> internalInventory.totalQuantity < 1 }
            nodesTemplate = template {
                listOf(
                        ResourceNode(0, 1, 0, ResourceCategory.ITEM, false, true, internalInventory)
                )
            }
        }

        val FURNACE = MachineBlockType<FurnaceBlock> {
            name = "Furnace"
            instantiate = { xPixel, yPixel, rotation -> FurnaceBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
            widthTiles = 2
            requiresUpdate = true
            textures = LevelObjectTextures(LevelObjectTexture(Image.Block.FURNACE))
            loop = true
            hitbox = Hitbox.TILE2X1
            nodesTemplate = template {
                val internalInventory = Inventory(1, 1)
                val internalTank = FluidTank(1)
                internalInventory.typeRule = { it is OreItemType }
                listOf(
                        ResourceNode(0, 0, 0, ResourceCategory.ITEM, true, false, internalInventory),
                        ResourceNode(1, 0, 2, ResourceCategory.FLUID, false, true, internalTank).apply { outputToLevel = false }
                )
            }
        }
        val SOLIDIFIER = MachineBlockType<SolidifierBlock> {
            name = "Molten Ore Solidifer"
            instantiate = { xPixel, yPixel, rotation -> SolidifierBlock(xPixel shr 4, yPixel shr 4, rotation) }
            widthTiles = 2
            heightTiles = 2
            loop = true
            textures = LevelObjectTextures(LevelObjectTexture(Animation.SOLIDIFIER[0]))
            hitbox = Hitbox.TILE2X2
            nodesTemplate = template {
                val tank = FluidTank(10, { it is MoltenOreFluidType })
                val out = Inventory(1, 1)
                listOf(
                        ResourceNode(1, 1, 0, ResourceCategory.FLUID, true, false, tank),
                        ResourceNode(1, 0, 2, ResourceCategory.ITEM, false, true, out)
                )
            }
        }
    }

}

class CrafterBlockType(initializer: CrafterBlockType.() -> Unit) : MachineBlockType<CrafterBlock>() {
    var craftingType = Crafter.Type.ITEM

    var internalStorageSize = 2

    init {
        widthTiles = 2
        heightTiles = 2
        initializer()
    }

    companion object {
        val ITEM_CRAFTER = CrafterBlockType {
            name = "Crafter"
            hitbox = Hitbox.TILE2X2
            instantiate = { xPixel, yPixel, rotation -> CrafterBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
            textures = LevelObjectTextures(LevelObjectTexture(Image.Block.CRAFTER))
            nodesTemplate = template {
                val internalInventory = Inventory(internalStorageSize, 1)
                listOf(
                        ResourceNode(0, 1, 0, ResourceCategory.ITEM, true, false, internalInventory),
                        ResourceNode(1, 0, 2, ResourceCategory.ITEM, false, true, internalInventory)
                )
            }
        }

        val ROBOT_FACTORY = CrafterBlockType {
            name = "Robot Factory"
            craftingType = Crafter.Type.ROBOT
            instantiate = { xPixel, yPixel, rotation -> CrafterBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
            widthTiles = 3
            heightTiles = 3
            hitbox = Hitbox.TILE2X2
            nodesTemplate = template {
                val internalInventory = Inventory(1, 1)
                listOf(
                        ResourceNode(0, 0, 0, ResourceCategory.ITEM, true, false, internalInventory)
                )
            }
        }
    }

}

class FluidTankBlockType(initializer: FluidTankBlockType.() -> Unit) : BlockType<FluidTankBlock>() {

    var maxAmount = 1

    init {
        instantiate = { xPixel, yPixel, rotation -> FluidTankBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
        initializer()
        val storage = FluidTank(maxAmount)
        nodesTemplate = template {
            listOf(
                    ResourceNode(0, 0, 0, ResourceCategory.FLUID, true, true, storage),
                    ResourceNode(0, 0, 1, ResourceCategory.FLUID, true, true, storage),
                    ResourceNode(0, 0, 2, ResourceCategory.FLUID, true, true, storage),
                    ResourceNode(0, 0, 3, ResourceCategory.FLUID, true, true, storage))
        }
    }

    companion object {
        val SMALL = FluidTankBlockType {
            name = "Small Tank"
            maxAmount = 20
        }
    }

}

class ChestBlockType(initializer: ChestBlockType.() -> Unit) : BlockType<ChestBlock>() {
    var invWidth = 1
    var invHeight = 1
    var invName = "Chest"

    init {
        instantiate = { xPixel, yPixel, rotation -> ChestBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
        initializer()
        val storage = Inventory(invWidth, invHeight)
        nodesTemplate = template {
            listOf(
                    ResourceNode(0, 0, 0, ResourceCategory.ITEM, true, true, storage),
                    ResourceNode(0, 0, 1, ResourceCategory.ITEM, true, true, storage),
                    ResourceNode(0, 0, 2, ResourceCategory.ITEM, true, true, storage),
                    ResourceNode(0, 0, 3, ResourceCategory.ITEM, true, true, storage)
            )
        }
    }

    companion object {
        val SMALL = ChestBlockType {
            name = "Small chest"
            invName = "Small chest"
            textures = LevelObjectTextures(LevelObjectTexture(Image.Block.CHEST_SMALL))
            invWidth = 8
            invHeight = 3
        }
        val LARGE = ChestBlockType {
            name = "Large chest"
            invName = "Large chest"
            textures = LevelObjectTextures(LevelObjectTexture(Image.Block.CHEST_LARGE))
            invWidth = 8
            invHeight = 6
        }
    }
}