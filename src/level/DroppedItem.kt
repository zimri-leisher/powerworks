package level

import graphics.Renderer
import inv.ItemType
import level.moving.MovingObject

class DroppedItem(xPixel: Int, yPixel: Int, val type: ItemType) :
        MovingObject(xPixel, yPixel, Hitbox.DROPPED_ITEM) {

    override fun render() {
        Renderer.renderTextureKeepAspect(type.texture, xPixel, yPixel, Hitbox.DROPPED_ITEM.width, Hitbox.DROPPED_ITEM.height)
        super.render()
    }
}