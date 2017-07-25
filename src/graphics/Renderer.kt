package graphics

import level.CameraObject
import java.awt.Graphics2D

object Renderer {
    /* Default camera position */
    var camera: CameraObject = object : CameraObject {
        override var xPixel = 0;
        override var yPixel = 0
    }

    var zoom: Double = 1.0

    lateinit var g2d: Graphics2D

    fun feed(graphics2D: Graphics2D) {
        g2d = graphics2D
    }

    fun renderTexture(t: Texture) {
        Images.ERROR
    }
}