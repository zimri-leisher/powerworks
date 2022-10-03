package level.block

import audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureRegion
import crafting.Crafter
import fluid.FluidTank
import graphics.Animation
import graphics.Image
import graphics.ImageCollection
import graphics.Texture
import item.Inventory
import level.Hitbox
import level.LevelManager
import level.LevelObjectTextures
import level.LevelObjectType
import level.block.BlockType.BlockNodesTemplate
import level.pipe.FluidPipeBlock
import level.pipe.ItemPipeBlock
import level.pipe.PipeBlock
import level.pipe.PipeState
import misc.Geometry
import misc.Geometry.rotate
import resource.*
import routing.script.RoutingLanguage
import screen.gui.*
import serialization.ObjectList
import java.util.*

/**
 * A type of [Block]. All [Block]s have a type, and they define constants between each of their instances. For example,
 * [BlockType.TUBE] defines the [TubeBlock]'s name, default texture and instantiation function. New subclasses of this
 * can define additional parameters, for example, the [MachineBlockType] defines a [MachineBlockType.maxWork] which
 * dictates how quickly machines of each type finish working.
 *
 * To create a new type, all you must do is instantiate this anywhere. Parameters are idiomatically set in the closure
 * accepted by the constructor, but you can really change them anywhere at any time.
 *
 * Here is an example of creating a new type:
 *
 *     val TEST_TYPE = BlockType<DefaultBlock> {
 *         widthTiles = 3
 *         heightTiles = 5
 *         name = "Tester"
 *     }
 *
 * You are able to define [ResourceNode] placement and [ResourceContainer]s--see [BlockNodesTemplate]. Because of the
 * [RoutingLanguage], you can often set most of the resource-based functionality of a [Block] through its type. This is the recommended
 * way to add new features, as opposed to putting code directly inside classes.
 * You are also able to define a [GuiPool] which will allow easy creation and deletion of [Gui]s from a pool
 */
open class BlockType<T : Block>(initializer: BlockType<T>.() -> Unit = {}) : LevelObjectType<T>() {

    /**
     * The user-friendly name of this [BlockType]. This will be displayed in in inventories and other GUIs
     */
    var name = "Error"

    /**
     * How many tiles wide the base of this [BlockType] will be
     */
    var widthTiles = 1

    /**
     * How many tiles high the base of this [BlockType] will be
     */
    var heightTiles = 1

    /**
     * The template that defines [ResourceNode] placement and [ResourceContainer]s. See [BlockNodesTemplate]
     */
    var nodesTemplate = BlockNodesTemplate()

    /**
     * The [GuiPool] for [Block]s of this [BlockType]. This is meant for [Block]s which have a single, standard
     * [Gui] across all instances, and is usually accessed in the [Block.onInteractOn] method (e.g. `guiPool.open(this)`)
     */
    var guiPool: GuiPool<*>? = null

    init {
        instantiate = { x, y, rotation -> DefaultBlock(this as BlockType<DefaultBlock>, x shr 4, y shr 4, rotation) as T }
        hitbox = Hitbox.TILE
        initializer()
        ALL.add(this)
    }

    protected fun nodeTemplate(closure: BlockNodesTemplate.() -> Unit) {
        nodesTemplate.closure()
    }

    override fun toString() = name

    companion object {
        @ObjectList
        val ALL = mutableListOf<BlockType<*>>()

        val ERROR = BlockType<DefaultBlock>()

        init {
            MachineBlockType
            CrafterBlockType
            FluidTankBlockType
            ChestBlockType
        }
    }


    // need a new vision for how this will work
    // first thing:
    // instantiate nodes and containers in the actual classes
    // yes, this means that you can have the same class for different types (allowing diff nodes or containers)
    // however, who cares, it complicates things

    /**
     * A way of storing the positions of nodes for a block type. Define new [ResourceNode]s inside this template by using
     * the [node] function with the appropriate arguments. When a [Block] with this template is placed in a [Level],
     * [instantiate] will be called, which takes [ResourceNode]s from this template and creates them at the
     * [Block], with the correct rotation and position. It additionally [copies][ResourceContainer.copy] new containers
     * as necessary
     */
    inner class BlockNodesTemplate {
        val containers = mutableListOf<ResourceContainer>()
        val nodes = mutableListOf<ResourceNode2>()

        /**
         * Creates a template [ResourceNode]
         * @param xOffset the amount of x tiles offset from the bottom left
         * @param yOffset the amount of y tiles offset from the bottom left
         * @param attachedContainer the [ResourceContainer] this node is attached to
         * @param allowIn the [RoutingLanguage] script determining whether this node allows input. Defaults to "false"
         * @param allowOut the [RoutingLanguage] script determining whether this node allows input. Defaults to "false"
         * @param forceIn the [RoutingLanguage] script determining whether this node will forcibly pull in resources. Defaults to "false"
         * @param forceOut the [RoutingLanguage] script determining whether this node will forcibly push out resources. Defaults to "false"
         * @param allowBehaviorModification whether or not the behavior of this node should be able to be changed by the player
         */
        fun node(xOffset: Int = 0, yOffset: Int = 0,
                 dir: Int = 0,
                 attachedContainer: ResourceContainer,
                 allowIn: String = "false", allowInTypes: List<ResourceType> = listOf(), allowOut: String = "false", allowOutTypes: List<ResourceType> = listOf(),
                 forceIn: String = "false", forceInTypes: List<ResourceType> = listOf(), forceOut: String = "false", forceOutTypes: List<ResourceType> = listOf(),
                 allowBehaviorModification: Boolean = false): ResourceNode2 {
//            val newNode = ResourceNode(xOffset, yOffset, dir, attachedContainer.resourceCategory, attachedContainer, LevelManager.EMPTY_LEVEL)
//            with(newNode.behavior) {
//                this.allowIn.setStatement(RoutingLanguage.parse(allowIn), allowInTypes)
//                this.allowOut.setStatement(RoutingLanguage.parse(allowOut), allowOutTypes)
//                this.forceIn.setStatement(RoutingLanguage.parse(forceIn), forceInTypes)
//                this.forceOut.setStatement(RoutingLanguage.parse(forceOut), forceOutTypes)
//                this.allowModification = allowBehaviorModification
//            }
            val newNode = ResourceNode2(attachedContainer, xOffset, yOffset)
            if (containers.none { it === newNode.container }) {
                containers.add(newNode.container)
            }
            nodes.add(newNode)
            return newNode
        }

        fun container(container: ResourceContainer): ResourceContainer {
            if (containers.none { it === container }) {
                containers.add(container)
            }
            return container
        }

        private fun instantiateContainers(id: UUID): Map<ResourceContainer, ResourceContainer> {
            val rand = Random(id.leastSignificantBits)
            val byteArray = ByteArray(36)
            // generate id based off of block id
            // sort by category to lower chance that reordering messes something up
            containers.sortBy { it.resourceCategory }
            return containers.associateWith {
                rand.nextBytes(byteArray)
                val containerId = UUID.nameUUIDFromBytes(byteArray)
                it.copy().apply { this.id = containerId }
            }
        }

        fun instantiate(xTile: Int, yTile: Int, rotation: Int, id: UUID): List<ResourceNode2> {
            val ret = mutableListOf<ResourceNode2>()
            val containers = instantiateContainers(id)
            // we want these to be sorted in an order that doesn't depend on the order of creation
            // sort it by all the factors that determine a node uniquely.
            // if two nodes in a template have the same values for these, we could have problems.
            // TODO not sure if this is always correct.
            nodes.sortBy { it.xTile }
            nodes.sortBy { it.yTile }
            nodes.sortBy { it.rotation }
            val random = Random(id.mostSignificantBits)
            val byteArray = ByteArray(36)
            for (node in nodes) {
                val coord = rotate(node.xTile, node.yTile, widthTiles, heightTiles, rotation)
                val newContainer = containers.filter { it.key === node.container }.entries.first().value
                random.nextBytes(byteArray)
                val nodeId = UUID.nameUUIDFromBytes(byteArray)
                val newNode = node.copy(coord.xTile + xTile, coord.yTile + yTile, Geometry.addAngles(node.rotation, rotation), attachedContainer = newContainer)
//                if (node.behavior.forceOut != newNode.behavior.forceOut) {
//                    println("oops behaviors diff")
//                }
                newNode.id = nodeId
                ret.add(newNode)
            }
            return ret
        }
    }
}

class PipeBlockType<T : PipeBlock>(initializer: PipeBlockType<T>.() -> Unit = {}) : BlockType<T>() {
    var images: Map<PipeState, TextureRegion> = mapOf()
    var category = ResourceCategory.ITEM

    init {
        initializer()
        ALL.add(this)
    }

    companion object {
        @ObjectList
        val ALL = mutableListOf<PipeBlockType<*>>()

        val FLUID_PIPE = PipeBlockType<FluidPipeBlock> {
            category = ResourceCategory.FLUID
            name = "Pipe"
            textures = LevelObjectTextures(Image.Block.PIPE_2_WAY_VERTICAL)
            instantiate = { x, y, _ -> FluidPipeBlock(x shr 4, y shr 4) }
            images = mapOf(
                    PipeState.ALL to Image.Block.PIPE_4_WAY,
                    PipeState.NONE to Image.Block.PIPE_2_WAY_VERTICAL,
                    PipeState.UP_DOWN to Image.Block.PIPE_2_WAY_VERTICAL,
                    PipeState.DOWN_ONLY to Image.Block.PIPE_2_WAY_VERTICAL,
                    PipeState.UP_ONLY to Image.Block.PIPE_2_WAY_VERTICAL,
                    PipeState.RIGHT_LEFT to Image.Block.PIPE_2_WAY_HORIZONTAL,
                    PipeState.LEFT_ONLY to Image.Block.PIPE_2_WAY_HORIZONTAL,
                    PipeState.RIGHT_ONLY to Image.Block.PIPE_2_WAY_HORIZONTAL,
                    PipeState.UP_RIGHT to ImageCollection.PIPE_CORNER[0],
                    PipeState.RIGHT_DOWN to ImageCollection.PIPE_CORNER[1],
                    PipeState.DOWN_LEFT to ImageCollection.PIPE_CORNER[2],
                    PipeState.LEFT_UP to ImageCollection.PIPE_CORNER[3],
                    PipeState.LEFT_UP_RIGHT to ImageCollection.PIPE_3_WAY[0],
                    PipeState.UP_RIGHT_DOWN to ImageCollection.PIPE_3_WAY[1],
                    PipeState.RIGHT_DOWN_LEFT to ImageCollection.PIPE_3_WAY[2],
                    PipeState.DOWN_LEFT_UP to ImageCollection.PIPE_3_WAY[3]
            )
        }
        val ITEM_PIPE = PipeBlockType<ItemPipeBlock> {
            category = ResourceCategory.ITEM
            name = "Tube"
            textures = LevelObjectTextures(Image.Block.TUBE_2_WAY_VERTICAL)
            instantiate = { x, y, _ -> ItemPipeBlock(x shr 4, y shr 4) }
            images = mapOf(
                    PipeState.ALL to Image.Block.TUBE_4_WAY,
                    PipeState.NONE to Image.Block.TUBE_2_WAY_VERTICAL,
                    PipeState.UP_DOWN to Image.Block.TUBE_2_WAY_VERTICAL,
                    PipeState.DOWN_ONLY to Image.Block.TUBE_2_WAY_VERTICAL,
                    PipeState.UP_ONLY to Image.Block.TUBE_2_WAY_VERTICAL,
                    PipeState.RIGHT_LEFT to Image.Block.TUBE_2_WAY_HORIZONTAL,
                    PipeState.LEFT_ONLY to Image.Block.TUBE_2_WAY_HORIZONTAL,
                    PipeState.RIGHT_ONLY to Image.Block.TUBE_2_WAY_HORIZONTAL,
                    PipeState.UP_RIGHT to ImageCollection.TUBE_CORNER[0],
                    PipeState.RIGHT_DOWN to ImageCollection.TUBE_CORNER[1],
                    PipeState.DOWN_LEFT to ImageCollection.TUBE_CORNER[2],
                    PipeState.LEFT_UP to ImageCollection.TUBE_CORNER[3],
                    PipeState.LEFT_UP_RIGHT to ImageCollection.TUBE_3_WAY[0],
                    PipeState.UP_RIGHT_DOWN to ImageCollection.TUBE_3_WAY[1],
                    PipeState.RIGHT_DOWN_LEFT to ImageCollection.TUBE_3_WAY[2],
                    PipeState.DOWN_LEFT_UP to ImageCollection.TUBE_3_WAY[3]
            )
        }
    }
}

/**
 * A [BlockType] for [MachineBlock]s. Defines various things related to work speed and efficiency, as well as [Sound]s to
 * be played
 */
open class MachineBlockType<T : MachineBlock>(initializer: MachineBlockType<T>.() -> Unit = {}) : BlockType<T>() {
    /**
     * Power consumption multiplier, inverse of this
     */
    var efficiency = 1f

    /**
     * [maxWork] multiplier, inverse of this
     */
    var speed = 1f

    /**
     * The amount of game ticks this takes to complete. Can be modified by [speed]
     */
    var maxWork = 200

    /**
     * Whether [MachineBlock]s of this type should automatically start work once they finish
     */
    var loop = true

    /**
     * The sound this plays while on
     */
    var onSound: Sound? = null

    /**
     * Whether [MachineBlock]s of this type should be on by default
     */
    var startOn = false

    init {
        requiresUpdate = true
        initializer()
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<MachineBlockType<*>>()

        val SMELTER = MachineBlockType<SmelterBlock> {
            instantiate = { x, y, rotation -> SmelterBlock(x shr 4, y shr 4, rotation) }
            name = "Smelter"
            textures = LevelObjectTextures(Image.Block.SMELTER)
            widthTiles = 2
            heightTiles = 2
            hitbox = Hitbox.TILE2X2
            nodeTemplate {
                val internalInventory = Inventory(1, 1)
                node(0, 1, 0, internalInventory, allowIn = "true", allowInTypes = ResourceTypeGroup.ORE_ITEMS.types)
                val internalOutputInventory = Inventory(1, 1)
                node(0, 0, 2, internalOutputInventory, allowOut = "true", forceOut = "true")
            }
            guiPool = GuiPool({ GuiSmelterBlock(it as SmelterBlock) })
        }

        val FARSEEKER = MachineBlockType<FarseekerBlock> {
            instantiate = { x, y, rotation -> FarseekerBlock(x shr 4, y shr 4, rotation) }
            name = "Farseeker"
            textures = LevelObjectTextures(Image.Block.FARSEEKER)
            widthTiles = 4
            heightTiles = 4
            hitbox = Hitbox.TILE4X4
            guiPool = GuiPool({ GuiFarseekerBlock(it as FarseekerBlock) })
        }

        val MINER = MachineBlockType<MinerBlock> {
            name = "Miner"
            instantiate = { x, y, rotation -> MinerBlock(x shr 4, y shr 4, rotation) }
            textures = LevelObjectTextures(Animation.MINER)
            maxWork = 350
            widthTiles = 2
            heightTiles = 2
            hitbox = Hitbox.TILE2X2
            startOn = true
            nodeTemplate {
                // modifying the order these nodes are generated in can potentially
                val internalInventory = Inventory(1, 1)
                node(0, 0, 2, internalInventory, allowOut = "true", forceOut = "true")
            }
            guiPool = GuiPool({ GuiMinerBlock(it as MinerBlock) })
        }

        val FURNACE = MachineBlockType<FurnaceBlock> {
            name = "Furnace"
            instantiate = { x, y, rotation -> FurnaceBlock(this, x shr 4, y shr 4, rotation) }
            widthTiles = 2
            textures = LevelObjectTextures(Image.Block.FURNACE)
            loop = true
            hitbox = Hitbox.TILE2X1
            nodeTemplate {
                val internalInventory = Inventory(1, 1)
                val internalTank = FluidTank(1)
                node(0, 0, 0, internalInventory, "true", ResourceTypeGroup.ORE_ITEMS.types, "false")
                node(0, 0, 2, internalTank, allowIn = "false", allowOut = "true", forceOut = "true")
            }
            guiPool = GuiPool({ GuiFurnaceBlock(it as FurnaceBlock) })
        }
        val SOLIDIFIER = MachineBlockType<SolidifierBlock> {
            name = "Molten Ore Solidifer"
            instantiate = { x, y, rotation -> SolidifierBlock(x shr 4, y shr 4, rotation) }
            widthTiles = 2
            heightTiles = 2
            loop = true
            textures = LevelObjectTextures(Animation.SOLIDIFIER)
            hitbox = Hitbox.TILE2X2
            nodeTemplate {
                val tank = FluidTank(10)
                val out = Inventory(1, 1)
                node(0, 1, 0, tank, "true", ResourceTypeGroup.MOLTEN_ORE_FLUIDS.types, "false")
                node(0, 0, 2, out, "false", allowOut = "true", forceOut = "true")
            }
            guiPool = GuiPool({ GuiSolidifierBlock(it as SolidifierBlock) }, 3)
        }
        val ARMORY = MachineBlockType<ArmoryBlock> {
            name = "Armory"
            textures = LevelObjectTextures(Image.Block.ARMORY)
            instantiate = { x, y, rotation -> ArmoryBlock(x shr 4, y shr 4, rotation) }
            widthTiles = 2
            heightTiles = 2
            loop = true
            startOn = true
            hitbox = Hitbox.TILE2X2
            guiPool = GuiPool({ GuiArmoryBlock(it as ArmoryBlock) }, 3)
            nodeTemplate {
                val inputInv = Inventory(3, 1)
                node(0, 1, 0, inputInv, "true", allowOut = "false")
                node(1, 1, 0, inputInv, "true", allowOut = "false")
                container(Inventory(3, 1))
            }
        }
    }

}

class CrafterBlockType<T : CrafterBlock>(initializer: CrafterBlockType<T>.() -> Unit) : MachineBlockType<T>() {
    var crafterType = Crafter.Type.DEFAULT

    var internalStorageSize = 3

    init {
        widthTiles = 2
        heightTiles = 2
        guiPool = GuiPool({ GuiCrafterBlock(it as CrafterBlock) })
        initializer()
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<CrafterBlockType<*>>()

        val ERROR = CrafterBlockType<CrafterBlock> {
            instantiate = { x, y, rotation -> CrafterBlock(this, x shr 4, y shr 4, rotation) }
            nodeTemplate {
                val input = Inventory(internalStorageSize, 1)
                node(0, 1, 0, input, "true", allowOut = "false")
                val output = Inventory(1, 1)
                node(0, 0, 2, output, "false", allowOut = "true", forceOut = "true")
            }
        }

        val ITEM_CRAFTER = CrafterBlockType<CrafterBlock> {
            name = "Crafter"
            crafterType = Crafter.Type.ITEM
            hitbox = Hitbox.TILE2X2
            instantiate = { x, y, rotation -> CrafterBlock(this, x shr 4, y shr 4, rotation) }
            textures = LevelObjectTextures(Image.Block.CRAFTER)
            nodeTemplate {
                val input = Inventory(internalStorageSize, 1)
                node(0, 1, 0, input, "true", allowOut = "false")
                val output = Inventory(1, 1)
                node(0, 0, 2, output, "false", allowOut = "true", forceOut = "true")
            }
        }

        val ROBOT_FACTORY = CrafterBlockType<RobotFactoryBlock> {
            name = "Robot Factory"
            crafterType = Crafter.Type.ROBOT
            instantiate = { x, y, rotation -> RobotFactoryBlock(x shr 4, y shr 4, rotation) }
            widthTiles = 3
            heightTiles = 3
            internalStorageSize = 3
            textures = LevelObjectTextures(Texture(Image.Block.ROBOT_CRAFTER, xOffset = -8))
            hitbox = Hitbox.TILE3X3
            nodeTemplate {
                val internalInventory = Inventory(internalStorageSize, 1)
                node(0, 1, 3, internalInventory, "true", allowOut = "false")
                node(2, 1, 1, internalInventory, "true", allowOut = "false")
                val output = Inventory(1, 1)
                node(1, 0, 2, output, "false", allowOut = "true", forceOut = "true")
            }
        }
    }

}

class FluidTankBlockType(initializer: FluidTankBlockType.() -> Unit) : BlockType<FluidTankBlock>() {

    var maxAmount = 1

    init {
        instantiate = { x, y, rotation -> FluidTankBlock(this, x shr 4, y shr 4, rotation) }
        nodeTemplate {
            val storage = FluidTank(maxAmount)
            node(0, 0, 0, storage, "true", allowOut = "true")
            node(0, 0, 1, storage, "true", allowOut = "true")
            node(0, 0, 2, storage, "true", allowOut = "true")
            node(0, 0, 3, storage, "true", allowOut = "true")
        }
//        guiPool = BlockGUIPool({ FluidTankBlockGUI(it as FluidTankBlock) }, 3)
        initializer()
        ALL.add(this)
    }

    companion object {

        val ALL = mutableListOf<FluidTankBlockType>()

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
        instantiate = { x, y, rotation -> ChestBlock(this, x shr 4, y shr 4, rotation) }
        guiPool = GuiPool({ GuiChestBlock(it as ChestBlock) })
        initializer()
        nodeTemplate {
            val storage = Inventory(invWidth, invHeight)
            node(0, 0, 0, storage, "true", allowOut = "true")
            node(0, 0, 1, storage, "true", allowOut = "true")
            node(0, 0, 2, storage, "true", allowOut = "true")
            node(0, 0, 3, storage, "true", allowOut = "true")
        }
        ALL.add(this)
    }

    companion object {
        @ObjectList
        val ALL = mutableListOf<ChestBlockType>()

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