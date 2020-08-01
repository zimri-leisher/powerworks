package level

import graphics.Renderer
import io.PressType
import item.ItemType
import level.moving.MovingObject
import level.moving.MovingObjectType
import main.DebugCode
import main.Game
import network.DroppedItemReference
import network.LevelObjectReference
import player.PickUpDroppedItemAction
import player.PlayerManager
import screen.mouse.Tooltips
import serialization.Id

class DroppedItem(xPixel: Int, yPixel: Int,
                  @Id(-10)
                  val itemType: ItemType,
                  quantity: Int = 1) :
        LevelObject(LevelObjectType.DROPPED_ITEM, xPixel, yPixel, 0) {

    @Id(-20)
    var quantity = quantity
        set(value) {
            if (quantity < 1) {
                level.remove(this)
            } else {
                field = value
            }
        }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.RELEASED) {
            PlayerManager.takeAction(PickUpDroppedItemAction(PlayerManager.localPlayer, listOf(DroppedItemReference(this))))
        }
    }

    override fun render() {
        itemType.icon.render(xPixel, yPixel, Hitbox.DROPPED_ITEM.width, Hitbox.DROPPED_ITEM.height, true)
        Renderer.renderText(quantity, xPixel, yPixel)
        if (Game.currentDebugCode == DebugCode.RENDER_HITBOXES) {
            renderHitbox()
        }
    }

    override fun toString() = "Dropped item at $xPixel, $yPixel, type: $type, quantity: $quantity"

    override fun toReference() = DroppedItemReference(this)

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