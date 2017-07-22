package graphics

import data.LevelLocation
import level.CameraObject

object Renderer {
    /* Default camera position */
    var camera: CameraObject = object: CameraObject { override var loc = LevelLocation(0, 0) }

    var zoom: Double = 1.0
}