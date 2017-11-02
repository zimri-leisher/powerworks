package io

import graphics.Renderer
import inv.Item

object Mouse {
    var button = 0
    var xPixel = 0
    var yPixel = 0
    var heldItem: Item? = null

    fun render() {
        if(heldItem != null) {
            Renderer.renderTexture(heldItem!!.type.texture, xPixel, yPixel)
        }
    }
}