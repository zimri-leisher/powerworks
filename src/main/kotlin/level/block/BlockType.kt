package level.block

import audio.Sound
import crafting.Crafter
import fluid.FluidTank
import fluid.MoltenOreFluidType
import graphics.Animation
import graphics.Image
import graphics.Texture
import item.Inventory
import item.OreItemType
import level.Hitbox
import level.LevelObjectTextures
import level.LevelObjectType
import level.pipe.PipeBlock
import level.tube.TubeBlock
import misc.Geometry
import misc.Geometry.rotate
import resource.ResourceContainer
import resource.ResourceNode
import resource.ResourceType
import routing.RoutingLanguage
import routing.RoutingLanguageStatement
import screen.*
import screen.elements.BlockGUI

open class BlockType<T : Block>(initializer: BlockType<T>.() -> Unit = {}) : LevelObjectType<T>() {

    var name = "Error"
    var widthTiles = 1
    var heightTiles = 1
    var nodesTemplate = BlockNodesTemplate()
    var guiPool: BlockGUIPool<*>? = null

    init {
        instantiate = { xPixel, yPixel, rotation -> DefaultBlock(this as BlockType<DefaultBlock>, xPixel shr 4, yPixel shr 4, rotation) as T }
        hitbox = Hitbox.TILE
        initializer()
        ALL.add(this)
    }

    protected fun nodeTemplate(closure: BlockNodesTemplate.() -> Unit) {
        nodesTemplate.closure()
    }

    override fun toString() = name

    companion object {
        val ALL = mutableListOf<BlockType<*>>()

        val ERROR = BlockType<DefaultBlock>()

        val TUBE = BlockType<TubeBlock> {
            name = "Tube"
            textures = LevelObjectTextures(Image.Block.TUBE_2_WAY_VERTICAL)
            instantiate = { xPixel, yPixel, _ -> TubeBlock(xPixel shr 4, yPixel shr 4) }
        }

        val PIPE = BlockType<PipeBlock> {
            name = "Pipe"
            textures = LevelObjectTextures(Image.Block.PIPE_2_WAY_VERTICAL)
            instantiate = { xPixel, yPixel, _ -> PipeBlock(xPixel shr 4, yPixel shr 4) }
        }
    }

    /**
     * A way of storing the positions of nodes for a block type. This will be instantiated at an offset for any block placed
     * at any rotation in the level (if it has nodes)
     */
    inner class BlockNodesTemplate {
        val containers = mutableListOf<ResourceContainer>()
        val nodes = mutableListOf<ResourceNode>()

        fun node(xOffset: Int = 0, yOffset: Int = 0,
                                    dir: Int = 0,
                                    attachedContainer: ResourceContainer,
                                    allowIn: String = "false", allowOut: String = "false",
                                    forceIn: String = "false", forceOut: String = "false",
                                    allowBehaviorModification: Boolean = false): ResourceNode {
            val r = ResourceNode(xOffset, yOffset, dir, attachedContainer.resourceCategory, attachedContainer)
            with(r.behavior) {
                this.allowIn.setStatement(RoutingLanguage.parse(allowIn), null)
                this.allowOut.setStatement(RoutingLanguage.parse(allowOut), null)
                this.forceIn.setStatement(RoutingLanguage.parse(forceIn), null)
                this.forceOut.setStatement(RoutingLanguage.parse(forceOut), null)
                this.allowModification = allowBehaviorModification
            }
            if (containers.none { it === attachedContainer }) {
                containers.add(attachedContainer)
            }
            nodes.add(r)
            return r
        }

        private fun instantiateContainers() = containers.associateWith { it.copy() }

        fun instantiate(xTile: Int, yTile: Int, dir: Int): List<ResourceNode> {
            // todo stop this from being called every tick with a ghost block out
            val ret = mutableListOf<ResourceNode>()
            val containers = instantiateContainers()
            for (node in nodes) {
                val coord = rotate(node.xTile, node.yTile, widthTiles, heightTiles, dir)
                val newContainer = containers.filter { it.key === node.attachedContainer }.entries.first().value
                val newNode = node.copy(coord.xTile + xTile, coord.yTile + yTile, Geometry.addAngles(node.dir, dir), attachedContainer = newContainer)
                if(node.behavior.forceOut != newNode.behavior.forceOut) {
                    println("oopps behaviors diff")
                }
                ret.add(newNode)
            }
            return ret
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
            textures = LevelObjectTextures(Animation.MINER)
            widthTiles = 2
            heightTiles = 2
            hitbox = Hitbox.TILE2X2
            defaultOn = true
            val internalInventory = Inventory(1, 1)
            internalInventory.additionRule = { _, _ -> totalQuantity < 1 }
            nodeTemplate {
                node(0, 1, 0, internalInventory, allowOut = "true", forceOut = "total quantity > 0")
            }
        }

        val FURNACE = MachineBlockType<FurnaceBlock> {
            name = "Furnace"
            instantiate = { xPixel, yPixel, rotation -> FurnaceBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
            widthTiles = 2
            requiresUpdate = true
            textures = LevelObjectTextures(Image.Block.FURNACE)
            loop = true
            hitbox = Hitbox.TILE2X1
            nodeTemplate {
                val internalInventory = Inventory(1, 1)
                val internalTank = FluidTank(1)
                internalInventory.typeRule = { it is OreItemType }
                node(0, 0, 0, internalInventory, "true", "false")
                node(1, 0, 2, internalTank, "false", "true").outputToLevel = false
            }
            guiPool = BlockGUIPool({ FurnaceBlockGUI(it as FurnaceBlock) }, 3)
        }
        val SOLIDIFIER = MachineBlockType<SolidifierBlock> {
            name = "Molten Ore Solidifer"
            instantiate = { xPixel, yPixel, rotation -> SolidifierBlock(xPixel shr 4, yPixel shr 4, rotation) }
            widthTiles = 2
            heightTiles = 2
            loop = true
            textures = LevelObjectTextures(Animation.SOLIDIFIER)
            hitbox = Hitbox.TILE2X2
            nodeTemplate {
                val tank = FluidTank(10, { it is MoltenOreFluidType })
                val out = Inventory(1, 1)
                node(1, 1, 0, tank, "true", "false")
                node(1, 0, 2, out, "false", "true")
            }
            guiPool = BlockGUIPool({ SolidifierBlockGUI(it as SolidifierBlock) }, 3)
        }
    }

}

class CrafterBlockType(initializer: CrafterBlockType.() -> Unit) : MachineBlockType<CrafterBlock>() {
    var crafterType = Crafter.Type.DEFAULT

    var internalStorageSize = 3

    init {
        widthTiles = 2
        heightTiles = 2
        guiPool = BlockGUIPool({ CrafterBlockGUI(it as CrafterBlock) })
        initializer()
    }

    companion object {
        val ITEM_CRAFTER = CrafterBlockType {
            name = "Crafter"
            crafterType = Crafter.Type.ITEM
            hitbox = Hitbox.TILE2X2
            instantiate = { xPixel, yPixel, rotation -> CrafterBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
            textures = LevelObjectTextures(Image.Block.CRAFTER)
            nodeTemplate {
                val internalInventory = Inventory(internalStorageSize, 1)
                node(0, 1, 0, internalInventory, "true", "false")
                node(1, 0, 2, internalInventory, "false", "true")
            }
        }

        val ROBOT_FACTORY = CrafterBlockType {
            name = "Robot Factory"
            crafterType = Crafter.Type.ROBOT
            instantiate = { xPixel, yPixel, rotation -> CrafterBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
            widthTiles = 3
            heightTiles = 3
            textures = LevelObjectTextures(Texture(Image.Block.ROBOT_CRAFTER, xPixelOffset = -8))
            hitbox = Hitbox.TILE3X3
            nodeTemplate {
                val internalInventory = Inventory(1, 1)
                node(0, 1, 3, internalInventory, "true", "false")
                node(2, 1, 1, internalInventory, "true", "false")
                node(1, 0, 2, internalInventory, "false", "true")
            }
        }
    }

}

class FluidTankBlockType(initializer: FluidTankBlockType.() -> Unit) : BlockType<FluidTankBlock>() {

    var maxAmount = 1

    init {
        instantiate = { xPixel, yPixel, rotation -> FluidTankBlock(this, xPixel shr 4, yPixel shr 4, rotation) }
        nodeTemplate {
            val storage = FluidTank(maxAmount)
            node(0, 0, 0, storage, "true", "true")
            node(0, 0, 1, storage, "true", "true")
            node(0, 0, 2, storage, "true", "true")
            node(0, 0, 3, storage, "true", "true")
        }
        guiPool = BlockGUIPool({ FluidTankBlockGUI(it as FluidTankBlock) }, 3)
        initializer()
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
        guiPool = BlockGUIPool({ ChestBlockGUI(it as ChestBlock) })
        initializer()
        nodeTemplate {
            val storage = Inventory(invWidth, invHeight)
            node(0, 0, 0, storage, "true", "true")
            node(0, 0, 1, storage, "true", "true")
            node(0, 0, 2, storage, "true", "true")
            node(0, 0, 3, storage, "true", "true")
        }
    }

    companion object {
        val SMALL = ChestBlockType {
            name = "Small chest"
            invName = "Small chest"
            textures = LevelObjectTextures(Image.Block.CHEST_SMALL)
            invWidth = 8
            invHeight = 3
        }
        val LARGE = ChestBlockType {
            name = "Large chest"
            invName = "Large chest"
            textures = LevelObjectTextures(Image.Block.CHEST_LARGE)
            invWidth = 8
            invHeight = 6
        }
    }
}