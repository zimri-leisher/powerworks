package screen.mouse.tool

import com.badlogic.gdx.graphics.Color
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.*
import item.BlockItemType
import level.Level
import level.LevelManager
import level.block.BlockType
import level.getCollisionsWith
import main.GameState
import main.toColor
import misc.Geometry
import player.ActionLevelObjectPlace
import player.PlayerManager
import screen.gui.GuiTutorial
import screen.gui.TutorialStage
import screen.mouse.Mouse

object BlockPlacer : Tool(Control.PLACE_BLOCK), ControlEventHandler {
    var type: BlockItemType? = null

    var xTile = 0
    var yTile = 0
    var rotation = 0
    var canPlace = false
    var hasPlacedThisInteraction = false

    init {
        InputManager.register(this, Control.ROTATE_BLOCK)
        activationPredicate = {
            val item = Mouse.heldItemType
            hasPlacedThisInteraction || (LevelManager.levelObjectUnderMouse == null && item is BlockItemType && item.placedBlock != BlockType.ERROR && !Selector.dragging)
        }
    }

    override fun onUse(event: ControlEvent, mouseLevelX: Int, mouseLevelY: Int): Boolean {
        if (event.control == Control.PLACE_BLOCK) {
            if (event.type != ControlEventType.RELEASE) {
                if (canPlace && LevelManager.levelUnderMouse != null) {
                    val blockType = type!!.placedBlock
                    if(GuiTutorial.currentTutorialStage == TutorialStage.PLACE_BLOCK) {
                        GuiTutorial.showNextStage()
                    }
                    PlayerManager.takeAction(ActionLevelObjectPlace(PlayerManager.localPlayer, blockType, xTile shl 4, yTile shl 4, rotation, LevelManager.levelUnderMouse!!))
                    canPlace = false
                    hasPlacedThisInteraction = true
                    return true
                }
            } else {
                val temp = hasPlacedThisInteraction
                hasPlacedThisInteraction = false
                return temp
            }
        }
        return false
    }

    override fun handleControlEvent(event: ControlEvent) {
        if (GameState.currentState == GameState.INGAME && event.type == ControlEventType.PRESS) {
            if (event.control == Control.ROTATE_BLOCK) {
                rotation = (rotation + 1) % 4
            }
        }
    }

    override fun update() {
        canPlace = false
        if (LevelManager.levelUnderMouse == null)
            return
        type = Mouse.heldItemType as? BlockItemType
        if (type != null) {
            xTile = (LevelManager.mouseLevelXTile) - type!!.placedBlock.widthTiles / 2
            yTile = (LevelManager.mouseLevelYTile) - type!!.placedBlock.heightTiles / 2
            val level = LevelManager.levelUnderMouse!!
            val collisions = level.getCollisionsWith(type!!.placedBlock.hitbox, xTile shl 4, yTile shl 4) +
                    level.data.ghostObjects.getCollisionsWith(type!!.placedBlock.hitbox, xTile shl 4, yTile shl 4)
            canPlace = collisions.none()
        }
    }

    override fun renderAbove(level: Level) {
        if (level == LevelManager.levelUnderMouse) {
            if (type != null) {
                val blockType = type!!.placedBlock
                val x = xTile shl 4
                val y = yTile shl 4
                blockType.textures.render(x, y, rotation, TextureRenderParams(color = Color(1f, 1f, 1f, 0.4f)))
                for (node in blockType.nodesTemplate.nodes) {
                    val (rotatedXTile, rotatedYTile) = Geometry.rotate(node.xTile, node.yTile, blockType.widthTiles, blockType.heightTiles, rotation)
                    node.render(xTile + rotatedXTile, yTile + rotatedYTile, Geometry.addAngles(rotation, node.dir))
                }
                Renderer.renderEmptyRectangle(x, y, blockType.widthTiles shl 4, blockType.heightTiles shl 4, params = TextureRenderParams(color = toColor(if (canPlace) 0x04C900 else 0xC90004, 0.3f)))
                Renderer.renderTextureKeepAspect(Image.Misc.ARROW, x, y, blockType.widthTiles shl 4, blockType.heightTiles shl 4, TextureRenderParams(rotation = -90f * rotation + 180f))
            }
        }
    }
}