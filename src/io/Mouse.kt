package io

import graphics.Renderer
import inv.Item
import screen.GUIItemSlot

object Mouse {
    var button = 0
    var xPixel = 0
    var yPixel = 0
    var heldItem: Item? = null

    fun render() {
        if (heldItem != null) {
            val i = heldItem!!
            var w = GUIItemSlot.WIDTH
            var h = GUIItemSlot.HEIGHT
            val t = i.type.texture
            if (t.widthPixels > t.heightPixels) {
                if (t.widthPixels > GUIItemSlot.WIDTH) {
                    w = GUIItemSlot.WIDTH
                    val ratio = GUIItemSlot.WIDTH.toDouble() / t.widthPixels
                    h = (t.heightPixels * ratio).toInt()
                }
            }
            if (t.heightPixels > t.widthPixels) {
                if (t.heightPixels > GUIItemSlot.HEIGHT) {
                    h = GUIItemSlot.HEIGHT
                    val ratio = GUIItemSlot.HEIGHT.toDouble() / t.heightPixels
                    w = (t.widthPixels * ratio).toInt()
                }
            }
            Renderer.renderTexture(t, xPixel + (GUIItemSlot.WIDTH - w) / 2, yPixel + (GUIItemSlot.HEIGHT - h) / 2, w, h)
            Renderer.renderText(i.quantity.toString(), xPixel, yPixel + 4)
        }
    }
}