package level.block

import graphics.Image
import graphics.Texture
import level.Hitbox

open class BlockTemplate(init: BlockTemplate.() -> Unit = {}) {
    var textures: Array<Texture> = arrayOf(Image.Misc.ERROR)
    var name = "Error"
    var widthTiles = 1
    var heightTiles = 1
    var hitbox = Hitbox.TILE
    var nodeTemplate = BlockNodesTemplate.NONE

    init {
        init()
    }

    /**
     * Called when a block with this type is initialized but not when it is added to the level
     */
    var onInit: () -> Unit = {}
    /**
     * 1: x tile
     * 2: y tile
     * 3: rotation
     */
    var onAddToLevel: (Int, Int, Int) -> Unit = { _, _, _ -> }
    /**
     * 1: x tile
     * 2: y tile
     */
    var onRemoveFromLevel: (Int, Int) -> Unit = { _, _ -> }

    companion object {
        val ERROR = BlockTemplate()
        val TUBE = BlockTemplate {
            name = "Tube"
            textures = arrayOf(Image.Block.TUBE_2_WAY_VERTICAL)
        }
    }
}

class ChestBlockTemplate(init: ChestBlockTemplate.() -> Unit) : BlockTemplate() {
    var invWidth = 1
    var invHeight = 1

    init {
        init()
    }

    companion object {
        val CHEST_SMALL = ChestBlockTemplate {
            name = "Small chest"
            textures = arrayOf(Image.Block.CHEST_SMALL)
            invWidth = 8
            invHeight = 3
            onAddToLevel = { xTile, yTile, rot ->

            }
        }
    }
}