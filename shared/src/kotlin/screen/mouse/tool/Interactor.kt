package screen.mouse.tool

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.InputManager
import io.PressType
import level.LevelManager
import level.block.Block
import level.moving.MovingObject
import screen.mouse.Mouse

object Interactor : Tool(Control.Group.INTERACTION.controls) {
    init {
        activationPredicate = {
            LevelManager.levelObjectUnderMouse?.isInteractable == true && !Selector.dragging && !BlockPlacer.hasPlacedThisInteraction
        }
    }
    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (control in Control.Group.SCROLL) {
            LevelManager.levelObjectUnderMouse!!.onScroll(if (control == Control.SCROLL_DOWN) -1 else 1)
        } else {
            LevelManager.levelObjectUnderMouse!!.onInteractOn(type, mouseLevelXPixel, mouseLevelYPixel, Mouse.button,
                    InputManager.inputsBeingPressed.contains("SHIFT"), InputManager.inputsBeingPressed.contains("CONTROL"), InputManager.inputsBeingPressed.contains("ALT"))
            LevelManager.onInteractWithLevelObjectUnderMouse()
        }
        return true
    }

    override fun renderBelow() {
        val s = LevelManager.levelObjectUnderMouse
        if (s is Block)
            Renderer.renderEmptyRectangle(s.xPixel - 1, s.yPixel - 1, (s.type.widthTiles shl 4) + 2, (s.type.heightTiles shl 4) + 2, params = TextureRenderParams(color = Color(0x1A6AF472)))
        else if (s is MovingObject)
            Renderer.renderEmptyRectangle(s.xPixel + s.hitbox.xStart - 1, s.yPixel + s.hitbox.yStart - 1, s.hitbox.width + 2, s.hitbox.height + 2, params = TextureRenderParams(color = Color(0x1A6AF472)))
    }
}