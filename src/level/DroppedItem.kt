package level

import graphics.Renderer
import inv.Item
import inv.ItemType
import io.*
import level.moving.MovingObject
import main.Game
import screen.Mouse

class DroppedItem(xPixel: Int, yPixel: Int, val type: ItemType, quantity: Int = 1) :
        MovingObject(xPixel, yPixel, Hitbox.DROPPED_ITEM), ControlPressHandler {

    var quantity = quantity
        set(value) {
            if (quantity < 1) {
                Game.currentLevel.remove(this)
            } else {
                field = value
            }
        }

    override fun onAddToLevel() {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL, Control.INTERACT)
    }

    override fun onRemoveFromLevel() {
        InputManager.removeControlPressHandler(this)
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.INTERACT && p.pressType == PressType.PRESSED) {
            Game.currentLevel.remove(this)
            if (Mouse.heldItem == null)
                Mouse.setHeldItem(Item(type, quantity))
            else {
                Game.currentLevel.add(DroppedItem(xPixel, yPixel, Mouse.heldItem!!.type, Mouse.heldItem!!.quantity))
                Mouse.setHeldItem(Item(type, quantity))
            }
        }
    }

    override fun render() {
        Renderer.renderTextureKeepAspect(type.texture, xPixel, yPixel, Hitbox.DROPPED_ITEM.width, Hitbox.DROPPED_ITEM.height)
        Renderer.renderText(quantity, xPixel, yPixel)
        super.render()
    }

    companion object {
        init {
            Mouse.addLevelTooltipTemplate({
                if (it is DroppedItem)
                    return@addLevelTooltipTemplate it.type.name
                return@addLevelTooltipTemplate null
            })
        }
    }
}