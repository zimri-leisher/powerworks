package level

import graphics.Renderer
import io.*
import item.Item
import item.ItemType
import level.moving.MovingObject
import main.Game
import screen.HUD
import screen.mouse.Mouse
import screen.mouse.Tooltips

class DroppedItem(xPixel: Int, yPixel: Int, val itemType: ItemType, quantity: Int = 1) :
        MovingObject(LevelObjectType.DROPPED_ITEM, xPixel, yPixel, 0, Hitbox.DROPPED_ITEM) {

    var quantity = quantity
        set(value) {
            if (quantity < 1) {
                Level.remove(this)
            } else {
                field = value
            }
        }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.RELEASED) {
            Level.remove(this)
            Game.mainInv.add(Item(itemType, quantity))
            Mouse.heldItemType = itemType
            HUD.Hotbar.items.add(itemType)
        }
    }

    override fun render() {
        Renderer.renderTextureKeepAspect(itemType.icon, xPixel, yPixel, Hitbox.DROPPED_ITEM.width, Hitbox.DROPPED_ITEM.height)
        Renderer.renderText(quantity, xPixel, yPixel)
        super.render()
    }

    override fun toString() = "Dropped item at $xPixel, $yPixel, type: $type, quantity: $quantity"

    companion object {
        init {
            Tooltips.addLevelTooltipTemplate({
                if (it is DroppedItem)
                    return@addLevelTooltipTemplate "${it.itemType.name} * ${it.quantity}"
                return@addLevelTooltipTemplate null
            })
        }
    }
}