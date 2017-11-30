package level

import graphics.Renderer
import inv.ItemType
import level.moving.MovingObject

class DroppedItem(xPixel: Int, yPixel: Int, val type: ItemType) :
        MovingObject(xPixel, yPixel, Hitbox.DROPPED_ITEM) {

    override fun render() {
        val widthPixels = Hitbox.DROPPED_ITEM.width
        val heightPixels = Hitbox.DROPPED_ITEM.height
        var w = widthPixels
        var h = heightPixels
        val t = type.texture
        if (t.widthPixels > t.heightPixels) {
            if (t.widthPixels > widthPixels) {
                w = widthPixels
                val ratio = widthPixels.toDouble() / t.widthPixels
                h = (t.heightPixels * ratio).toInt()
            }
        }
        if (t.heightPixels > t.widthPixels) {
            if (t.heightPixels > heightPixels) {
                h = heightPixels
                val ratio = heightPixels.toDouble() / t.heightPixels
                w = (t.widthPixels * ratio).toInt()
            }
        }
        Renderer.renderTexture(type.texture, xPixel + (widthPixels - w) / 2, yPixel + (heightPixels - h) / 2, w, h)
        super.render()
    }
}