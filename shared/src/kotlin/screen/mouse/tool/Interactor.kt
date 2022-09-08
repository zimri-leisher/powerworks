package screen.mouse.tool

import com.badlogic.gdx.graphics.Color
import graphics.Renderer
import graphics.TextureRenderParams
import io.Control
import io.ControlEvent
import io.InputManager
import io.Modifier
import level.Level
import level.LevelManager
import level.block.ArmoryBlock
import level.block.Block
import level.moving.MovingObject
import screen.gui.GuiIngame
import screen.mouse.Mouse

object Interactor : Tool(Control.Group.INTERACTION.controls) {
    init {
        activationPredicate = {
            LevelManager.levelObjectUnderMouse?.isInteractable == true && !Selector.dragging && !BlockPlacer.hasPlacedThisInteraction
        }
    }

    override fun onUse(event: ControlEvent, mouseLevelX: Int, mouseLevelY: Int): Boolean {
        // todo don't let the player interact with stuff too far away or too fast
        if (event.control in Control.Group.SCROLL) {
            LevelManager.levelObjectUnderMouse!!.onScroll(if (event.control == Control.SCROLL_DOWN) -1 else 1)
        } else {
            LevelManager.levelObjectUnderMouse!!.onInteractOn(
                event,
                mouseLevelX,
                mouseLevelY,
                Mouse.button,
                InputManager.state.isDown(Modifier.SHIFT),
                InputManager.state.isDown(Modifier.CTRL),
                InputManager.state.isDown(Modifier.ALT)
            )
        }
        return true
    }

    override fun renderBelow(level: Level) {
        if (level == LevelManager.levelUnderMouse) {
            val selected = LevelManager.levelObjectUnderMouse
            if (selected is Block) {
                Renderer.renderEmptyRectangle(
                    selected.x - 1,
                    selected.y - 1,
                    (selected.type.widthTiles shl 4) + 2,
                    (selected.type.heightTiles shl 4) + 2,
                    params = TextureRenderParams(color = Color(0x1A6AF472))
                )
            } else if (selected is MovingObject) {
                Renderer.renderEmptyRectangle(
                    selected.x + selected.hitbox.xStart - 1,
                    selected.y + selected.hitbox.yStart - 1,
                    selected.hitbox.width + 2,
                    selected.hitbox.height + 2,
                    params = TextureRenderParams(color = Color(0x1A6AF472))
                )
            }
        }
    }
}