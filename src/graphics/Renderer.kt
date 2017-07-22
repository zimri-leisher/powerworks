package graphics

import data.LevelLocation
import level.CameraObject
import java.awt.Graphics2D

object Renderer {
    /* Default camera position */
    var camera: CameraObject = object: CameraObject { override var loc = LevelLocation(0, 0) }

    var zoom: Double = 1.0

    lateinit var g2d: Graphics2D


}