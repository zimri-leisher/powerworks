package data

import graphics.Renderer
import level.CameraObject

class ScreenLocation(var xPixel: Int, var yPixel: Int) {
    fun toLevel(): LevelLocation {
        val c: CameraObject = Renderer.camera
        val zoom = Renderer.zoom
        return LevelLocation((xPixel / zoom).toInt() + c.loc.xPixel, (yPixel / zoom).toInt() + c.loc.yPixel)
    }
}