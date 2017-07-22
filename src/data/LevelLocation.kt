package data

import graphics.Renderer
import level.CameraObject

class LevelLocation(var xPixel: Int, var yPixel: Int) {
    fun toScreen(): ScreenLocation {
        val c: CameraObject = Renderer.camera
        val zoom = Renderer.zoom
        return ScreenLocation(((xPixel - c.loc.xPixel) * zoom).toInt(), ((yPixel - c.loc.yPixel) * zoom).toInt())
    }
}