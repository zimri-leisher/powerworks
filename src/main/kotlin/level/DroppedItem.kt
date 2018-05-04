package level

import graphics.Renderer
import io.*
import item.Item
import item.ItemType
import level.moving.MovingObject
import main.Game
import screen.HUD
import screen.Mouse

class DroppedItem(xPixel: Int, yPixel: Int, val itemType: ItemType, quantity: Int = 1) :
        MovingObject(LevelObjectType.DROPPED_ITEM, xPixel, yPixel, 0, Hitbox.DROPPED_ITEM), ControlPressHandler {

    var quantity = quantity
        set(value) {
            if (quantity < 1) {
                Level.remove(this)
            } else {
                field = value
            }
        }

    override fun onAddToLevel() {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_THIS, Control.INTERACT)
    }

    override fun onRemoveFromLevel() {
        InputManager.removeControlPressHandler(this)
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.INTERACT && p.pressType == PressType.RELEASED) {
            Level.remove(this)
            Game.mainInv.add(Item(itemType, quantity))
            Mouse.heldItemType = itemType
            HUD.Hotbar.items.add(itemType)
        }
    }

    override fun render() {
        Renderer.renderTextureKeepAspect(itemType.texture, xPixel, yPixel, Hitbox.DROPPED_ITEM.width, Hitbox.DROPPED_ITEM.height)
        Renderer.renderText(quantity, xPixel, yPixel)
        super.render()
    }

    companion object {
        init {
            Mouse.addLevelTooltipTemplate({
                if (it is DroppedItem)
                    return@addLevelTooltipTemplate "${it.itemType.name} * ${it.quantity}"
                return@addLevelTooltipTemplate null
            })
        }
    }
}