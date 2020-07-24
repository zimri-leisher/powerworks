package screen.mouse.tool

import com.badlogic.gdx.graphics.Color
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.ControlPressHandler
import io.PressType
import item.BlockItemType
import level.LevelManager
import level.block.BlockType
import level.canAdd
import level.getCollisionsWith
import main.toColor
import player.PlaceLevelObject
import player.PlayerManager
import screen.mouse.Mouse

object BlockPlacer : Tool(Control.PLACE_BLOCK, Control.ROTATE_BLOCK), ControlPressHandler {
    var type: BlockItemType? = null

    var xTile = 0
    var yTile = 0
    var rotation = 0
    var canPlace = false

    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            if (control == Control.ROTATE_BLOCK) {
                rotation = (rotation + 1) % 4
            }
        }
        if (control == Control.PLACE_BLOCK && type != PressType.RELEASED) {
            if (canPlace) {
                val blockType = BlockPlacer.type!!.placedBlock
                PlayerManager.takeAction(PlaceLevelObject(PlayerManager.localPlayer, blockType, xTile shl 4, yTile shl 4, rotation, LevelManager.levelUnderMouse!!))
                canPlace = false
            }
        }
    }

    override fun updateCurrentlyActive() {
        val item = Mouse.heldItemType
        currentlyActive = LevelManager.levelObjectUnderMouse == null && item is BlockItemType && item.placedBlock != BlockType.ERROR && !Selector.dragging
        if (currentlyActive) {
            type = item as BlockItemType
        } else {
            type = null
        }
    }

    override fun update() {
        if (LevelManager.levelUnderMouse == null)
            return
        xTile = (LevelManager.mouseLevelXTile) - type!!.placedBlock.widthTiles / 2
        yTile = (LevelManager.mouseLevelYTile) - type!!.placedBlock.heightTiles / 2
        val level = LevelManager.levelUnderMouse!!
        canPlace = level.canAdd(type!!.placedBlock, xTile shl 4, yTile shl 4)
                && level.getCollisionsWith(level.data.ghostObjects, type!!.placedBlock.hitbox, xTile shl 4, yTile shl 4).isEmpty()
    }

    override fun renderAbove() {
        if (currentlyActive) {
            val blockType = type!!.placedBlock
            val xPixel = xTile shl 4
            val yPixel = yTile shl 4
            blockType.textures.render(xPixel, yPixel, rotation, TextureRenderParams(color = Color(1f, 1f, 1f, 0.4f)))
            Renderer.renderEmptyRectangle(xPixel, yPixel, blockType.widthTiles shl 4, blockType.heightTiles shl 4, params = TextureRenderParams(color = toColor(if (canPlace) 0x04C900 else 0xC90004, 0.3f)))
            Renderer.renderTextureKeepAspect(Image.Misc.ARROW, xPixel, yPixel, blockType.widthTiles shl 4, blockType.heightTiles shl 4, TextureRenderParams(rotation = 90f * rotation))
        }
    }
}