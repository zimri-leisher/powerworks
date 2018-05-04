package level.block

import audio.Sound
import crafting.Crafter
import graphics.Image
import graphics.LocalAnimation
import graphics.SyncAnimation
import graphics.Texture
import item.Inventory
import item.ItemType
import level.Hitbox
import level.LevelObjectType
import level.tube.TubeBlock
import resource.ResourceNode
import resource.ResourceType

data class BlockTexture(val texture: Texture, val xPixelOffset: Int = 0, val yPixelOffset: Int = 0)

class BlockTextures(private vararg val textures: BlockTexture) {
    operator fun get(i: Int) = textures[Math.min(i, textures.lastIndex)]
    operator fun iterator() = textures.iterator()
}

open class BlockType<T : Block>(init: BlockType<T>.() -> Unit = {}) : LevelObjectType<T>() {

    var textures = BlockTextures(BlockTexture(Image.Misc.ERROR))
    var name = "Error"
    var widthTiles = 1
    var heightTiles = 1
    var requiresUpdate = false
    var hitbox = Hitbox.TILE
    var nodesTemplate = BlockNodesTemplate.NONE

    init {
        instantiate = { xTile, yTile, rotation -> DefaultBlock(this as BlockType<DefaultBlock>, xTile, yTile, rotation) as T }
        init()
        ALL.add(this)
    }

    override fun toString() = name

    companion object {
        val ALL = mutableListOf<BlockType<*>>()
        val ERROR = BlockType<DefaultBlock>()
        val TUBE = BlockType<TubeBlock> {
            name = "Tube"
            textures = BlockTextures(BlockTexture(Image.Block.TUBE_2_WAY_VERTICAL))
            instantiate = { xTile, yTile, rotation -> TubeBlock(xTile, yTile) }
        }
    }
}

open class MachineBlockType<T : MachineBlock>(init: MachineBlockType<T>.() -> Unit = {}) : BlockType<T>() {
    /**
     * Power consumption multiplier, inverse of this
     */
    var efficiency = 1f
    var speed = 1f
    var maxWork = 200
    var loop = true
    var onSound: Sound? = null

    init {
        init()
    }

    companion object {
        val MINER = MachineBlockType<MinerBlock> {
            name = "Miner"
            instantiate = { xTile, yTile, rotation -> MinerBlock(xTile, yTile, rotation) }
            textures = BlockTextures(BlockTexture(LocalAnimation(SyncAnimation.MINER, true), yPixelOffset = 32))
            widthTiles = 2
            heightTiles = 2
            requiresUpdate = true
            hitbox = Hitbox.TILE2X2
            nodesTemplate = BlockNodesTemplate(widthTiles, heightTiles) {
                listOf(
                        ResourceNode<ItemType>(0, 0, 0, false, true, ResourceType.ITEM)
                )
            }
        }
    }
}

class CrafterBlockType(init: CrafterBlockType.() -> Unit) : MachineBlockType<CrafterBlock>() {
    var craftingType = Crafter.ITEM_CRAFTER
    var internalStorageSize = 2

    init {
        widthTiles = 2
        heightTiles = 2
        init()
    }

    companion object {
        val ITEM_CRAFTER = CrafterBlockType {
            name = "Crafter"
            hitbox = Hitbox.TILE2X2
            instantiate = { xTile, yTile, rotation -> CrafterBlock(this, xTile, yTile, rotation) }
            textures = BlockTextures(BlockTexture(Image.Block.CRAFTER, yPixelOffset = 25))
            requiresUpdate = true
            nodesTemplate = BlockNodesTemplate(widthTiles, heightTiles) {
                val internalInventory = Inventory(internalStorageSize, 1)
                listOf(
                        ResourceNode(0, 0, 0, true, false, ResourceType.ITEM, internalInventory),
                        ResourceNode(1, 1, 2, false, true, ResourceType.ITEM, internalInventory)
                )
            }
        }
    }
}

class ChestBlockType(init: ChestBlockType.() -> Unit) : BlockType<ChestBlock>() {
    var invWidth = 1
    var invHeight = 1
    var invName = "Chest"

    init {
        instantiate = { xTile, yTile, rotation -> ChestBlock(this, xTile, yTile, rotation) }
        init()
        val storage = Inventory(invWidth, invHeight)
        nodesTemplate = BlockNodesTemplate(widthTiles, heightTiles) {
            listOf(
                    ResourceNode(0, 0, 0, true, true, ResourceType.ITEM, storage),
                    ResourceNode(0, 0, 1, true, true, ResourceType.ITEM, storage),
                    ResourceNode(0, 0, 2, true, true, ResourceType.ITEM, storage),
                    ResourceNode(0, 0, 3, true, true, ResourceType.ITEM, storage))
        }
    }

    companion object {
        val CHEST_SMALL = ChestBlockType {
            name = "Small chest"
            invName = "Small chest"
            textures = BlockTextures(BlockTexture(Image.Block.CHEST_SMALL, yPixelOffset = 16))
            invWidth = 8
            invHeight = 3
        }
        val CHEST_LARGE = ChestBlockType {
            name = "Large chest"
            invName = "Large chest"
            textures = BlockTextures(BlockTexture(Image.Block.CHEST_LARGE, yPixelOffset = 16))
            invWidth = 8
            invHeight = 6
        }
    }
}